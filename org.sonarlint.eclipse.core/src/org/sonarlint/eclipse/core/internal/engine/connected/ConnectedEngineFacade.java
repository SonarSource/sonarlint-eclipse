/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.engine.connected;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.StoragePathManager;
import org.sonarlint.eclipse.core.internal.backend.PluginPathHelper;
import org.sonarlint.eclipse.core.internal.engine.AnalysisRequirementNotifications;
import org.sonarlint.eclipse.core.internal.engine.SkippedPluginsNotifier;
import org.sonarlint.eclipse.core.internal.http.PreemptiveAuthenticatorInterceptor;
import org.sonarlint.eclipse.core.internal.http.SonarLintHttpClientOkHttpImpl;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintAnalyzerLogOutput;
import org.sonarlint.eclipse.core.internal.jobs.WrappedProgressMonitor;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.internal.vcs.VcsService;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedRuleDetails;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectionValidator;
import org.sonarsource.sonarlint.core.client.api.connected.ProjectBranches;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;
import org.sonarsource.sonarlint.core.commons.Language;
import org.sonarsource.sonarlint.core.commons.http.HttpClient;
import org.sonarsource.sonarlint.core.commons.progress.ProgressMonitor;
import org.sonarsource.sonarlint.core.serverapi.EndpointParams;
import org.sonarsource.sonarlint.core.serverapi.ServerApi;
import org.sonarsource.sonarlint.core.serverapi.ServerApiHelper;
import org.sonarsource.sonarlint.core.serverapi.component.ServerProject;
import org.sonarsource.sonarlint.core.serverapi.hotspot.GetSecurityHotspotRequestParams;
import org.sonarsource.sonarlint.core.serverapi.hotspot.ServerHotspotDetails;
import org.sonarsource.sonarlint.core.serverapi.organization.ServerOrganization;
import org.sonarsource.sonarlint.core.serverconnection.ProjectBinding;
import org.sonarsource.sonarlint.core.serverconnection.issues.ServerIssue;
import org.sonarsource.sonarlint.core.serverconnection.issues.ServerTaintIssue;
import org.sonarsource.sonarlint.core.serverconnection.smartnotifications.ServerNotificationsRegistry;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class ConnectedEngineFacade implements IConnectedEngineFacade {

  public static final String OLD_SONARCLOUD_URL = "https://sonarqube.com";

  private final String id;
  private String host;
  @Nullable
  private String organization;
  private boolean hasAuth;
  @Nullable
  private ConnectedSonarLintEngine wrappedEngine;
  private final List<IConnectedEngineFacadeListener> facadeListeners = new ArrayList<>();
  private boolean notificationsDisabled;
  // Cache the project list to avoid dead lock
  private final Map<String, ServerProject> allProjectsByKey = new ConcurrentHashMap<>();

  public static String getSonarCloudUrl() {
    // For testing we need to allow changing default URL
    return System.getProperty("sonarlint.internal.sonarcloud.url", "https://sonarcloud.io");
  }

  ConnectedEngineFacade(String id) {
    this.id = id;
  }

  @Nullable
  private synchronized ConnectedSonarLintEngine getOrCreateEngine() {
    if (wrappedEngine == null) {
      SonarLintLogger.get().info("Starting SonarLint engine for connection '" + id + "'...");
      var nodeJsManager = SonarLintCorePlugin.getNodeJsManager();
      var builder = isSonarCloud() ? ConnectedGlobalConfiguration.sonarCloudBuilder() : ConnectedGlobalConfiguration.sonarQubeBuilder();
      builder
        .setConnectionId(getId())
        .setWorkDir(StoragePathManager.getConnectionSpecificWorkDir(getId()))
        .setStorageRoot(StoragePathManager.getStorageDir())
        .setLogOutput(new SonarLintAnalyzerLogOutput())
        .addEnabledLanguages(SonarLintUtils.getEnabledLanguages().toArray(new Language[0]))
        .setNodeJs(nodeJsManager.getNodeJsPath(), nodeJsManager.getNodeJsVersion())
        .setClientPid(SonarLintUtils.getPlatformPid());

      builder.useEmbeddedPlugin(Language.JS.getPluginKey(), PluginPathHelper.findEmbeddedJsPlugin());
      builder.useEmbeddedPlugin(Language.HTML.getPluginKey(), PluginPathHelper.findEmbeddedHtmlPlugin());
      builder.useEmbeddedPlugin(Language.XML.getPluginKey(), PluginPathHelper.findEmbeddedXmlPlugin());
      builder.useEmbeddedPlugin(Language.SECRETS.getPluginKey(), PluginPathHelper.findEmbeddedSecretsPlugin());

      var globalConfig = builder.build();
      try {
        this.wrappedEngine = new ConnectedSonarLintEngineImpl(globalConfig);
        SkippedPluginsNotifier.notifyForSkippedPlugins(wrappedEngine.getPluginDetails(), id);
        subscribeForEventsForBoundProjects();
      } catch (Throwable e) {
        SonarLintLogger.get().error("Unable to start connected SonarLint engine", e);
        wrappedEngine = null;
      }
    }
    return wrappedEngine;
  }

  private <G> Optional<G> withEngine(Function<ConnectedSonarLintEngine, G> function) {
    getOrCreateEngine();
    if (wrappedEngine != null) {
      return Optional.ofNullable(function.apply(wrappedEngine));
    }
    return Optional.empty();
  }

  private void doWithEngine(Consumer<ConnectedSonarLintEngine> consumer) {
    getOrCreateEngine();
    if (wrappedEngine != null) {
      consumer.accept(wrappedEngine);
    }
  }

  private void reloadProjects(ConnectedSonarLintEngine engine, IProgressMonitor monitor) {
    this.allProjectsByKey.clear();
    this.allProjectsByKey.putAll(engine.downloadAllProjects(createEndpointParams(), buildClientWithProxyAndCredentials(),
      new WrappedProgressMonitor(monitor, "Download project list from server '" + getId() + "'")));
    // Some project names might have been changed
    notifyAllListenersStateChanged();
  }

  @Override
  public void notifyAllListenersStateChanged() {
    for (var listener : facadeListeners) {
      listener.stateChanged(this);
    }
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getHost() {
    return host;
  }

  public ConnectedEngineFacade setHost(String host) {
    this.host = host;
    return this;
  }

  @Nullable
  @Override
  public String getOrganization() {
    return organization;
  }

  public ConnectedEngineFacade setOrganization(@Nullable String organization) {
    this.organization = organization;
    return this;
  }

  @Override
  public boolean hasAuth() {
    return hasAuth;
  }

  public ConnectedEngineFacade setHasAuth(boolean hasAuth) {
    this.hasAuth = hasAuth;
    return this;
  }

  @Override
  public synchronized void delete() {
    doStop();
    for (var sonarLintProject : getBoundProjects()) {
      unbind(sonarLintProject);
    }
    SonarLintCorePlugin.getServersManager().removeServer(this);
  }

  public static void unbind(ISonarLintProject project) {
    var config = SonarLintCorePlugin.loadConfig(project);
    config.setProjectBinding(null);
    SonarLintCorePlugin.saveConfig(project, config);
    project.deleteAllMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID);
    project.deleteAllMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_FLOW_ID);
    project.deleteAllMarkers(SonarLintCorePlugin.MARKER_REPORT_ID);
    project.deleteAllMarkers(SonarLintCorePlugin.MARKER_REPORT_FLOW_ID);
    SonarLintCorePlugin.clearIssueTracker(project);
  }

  @Override
  public void updateConfig(String url, @Nullable String organization, String username, String password, boolean notificationsDisabled) {
    this.host = url;
    this.organization = organization;
    this.hasAuth = StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password);
    this.notificationsDisabled = notificationsDisabled;
    SonarLintCorePlugin.getServersManager().updateConnection(this, username, password);
  }

  @Nullable
  @Override
  public AnalysisResults runAnalysis(ConnectedAnalysisConfiguration config, IssueListener issueListener, IProgressMonitor monitor) {
    return withEngine(engine -> {
      var analysisResults = engine.analyze(config, issueListener, null, new WrappedProgressMonitor(monitor, "Analysis"));
      AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(analysisResults, engine.getPluginDetails());
      return analysisResults;
    }).orElse(null);
  }

  @Override
  public CompletableFuture<ConnectedRuleDetails> getRuleDescription(String ruleKey, @Nullable String projectKey) {
    return withEngine(engine -> engine.getActiveRuleDetails(createEndpointParams(), buildClientWithProxyAndCredentials(), ruleKey, projectKey))
      .orElse(CompletableFuture.completedFuture(null));
  }

  public synchronized void stop() {
    doStop();
  }

  private void doStop() {
    if (wrappedEngine != null) {
      wrappedEngine.stop(false);
      wrappedEngine = null;
    }
  }

  @Override
  public void updateProjectList(IProgressMonitor monitor) {
    doWithEngine(engine -> reloadProjects(engine, monitor));
  }

  private static Stream<ISonarLintProject> getOpenedProjects() {
    return ProjectsProviderUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen);
  }

  @Override
  public Set<String> getBoundProjectKeys() {
    return getOpenedProjects()
      .map(project -> SonarLintCorePlugin.loadConfig(project).getProjectBinding())
      .filter(binding -> binding.filter(b -> id.equals(b.connectionId())).isPresent())
      .map(Optional::get)
      .map(ProjectBinding::projectKey)
      .collect(Collectors.toSet());
  }

  @Override
  public List<ISonarLintProject> getBoundProjects() {
    return getOpenedProjects()
      .filter(p -> {
        var config = SonarLintCorePlugin.loadConfig(p);
        return config.getProjectBinding().filter(b -> id.equals(b.connectionId())).isPresent();
      }).collect(toList());
  }

  public List<RemoteSonarProject> getBoundRemoteProjects() {
    return ProjectsProviderUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen)
      .map(SonarLintCorePlugin::loadConfig)
      .map(SonarLintProjectConfiguration::getProjectBinding)
      .flatMap(Optional::stream)
      .filter(c -> c.connectionId().equals(id))
      .map(ProjectBinding::projectKey)
      .distinct()
      .sorted()
      .map(projectKey -> {
        var remoteProject = getCachedRemoteProject(projectKey);
        if (remoteProject.isPresent()) {
          return new RemoteSonarProject(id, remoteProject.get().getKey(), remoteProject.get().getName());
        } else {
          return new RemoteSonarProject(id, projectKey, "<unknown>");
        }
      })
      .collect(toList());
  }

  @Override
  public List<ISonarLintProject> getBoundProjects(String projectKey) {
    return ProjectsProviderUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen)
      .filter(p -> {
        var config = SonarLintCorePlugin.loadConfig(p);
        return config.getProjectBinding().filter(b -> id.equals(b.connectionId()) && projectKey.equals(b.projectKey())).isPresent();
      }).collect(toList());
  }

  @Override
  public void updateProjectStorage(String projectKey, IProgressMonitor monitor) {
    doWithEngine(engine -> {
      engine.updateProject(createEndpointParams(), buildClientWithProxyAndCredentials(), projectKey,
        new WrappedProgressMonitor(monitor, "Update configuration from server '" + getId() + "' for project '" + projectKey + "'"));
      getBoundProjects(projectKey).forEach(p -> {
        var projectBinding = engine.calculatePathPrefixes(projectKey, p.files().stream().map(ISonarLintFile::getProjectRelativePath).collect(toList()));
        var idePathPrefix = projectBinding.idePathPrefix();
        var sqPathPrefix = projectBinding.serverPathPrefix();
        SonarLintLogger.get().debug("Detected prefixes for " + p.getName() + ":\n  IDE prefix: " + idePathPrefix + "\n  Server side prefix: " + sqPathPrefix);
        var config = SonarLintCorePlugin.loadConfig(p);
        config.setProjectBinding(new EclipseProjectBinding(getId(), projectKey, sqPathPrefix, idePathPrefix));
        SonarLintCorePlugin.saveConfig(p, config);
      });
      SkippedPluginsNotifier.notifyForSkippedPlugins(engine.getPluginDetails(), id);
    });
    // Some prefix/suffix might have been changed
    notifyAllListenersStateChanged();

  }

  public static CompletableFuture<Status> testConnection(String url, @Nullable String organization, @Nullable String username, @Nullable String password) {
    var withProxy = buildOkHttpClientWithProxyAndCredentials(url, username, password);
    var helper = new ServerApiHelper(createEndpointParams(url, organization), new SonarLintHttpClientOkHttpImpl(withProxy.build()));
    return new ConnectionValidator(helper).validateConnection()
      .thenApply(testConnection -> {
        if (testConnection.success()) {
          return new Status(IStatus.OK, SonarLintCorePlugin.PLUGIN_ID, "Successfully connected!");
        } else {
          return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, testConnection.message());
        }
      })
      .exceptionally(e -> {
        if (e.getCause() instanceof UnknownHostException) {
          return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, "Unknown host: " + url);
        }
        SonarLintLogger.get().debug(e.getMessage(), e);
        return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, e.getMessage(), e);
      });
  }

  public static List<ServerOrganization> listUserOrganizations(String url, String username, String password, IProgressMonitor monitor) {
    var withProxy = buildOkHttpClientWithProxyAndCredentials(url, username, password);
    var endpointPAramsWithoutOrg = createEndpointParams(url, null);
    var serverApi = new ServerApi(endpointPAramsWithoutOrg, new SonarLintHttpClientOkHttpImpl(withProxy.build()));
    return serverApi.organization().listUserOrganizations(new ProgressMonitor(new WrappedProgressMonitor(monitor, "Fetch organizations")));
  }

  public HttpClient buildClientWithProxyAndCredentials() {
    var withProxy = SonarLintUtils.withProxy(getHost(), SonarLintCorePlugin.getOkHttpClient());
    if (hasAuth()) {
      @Nullable
      String username;
      @Nullable
      String password;
      try {
        username = ConnectedEngineFacadeManager.getUsername(this);
        password = ConnectedEngineFacadeManager.getPassword(this);
      } catch (StorageException e) {
        throw new IllegalStateException("Unable to read server credentials from storage: " + e.getMessage(), e);
      }
      withProxy.addNetworkInterceptor(new PreemptiveAuthenticatorInterceptor(credentials(username, password)));
    }
    return new SonarLintHttpClientOkHttpImpl(withProxy.build());
  }

  public static boolean checkNotificationsSupported(String url, @Nullable String organization, String username, String password) {
    var withProxy = buildOkHttpClientWithProxyAndCredentials(url, username, password);

    return ServerNotificationsRegistry.isSupported(createEndpointParams(url, organization), new SonarLintHttpClientOkHttpImpl(withProxy.build()));
  }

  private static EndpointParams createEndpointParams(String url, @Nullable String organization) {
    return new EndpointParams(url, getSonarCloudUrl().equals(url), organization);
  }

  private static OkHttpClient.Builder buildOkHttpClientWithProxyAndCredentials(String url, @Nullable String username, @Nullable String password) {
    var withProxy = SonarLintUtils.withProxy(url, SonarLintCorePlugin.getOkHttpClient());
    if (StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password)) {
      withProxy.addNetworkInterceptor(new PreemptiveAuthenticatorInterceptor(credentials(username, password)));
    }
    return withProxy;
  }

  private static String credentials(@Nullable String username, @Nullable String password) {
    return Credentials.basic(StringUtils.defaultString(username, ""), StringUtils.defaultString(password, ""));
  }

  public boolean checkNotificationsSupported() {
    if (isSonarCloud()) {
      return true;
    }
    try {
      return ServerNotificationsRegistry.isSupported(createEndpointParams(), buildClientWithProxyAndCredentials());
    } catch (Exception e) {
      // Maybe the server is temporarily unavailable
      SonarLintLogger.get().debug("Unable to check for if notifications are supported for server '" + getHost() + "'", e);
      return false;
    }
  }

  public EndpointParams createEndpointParams() {
    return createEndpointParams(getHost(), getOrganization());
  }

  @Override
  public TextSearchIndex<ServerProject> computeProjectIndex() {
    var index = new TextSearchIndex<ServerProject>();
    for (ServerProject project : allProjectsByKey.values()) {
      index.index(project, project.getKey() + " " + project.getName());
    }
    return index;
  }

  @Override
  public Optional<ServerProject> getCachedRemoteProject(String projectKey) {
    return Optional.ofNullable(allProjectsByKey.get(projectKey));
  }

  @Override
  public List<ISonarLintFile> getServerFileExclusions(ProjectBinding binding, Collection<ISonarLintFile> files, Predicate<ISonarLintFile> testFilePredicate) {
    return withEngine(engine -> engine.getExcludedFiles(binding, files, ISonarLintFile::getProjectRelativePath, testFilePredicate)).orElse(emptyList());
  }

  @Override
  public void addConnectedEngineListener(IConnectedEngineFacadeListener listener) {
    facadeListeners.add(listener);
  }

  @Override
  public void removeConnectedEngineListener(IConnectedEngineFacadeListener listener) {
    facadeListeners.remove(listener);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ConnectedEngineFacade)) {
      return false;
    }
    return ((ConnectedEngineFacade) obj).getId().equals(this.getId());
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  @Override
  public boolean isSonarCloud() {
    return getSonarCloudUrl().equals(this.host);
  }

  @Override
  public boolean areNotificationsDisabled() {
    return notificationsDisabled;
  }

  public ConnectedEngineFacade setNotificationsDisabled(boolean value) {
    this.notificationsDisabled = value;
    return this;
  }

  public void downloadServerIssues(String projectKey, @Nullable String branchName, IProgressMonitor monitor) {
    doWithEngine(
      engine -> engine.downloadAllServerIssues(createEndpointParams(), buildClientWithProxyAndCredentials(), projectKey, branchName,
        new WrappedProgressMonitor(monitor, "Fetch issues")));
  }

  public List<ServerIssue> downloadAllServerIssuesForFile(ProjectBinding projectBinding, String branchName, String filePath, IProgressMonitor monitor) {
    return withEngine(
      engine -> {
        engine.downloadAllServerIssuesForFile(createEndpointParams(), buildClientWithProxyAndCredentials(), projectBinding, filePath,
          branchName, new WrappedProgressMonitor(monitor, "Fetch issues"));
        return engine.getServerIssues(projectBinding, branchName, filePath);
      })
        .orElse(emptyList());
  }

  public void downloadAllServerTaintIssuesForFile(ProjectBinding projectBinding, String branchName, String filePath, IProgressMonitor monitor) {
    doWithEngine(
      engine -> engine.downloadAllServerTaintIssuesForFile(createEndpointParams(), buildClientWithProxyAndCredentials(), projectBinding, filePath,
        branchName, new WrappedProgressMonitor(monitor, "Fetch taint issues")));
  }

  public List<ServerIssue> getServerIssues(ProjectBinding projectBinding, String branchName, String filePath) {
    return withEngine(engine -> engine.getServerIssues(projectBinding, branchName, filePath)).orElse(emptyList());
  }

  public List<ServerTaintIssue> getServerTaintIssues(EclipseProjectBinding projectBinding, String branchName, String filePath) {
    return withEngine(engine -> engine.getServerTaintIssues(projectBinding, branchName, filePath)).orElse(emptyList());
  }

  public ProjectBinding calculatePathPrefixes(String projectKey, List<String> ideFilePaths) {
    return withEngine(engine -> engine.calculatePathPrefixes(projectKey, ideFilePaths)).orElse(new ProjectBinding(projectKey, "", ""));
  }

  @Override
  public Optional<ServerHotspotDetails> getServerHotspot(String hotspotKey, String projectKey) {
    var serverApi = new ServerApi(createEndpointParams(), buildClientWithProxyAndCredentials());
    return serverApi.hotspot().fetch(new GetSecurityHotspotRequestParams(hotspotKey, projectKey));
  }

  @Override
  public ProjectBranches getServerBranches(String projectKey) {
    return withEngine(engine -> engine.getServerBranches(projectKey))
      .orElseThrow(() -> new IllegalStateException("The connected engine could not be started"));
  }

  @Override
  public void manualSync(Set<String> projectKeysToUpdate, IProgressMonitor monitor) {
    doWithEngine(engine -> {
      sync(projectKeysToUpdate, monitor, engine);
      updateAllProjectIssuesForCurrentBranch(projectKeysToUpdate, monitor, engine);
    });
  }

  private void sync(Set<String> projectKeysToUpdate, IProgressMonitor monitor, ConnectedSonarLintEngine engine) {
    engine.sync(createEndpointParams(), buildClientWithProxyAndCredentials(), projectKeysToUpdate,
      new WrappedProgressMonitor(monitor, "Synchronize projects storage for connection '" + getId() + "'"));
    // Force recompute of best server branch
    VcsService.clearVcsCache();

    updateProjectList(monitor);
  }

  private void updateAllProjectIssuesForCurrentBranch(Set<String> projectKeys, IProgressMonitor monitor, ConnectedSonarLintEngine engine) {
    for (var projectKey : projectKeys) {
      var boundProjects = getBoundProjects(projectKey);

      Set<String> activeBranches = new HashSet<>();
      for (var project : boundProjects) {
        activeBranches.add(VcsService.getServerBranch(project));
      }

      activeBranches.forEach(b -> engine.downloadAllServerIssues(createEndpointParams(),
        buildClientWithProxyAndCredentials(), projectKey, b,
        new WrappedProgressMonitor(monitor, "Synchronize issues for connection '" + getId() + "'")));

    }
  }

  @Override
  public void autoSync(Set<String> projectKeys, IProgressMonitor monitor) {
    doWithEngine(engine -> {
      sync(projectKeys, monitor, engine);
      syncProjectIssuesForCurrentBranch(projectKeys, monitor, engine);
    });
  }

  private void syncProjectIssuesForCurrentBranch(Set<String> projectKeys, IProgressMonitor monitor, ConnectedSonarLintEngine engine) {
    for (var projectKey : projectKeys) {
      var boundProjects = getBoundProjects(projectKey);

      Set<String> activeBranches = new HashSet<>();
      for (var project : boundProjects) {
        activeBranches.add(VcsService.getServerBranch(project));
      }

      activeBranches.forEach(b -> {
        engine.syncServerIssues(createEndpointParams(), buildClientWithProxyAndCredentials(), projectKey, b,
          new WrappedProgressMonitor(monitor, "Synchronize issues for connection '" + getId() + "'"));
        engine.syncServerTaintIssues(createEndpointParams(), buildClientWithProxyAndCredentials(), projectKey, b,
          new WrappedProgressMonitor(monitor, "Synchronize taint issues for connection '" + getId() + "'"));
      });

    }
  }

  @Override
  public void subscribeForEventsForBoundProjects() {
    if (wrappedEngine != null) {
      wrappedEngine.subscribeForEvents(createEndpointParams(), buildClientWithProxyAndCredentials(), getBoundProjectKeys(), e -> {
      }, null);
    }
  }
}
