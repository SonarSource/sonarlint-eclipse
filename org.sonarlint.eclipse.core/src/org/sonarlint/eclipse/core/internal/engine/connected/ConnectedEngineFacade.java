/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.Version;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.StoragePathManager;
import org.sonarlint.eclipse.core.internal.engine.AnalysisRequirementNotifications;
import org.sonarlint.eclipse.core.internal.engine.SkippedPluginsNotifier;
import org.sonarlint.eclipse.core.internal.http.PreemptiveAuthenticatorInterceptor;
import org.sonarlint.eclipse.core.internal.http.SonarLintHttpClientOkHttpImpl;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintAnalyzerLogOutput;
import org.sonarlint.eclipse.core.internal.jobs.WrappedProgressMonitor;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.utils.NodeJsManager;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.common.Language;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine.State;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectionValidator;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalStorageStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ProjectBinding;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;
import org.sonarsource.sonarlint.core.client.api.connected.SonarAnalyzer;
import org.sonarsource.sonarlint.core.client.api.connected.StateListener;
import org.sonarsource.sonarlint.core.client.api.connected.UpdateResult;
import org.sonarsource.sonarlint.core.client.api.connected.ValidationResult;
import org.sonarsource.sonarlint.core.client.api.exceptions.DownloadException;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;
import org.sonarsource.sonarlint.core.notifications.ServerNotificationsRegistry;
import org.sonarsource.sonarlint.core.serverapi.EndpointParams;
import org.sonarsource.sonarlint.core.serverapi.HttpClient;
import org.sonarsource.sonarlint.core.serverapi.ServerApi;
import org.sonarsource.sonarlint.core.serverapi.ServerApiHelper;
import org.sonarsource.sonarlint.core.serverapi.hotspot.GetSecurityHotspotRequestParams;
import org.sonarsource.sonarlint.core.serverapi.hotspot.ServerHotspot;
import org.sonarsource.sonarlint.core.serverapi.organization.ServerOrganization;
import org.sonarsource.sonarlint.core.serverapi.project.ServerProject;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;

public class ConnectedEngineFacade implements IConnectedEngineFacade, StateListener {

  public static final String OLD_SONARCLOUD_URL = "https://sonarqube.com";

  private static final String NEED_UPDATE = "Need data update";
  private final String id;
  private String host;
  private String organization;
  private boolean hasAuth;
  private ConnectedSonarLintEngine wrappedEngine;
  private final List<IConnectedEngineFacadeListener> facadeListeners = new ArrayList<>();
  private GlobalStorageStatus updateStatus;
  private boolean hasUpdates;
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
      NodeJsManager nodeJsManager = SonarLintCorePlugin.getNodeJsManager();
      ConnectedGlobalConfiguration globalConfig = ConnectedGlobalConfiguration.builder()
        .setServerId(getId())
        .setWorkDir(StoragePathManager.getServerWorkDir(getId()))
        .setStorageRoot(StoragePathManager.getServerStorageRoot())
        .setLogOutput(new SonarLintAnalyzerLogOutput())
        .addEnabledLanguages(SonarLintUtils.getEnabledLanguages().toArray(new Language[0]))
        .setNodeJs(nodeJsManager.getNodeJsPath(), nodeJsManager.getNodeJsVersion())
        .build();
      try {
        this.wrappedEngine = new ConnectedSonarLintEngineImpl(globalConfig);
        this.wrappedEngine.addStateListener(this);
        this.updateStatus = wrappedEngine.getGlobalStorageStatus();
        if (wrappedEngine.getState().equals(State.UPDATED)) {
          reloadProjects(wrappedEngine);
          SkippedPluginsNotifier.notifyForSkippedPlugins(wrappedEngine.getPluginDetails(), id);
        }
      } catch (Throwable e) {
        SonarLintLogger.get().error("Unable to start connected SonarLint engine", e);
        wrappedEngine = null;
      }
    }
    return wrappedEngine;
  }

  private <G> Optional<G> withEngine(Function<ConnectedSonarLintEngine, G> function) {
    getOrCreateEngine();
    synchronized (this) {
      if (wrappedEngine != null) {
        return Optional.ofNullable(function.apply(wrappedEngine));
      }
    }
    return Optional.empty();
  }

  private void doWithEngine(Consumer<ConnectedSonarLintEngine> consumer) {
    getOrCreateEngine();
    synchronized (this) {
      if (wrappedEngine != null) {
        consumer.accept(wrappedEngine);
      }
    }
  }

  private void reloadProjects(ConnectedSonarLintEngine engine) {
    this.allProjectsByKey.clear();
    this.allProjectsByKey.putAll(engine.allProjectsByKey());
  }

  @Override
  public void stateChanged(State state) {
    if (state.equals(State.UPDATED)) {
      reloadProjects(wrappedEngine);
    }
    notifyAllListenersStateChanged();
  }

  @Override
  public void notifyAllListenersStateChanged() {
    for (IConnectedEngineFacadeListener listener : facadeListeners) {
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
  public State getStorageState() {
    return withEngine(ConnectedSonarLintEngine::getState).orElse(State.UNKNOWN);
  }

  @Override
  public void checkForUpdates(IProgressMonitor progress) {
    this.hasUpdates = false;
    try {
      SubMonitor subMonitor = SubMonitor.convert(progress, getBoundProjects().size() + 1);
      SubMonitor globalMonitor = subMonitor.newChild(1);
      SonarLintLogger.get().info("Check for updates from server '" + getId() + "'");
      withEngine(engine -> engine.checkIfGlobalStorageNeedUpdate(createEndpointParams(), buildClientWithProxyAndCredentials(),
        new WrappedProgressMonitor(globalMonitor, "Check for configuration updates on server '" + getId() + "'"))).ifPresent(checkForUpdateResult -> {
          if (checkForUpdateResult.needUpdate()) {
            this.hasUpdates = true;
            checkForUpdateResult.changelog().forEach(line -> SonarLintLogger.get().info("  - " + line));
          }
        });

      Set<String> projectKeys = getBoundProjects()
        .stream()
        .map(SonarLintCorePlugin::loadConfig)
        .map(SonarLintProjectConfiguration::getProjectBinding)
        // Useless in practice because we only have bound projects
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(ProjectBinding::projectKey)
        .collect(Collectors.toSet());

      for (String projectKey : projectKeys) {
        SubMonitor projectMonitor = subMonitor.newChild(1);
        if (progress.isCanceled()) {
          return;
        }
        SonarLintLogger.get().info("Check for binding data updates on '" + getId() + "' for project '" + projectKey + "'");
        withEngine(engine -> engine.checkIfProjectStorageNeedUpdate(createEndpointParams(), buildClientWithProxyAndCredentials(), projectKey,
          new WrappedProgressMonitor(projectMonitor, "Checking for binding data update for project '" + projectKey + "'"))).ifPresent(projectUpdateCheckResult -> {
            if (projectUpdateCheckResult.needUpdate()) {
              this.hasUpdates = true;
              SonarLintLogger.get().info("For project '" + projectKey + "':");
              projectUpdateCheckResult.changelog().forEach(line -> SonarLintLogger.get().info("  - " + line));
            }
          });
      }
    } catch (DownloadException e) {
      // If server is not reachable, just ignore
      SonarLintLogger.get().debug("Unable to check for binding data updates on '" + getId() + "'", e);
    } finally {
      notifyAllListenersStateChanged();
    }
  }

  @Override
  public boolean hasUpdates() {
    return hasUpdates;
  }

  @Override
  public String getServerVersion() {
    if (getStorageState() != State.UPDATED) {
      return NEED_UPDATE;
    }
    return updateStatus.getServerVersion();
  }

  @Override
  public String getUpdateDate() {
    if (getStorageState() != State.UPDATED) {
      return NEED_UPDATE;
    }
    return new SimpleDateFormat().format(updateStatus.getLastUpdateDate());
  }

  @Override
  public String getSonarLintStorageStateLabel() {
    State state = withEngine(ConnectedSonarLintEngine::getState).orElse(State.UNKNOWN);
    switch (state) {
      case UNKNOWN:
        return "Unknown";
      case NEVER_UPDATED:
      case NEED_UPDATE:
        return NEED_UPDATE;
      case UPDATED:
        StringBuilder sb = new StringBuilder();
        if (!isSonarCloud()) {
          sb.append("Version: ");
          sb.append(getServerVersion());
          sb.append(", ");
        }
        sb.append("Last storage update: ");
        sb.append(getUpdateDate());
        if (hasUpdates) {
          sb.append(", New updates available");
        }
        return sb.toString();
      case UPDATING:
        return "Updating data...";
      default:
        throw new IllegalArgumentException(state.name());
    }
  }

  @Override
  public synchronized void delete() {
    doStop();
    for (ISonarLintProject sonarLintProject : getBoundProjects()) {
      unbind(sonarLintProject);
    }
    SonarLintCorePlugin.getServersManager().removeServer(this);
  }

  public static void unbind(ISonarLintProject project) {
    SonarLintCorePlugin.getInstance().notificationsManager().unsubscribe(project);
    SonarLintProjectConfiguration config = SonarLintCorePlugin.loadConfig(project);
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
      AnalysisResults analysisResults = engine.analyze(config, issueListener, null, new WrappedProgressMonitor(monitor, "Analysis"));
      AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(analysisResults, engine.getPluginDetails());
      return analysisResults;
    }).orElse(null);
  }

  @Nullable
  @Override
  public RuleDetails getRuleDescription(String ruleKey, @Nullable String projectKey) {
    return withEngine(engine -> engine.getActiveRuleDetails(ruleKey, projectKey)).orElse(null);
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
  public void updateStorage(IProgressMonitor monitor) {
    doWithEngine(engine -> {
      UpdateResult updateResult = engine.update(createEndpointParams(), buildClientWithProxyAndCredentials(),
        new WrappedProgressMonitor(monitor, "Update configuration from server '" + getId() + "'"));
      Collection<SonarAnalyzer> tooOld = updateResult.analyzers().stream()
        .filter(SonarAnalyzer::sonarlintCompatible)
        .filter(ConnectedEngineFacade::tooOld)
        .collect(Collectors.toList());
      if (!tooOld.isEmpty()) {
        SonarLintLogger.get().error(buildMinimumVersionFailMessage(tooOld));
      }
      updateStatus = updateResult.status();
      hasUpdates = false;
      SkippedPluginsNotifier.notifyForSkippedPlugins(engine.getPluginDetails(), id);
    });
  }

  private static boolean tooOld(SonarAnalyzer analyzer) {
    if (analyzer.minimumVersion() != null && analyzer.version() != null) {
      Version minimum = Version.parseVersion(analyzer.minimumVersion());
      Version version = Version.parseVersion(analyzer.version());
      return version.compareTo(minimum) < 0;
    }
    return false;
  }

  private static String buildMinimumVersionFailMessage(Collection<SonarAnalyzer> failingAnalyzers) {
    return "The following plugins do not meet the required minimum versions, please upgrade them on your SonarQube server:\n  " +
      failingAnalyzers.stream()
        .map(p -> p.key() + " (installed: " + p.version() + ", minimum: " + p.minimumVersion() + ")")
        .collect(Collectors.joining("\n  "));
  }

  @Override
  public void updateProjectList(IProgressMonitor monitor) {
    doWithEngine(engine -> {
      engine.downloadAllProjects(createEndpointParams(), buildClientWithProxyAndCredentials(),
        new WrappedProgressMonitor(monitor, "Download project list from server '" + getId() + "'"));
      reloadProjects(engine);
    });
  }

  @Override
  public List<ISonarLintProject> getBoundProjects() {
    return ProjectsProviderUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen)
      .filter(p -> {
        SonarLintProjectConfiguration config = SonarLintCorePlugin.loadConfig(p);
        return config.getProjectBinding().filter(b -> id.equals(b.connectionId())).isPresent();
      }).collect(toList());
  }

  public List<RemoteSonarProject> getBoundRemoteProjects(IProgressMonitor monitor) {
    return ProjectsProviderUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen)
      .map(SonarLintCorePlugin::loadConfig)
      .map(SonarLintProjectConfiguration::getProjectBinding)
      .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())
      .filter(c -> c.connectionId().equals(id))
      .map(ProjectBinding::projectKey)
      .distinct()
      .sorted()
      .map(projectKey -> {
        Optional<ServerProject> remoteProject = getRemoteProject(projectKey, monitor);
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
        SonarLintProjectConfiguration config = SonarLintCorePlugin.loadConfig(p);
        return config.getProjectBinding().filter(b -> id.equals(b.connectionId()) && projectKey.equals(b.projectKey())).isPresent();
      }).collect(toList());
  }

  @Override
  public void updateProjectStorage(String projectKey, IProgressMonitor monitor) {
    doWithEngine(engine -> {
      engine.updateProject(createEndpointParams(), buildClientWithProxyAndCredentials(), projectKey,
        false, new WrappedProgressMonitor(monitor, "Update configuration from server '" + getId() + "' for project '" + projectKey + "'"));
      getBoundProjects(projectKey).forEach(p -> {
        ProjectBinding projectBinding = engine.calculatePathPrefixes(projectKey, p.files().stream().map(ISonarLintFile::getProjectRelativePath).collect(toList()));
        String idePathPrefix = projectBinding.idePathPrefix();
        String sqPathPrefix = projectBinding.sqPathPrefix();
        SonarLintLogger.get().debug("Detected prefixes for " + p.getName() + ":\n  IDE prefix: " + idePathPrefix + "\n  Server side prefix: " + sqPathPrefix);
        SonarLintProjectConfiguration config = SonarLintCorePlugin.loadConfig(p);
        config.setProjectBinding(new EclipseProjectBinding(getId(), projectKey, sqPathPrefix, idePathPrefix));
        SonarLintCorePlugin.saveConfig(p, config);
      });
    });
    // Some prefix/suffix might have been changed
    notifyAllListenersStateChanged();
  }

  public static IStatus testConnection(String url, @Nullable String organization, @Nullable String username, @Nullable String password) {
    try {
      OkHttpClient.Builder withProxy = buildOkHttpClientWithProxyAndCredentials(url, username, password);
      ServerApiHelper helper = new ServerApiHelper(createEndpointParams(url, organization), new SonarLintHttpClientOkHttpImpl(withProxy.build()));
      ValidationResult testConnection = new ConnectionValidator(helper).validateConnection();
      if (testConnection.success()) {
        return new Status(IStatus.OK, SonarLintCorePlugin.PLUGIN_ID, "Successfully connected!");
      } else {
        return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, testConnection.message());
      }
    } catch (Exception e) {
      if (e.getCause() instanceof UnknownHostException) {
        return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, "Unknown host: " + url);
      }
      SonarLintLogger.get().debug(e.getMessage(), e);
      return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, e.getMessage(), e);
    }
  }

  public static List<ServerOrganization> listUserOrganizations(String url, String username, String password, IProgressMonitor monitor) {
    OkHttpClient.Builder withProxy = buildOkHttpClientWithProxyAndCredentials(url, username, password);
    EndpointParams endpointPAramsWithoutOrg = createEndpointParams(url, null);
    ServerApi serverApi = new ServerApi(endpointPAramsWithoutOrg, new SonarLintHttpClientOkHttpImpl(withProxy.build()));
    return serverApi.organization().listUserOrganizations(new WrappedProgressMonitor(monitor, "Fetch organizations"));
  }

  public HttpClient buildClientWithProxyAndCredentials() {
    OkHttpClient.Builder withProxy = SonarLintUtils.withProxy(getHost(), SonarLintCorePlugin.getOkHttpClient());
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
    OkHttpClient.Builder withProxy = buildOkHttpClientWithProxyAndCredentials(url, username, password);

    return ServerNotificationsRegistry.isSupported(createEndpointParams(url, organization), new SonarLintHttpClientOkHttpImpl(withProxy.build()));
  }

  private static EndpointParams createEndpointParams(String url, @Nullable String organization) {
    return new EndpointParams(url, getSonarCloudUrl().equals(url), organization);
  }

  private static OkHttpClient.Builder buildOkHttpClientWithProxyAndCredentials(String url, @Nullable String username, @Nullable String password) {
    OkHttpClient.Builder withProxy = SonarLintUtils.withProxy(url, SonarLintCorePlugin.getOkHttpClient());
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
    TextSearchIndex<ServerProject> index = new TextSearchIndex<>();
    for (ServerProject project : allProjectsByKey.values()) {
      index.index(project, project.getKey() + " " + project.getName());
    }
    return index;
  }

  @Override
  public Map<String, ServerProject> getCachedRemoteProjects() {
    return unmodifiableMap(allProjectsByKey);
  }

  @Override
  public Optional<ServerProject> getRemoteProject(String projectKey, IProgressMonitor monitor) {
    ServerProject remoteProjectFromStorage = allProjectsByKey.get(projectKey);
    if (remoteProjectFromStorage != null) {
      return Optional.of(remoteProjectFromStorage);
    } else {
      ServerApi serverApi = new ServerApi(createEndpointParams(), buildClientWithProxyAndCredentials());
      Optional<ServerProject> project = serverApi.project().getProject(projectKey, new WrappedProgressMonitor(monitor, "Fetch project name"));
      if (project.isPresent()) {
        allProjectsByKey.put(projectKey, project.get());
      }
      return project;
    }
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

  public void downloadServerIssues(String projectKey, IProgressMonitor monitor) {
    doWithEngine(
      engine -> engine.downloadServerIssues(createEndpointParams(), buildClientWithProxyAndCredentials(), projectKey, false, new WrappedProgressMonitor(monitor, "Fetch issues")));
  }

  public List<ServerIssue> downloadServerIssues(ProjectBinding projectBinding, String filePath, IProgressMonitor monitor) {
    return withEngine(
      engine -> engine.downloadServerIssues(createEndpointParams(), buildClientWithProxyAndCredentials(), projectBinding, filePath,
        false, new WrappedProgressMonitor(monitor, "Fetch issues")))
          .orElse(emptyList());
  }

  public List<ServerIssue> getServerIssues(ProjectBinding projectBinding, String filePath) {
    return withEngine(engine -> engine.getServerIssues(projectBinding, filePath)).orElse(emptyList());
  }

  public ProjectBinding calculatePathPrefixes(String projectKey, List<String> ideFilePaths) {
    return withEngine(engine -> engine.calculatePathPrefixes(projectKey, ideFilePaths)).orElse(new ProjectBinding(projectKey, "", ""));
  }

  @Override
  public Optional<ServerHotspot> getServerHotspot(String hotspotKey, String projectKey) {
    ServerApi serverApi = new ServerApi(createEndpointParams(), buildClientWithProxyAndCredentials());
    return serverApi.hotspot().fetch(new GetSecurityHotspotRequestParams(hotspotKey, projectKey));
  }
}
