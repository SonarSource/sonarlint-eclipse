/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.server;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.security.storage.StorageException;
import org.osgi.framework.Version;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.StoragePathManager;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintAnalyzerLogOutput;
import org.sonarlint.eclipse.core.internal.jobs.WrappedProgressMonitor;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.ConnectedSonarLintEngineImpl;
import org.sonarsource.sonarlint.core.WsHelperImpl;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedGlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine.State;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalStorageStatus;
import org.sonarsource.sonarlint.core.client.api.connected.ProjectBinding;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteOrganization;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteProject;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration.Builder;
import org.sonarsource.sonarlint.core.client.api.connected.SonarAnalyzer;
import org.sonarsource.sonarlint.core.client.api.connected.StateListener;
import org.sonarsource.sonarlint.core.client.api.connected.StorageUpdateCheckResult;
import org.sonarsource.sonarlint.core.client.api.connected.UpdateResult;
import org.sonarsource.sonarlint.core.client.api.connected.ValidationResult;
import org.sonarsource.sonarlint.core.client.api.connected.WsHelper;
import org.sonarsource.sonarlint.core.client.api.exceptions.DownloadException;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;
import org.sonarsource.sonarlint.core.notifications.SonarQubeNotifications;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toList;

public class Server implements IServer, StateListener {

  public static final String SONARCLOUD_URL = "https://sonarcloud.io";
  public static final String OLD_SONARCLOUD_URL = "https://sonarqube.com";

  private static final String NEED_UPDATE = "Need data update";
  private final String id;
  private String host;
  private String organization;
  private boolean hasAuth;
  private final ConnectedSonarLintEngine client;
  private final List<IServerListener> listeners = new ArrayList<>();
  private GlobalStorageStatus updateStatus;
  private boolean hasUpdates;
  private boolean notificationsEnabled;
  // Cache the project list to avoid dead lock
  private Map<String, RemoteProject> allProjectsByKey = new ConcurrentHashMap<>();

  Server(String id) {
    this.id = id;
    ConnectedGlobalConfiguration globalConfig = ConnectedGlobalConfiguration.builder()
      .setServerId(getId())
      .setWorkDir(StoragePathManager.getServerWorkDir(getId()))
      .setStorageRoot(StoragePathManager.getServerStorageRoot())
      .setLogOutput(new SonarLintAnalyzerLogOutput())
      .build();
    this.client = new ConnectedSonarLintEngineImpl(globalConfig);
    this.client.addStateListener(this);
    this.updateStatus = client.getGlobalStorageStatus();
    if (client.getState().equals(State.UPDATED)) {
      reloadProjects();
    }
  }

  private void reloadProjects() {
    this.allProjectsByKey.clear();
    this.allProjectsByKey.putAll(client.allProjectsByKey());
  }

  @Override
  public void stateChanged(State state) {
    if (state.equals(State.UPDATED)) {
      reloadProjects();
    }
    notifyAllListeners();
  }

  @Override
  public void notifyAllListeners() {
    for (IServerListener listener : listeners) {
      listener.accept(this);
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

  public Server setHost(String host) {
    this.host = host;
    return this;
  }

  @Override
  public String getOrganization() {
    return organization;
  }

  public Server setOrganization(@Nullable String organization) {
    this.organization = organization;
    return this;
  }

  @Override
  public boolean hasAuth() {
    return hasAuth;
  }

  public Server setHasAuth(boolean hasAuth) {
    this.hasAuth = hasAuth;
    return this;
  }

  @Override
  public boolean isStorageUpdated() {
    return client.getState() == State.UPDATED;
  }

  @Override
  public void checkForUpdates(IProgressMonitor progress) {
    this.hasUpdates = false;
    try {
      SubMonitor subMonitor = SubMonitor.convert(progress, getBoundProjects().size() + 1);
      SubMonitor globalMonitor = subMonitor.newChild(1);
      SonarLintLogger.get().info("Check for updates from server '" + getId() + "'");
      StorageUpdateCheckResult checkForUpdateResult = client.checkIfGlobalStorageNeedUpdate(getConfig(),
        new WrappedProgressMonitor(globalMonitor, "Check for configuration updates on server '" + getId() + "'"));
      if (checkForUpdateResult.needUpdate()) {
        this.hasUpdates = true;
        checkForUpdateResult.changelog().forEach(line -> SonarLintLogger.get().info("  - " + line));
      }

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
        StorageUpdateCheckResult projectUpdateCheckResult = client.checkIfProjectStorageNeedUpdate(getConfig(), projectKey,
          new WrappedProgressMonitor(projectMonitor, "Checking for binding data update for project '" + projectKey + "'"));
        if (projectUpdateCheckResult.needUpdate()) {
          this.hasUpdates = true;
          SonarLintLogger.get().info("For project '" + projectKey + "':");
          projectUpdateCheckResult.changelog().forEach(line -> SonarLintLogger.get().info("  - " + line));
        }
      }
    } catch (DownloadException e) {
      // If server is not reachable, just ignore
      SonarLintLogger.get().debug("Unable to check for binding data updates on '" + getId() + "'", e);
    } finally {
      notifyAllListeners();
    }
  }

  @Override
  public boolean hasUpdates() {
    return hasUpdates;
  }

  @Override
  public String getServerVersion() {
    if (!isStorageUpdated()) {
      return NEED_UPDATE;
    }
    return updateStatus.getServerVersion();
  }

  @Override
  public String getUpdateDate() {
    if (!isStorageUpdated()) {
      return NEED_UPDATE;
    }
    return new SimpleDateFormat().format(updateStatus.getLastUpdateDate());
  }

  @Override
  public boolean isUpdating() {
    return State.UPDATING == client.getState();
  }

  @Override
  public String getSonarLintEngineState() {
    switch (client.getState()) {
      case UNKNOW:
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
        throw new IllegalArgumentException(client.getState().name());
    }
  }

  @Override
  public synchronized void delete() {
    client.stop(true);
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
    project.deleteAllMarkers(SonarLintCorePlugin.MARKER_REPORT_ID);
    SonarLintCorePlugin.clearIssueTracker(project);
  }

  @Override
  public void updateConfig(String url, @Nullable String organization, String username, String password, boolean notificationsEnabled) {
    this.host = url;
    this.organization = organization;
    this.hasAuth = StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password);
    this.notificationsEnabled = notificationsEnabled;
    SonarLintCorePlugin.getServersManager().updateServer(this, username, password);
  }

  @Override
  public AnalysisResults runAnalysis(ConnectedAnalysisConfiguration config, IssueListener issueListener, IProgressMonitor monitor) {
    return client.analyze(config, issueListener, null, new WrappedProgressMonitor(monitor, "Analysis"));
  }

  @Override
  public synchronized RuleDetails getRuleDescription(String ruleKey) {
    return client.getRuleDetails(ruleKey);
  }

  public void stop() {
    client.stop(false);
  }

  @Override
  public synchronized void updateStorage(IProgressMonitor monitor) {
    UpdateResult updateResult = client.update(getConfig(), new WrappedProgressMonitor(monitor, "Update configuration from server '" + getId() + "'"));
    Collection<SonarAnalyzer> tooOld = updateResult.analyzers().stream()
      .filter(SonarAnalyzer::sonarlintCompatible)
      .filter(Server::tooOld)
      .collect(Collectors.toList());
    if (!tooOld.isEmpty()) {
      SonarLintLogger.get().error(buildMinimumVersionFailMessage(tooOld));
    }
    updateStatus = updateResult.status();
    hasUpdates = false;
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
    client.downloadAllProjects(getConfig(), new WrappedProgressMonitor(monitor, "Download project list from server '" + getId() + "'"));
    reloadProjects();
  }

  @Override
  public List<ISonarLintProject> getBoundProjects() {
    return ProjectsProviderUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen)
      .filter(p -> {
        SonarLintProjectConfiguration config = SonarLintCorePlugin.loadConfig(p);
        return config.getProjectBinding().filter(b -> id.equals(b.serverId())).isPresent();
      }).collect(toList());
  }

  public List<RemoteSonarProject> getBoundRemoteProjects(IProgressMonitor monitor) {
    return ProjectsProviderUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen)
      .map(SonarLintCorePlugin::loadConfig)
      .map(SonarLintProjectConfiguration::getProjectBinding)
      .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())
      .filter(c -> c.serverId().equals(id))
      .map(ProjectBinding::projectKey)
      .distinct()
      .sorted()
      .map(projectKey -> {
        Optional<RemoteProject> remoteProject = getRemoteProject(projectKey, monitor);
        if (remoteProject.isPresent()) {
          return new RemoteSonarProject(id, remoteProject.get().getKey(), remoteProject.get().getName());
        } else {
          return new RemoteSonarProject(id, projectKey, "<unknown>");
        }
      })
      .collect(toList());
  }

  public List<ISonarLintProject> getBoundProjects(String projectKey) {
    return ProjectsProviderUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen)
      .filter(p -> {
        SonarLintProjectConfiguration config = SonarLintCorePlugin.loadConfig(p);
        return config.getProjectBinding().filter(b -> id.equals(b.serverId()) && projectKey.equals(b.projectKey())).isPresent();
      }).collect(toList());
  }

  @Override
  public synchronized void updateProjectStorage(String projectKey, IProgressMonitor monitor) {
    client.updateProject(getConfig(), projectKey, new WrappedProgressMonitor(monitor, "Update configuration from server '" + getId() + "' for project '" + projectKey + "'"));
    getBoundProjects(projectKey).forEach(p -> {
      ProjectBinding projectBinding = client.calculatePathPrefixes(projectKey, p.files().stream().map(ISonarLintFile::getProjectRelativePath).collect(toList()));
      String idePathPrefix = projectBinding.idePathPrefix();
      String sqPathPrefix = projectBinding.sqPathPrefix();
      SonarLintLogger.get().debug("Detected prefixes for " + p.getName() + ":\n  IDE prefix: " + idePathPrefix + "\n  Server side prefix: " + sqPathPrefix);
      SonarLintProjectConfiguration config = SonarLintCorePlugin.loadConfig(p);
      config.setProjectBinding(new EclipseProjectBinding(getId(), projectKey, sqPathPrefix, idePathPrefix));
      SonarLintCorePlugin.saveConfig(p, config);
    });
    // Some prefix/suffix might have been changed
    notifyAllListeners();
  }

  public static IStatus testConnection(String url, @Nullable String organization, @Nullable String username, @Nullable String password) {
    try {
      Builder builder = getConfigBuilderNoCredentials(url, organization);
      if (StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password)) {
        builder.credentials(username, password);
      }
      WsHelper helper = new WsHelperImpl();
      ValidationResult testConnection = helper.validateConnection(builder.build());
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

  public static List<RemoteOrganization> listUserOrganizations(String url, String username, String password, IProgressMonitor monitor) {
    Builder builder = getConfigBuilderNoCredentials(url, null);
    if (StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password)) {
      builder.credentials(username, password);
    }
    WsHelper helper = new WsHelperImpl();
    return helper.listUserOrganizations(builder.build(), new WrappedProgressMonitor(monitor, "Fetch organizations"));
  }

  public ServerConfiguration getConfig() {
    Builder builder = getConfigBuilderNoCredentials(getHost(), getOrganization());

    if (hasAuth()) {
      try {
        builder.credentials(ServersManager.getUsername(this), ServersManager.getPassword(this));
      } catch (StorageException e) {
        throw new IllegalStateException("Unable to read server credentials from storage: " + e.getMessage(), e);
      }
    }
    return builder.build();
  }

  private static Builder getConfigBuilderNoCredentials(String url, @Nullable String organization) {
    Builder builder = ServerConfiguration.builder()
      .url(url)
      .organizationKey(organization)
      .userAgent("SonarLint Eclipse " + SonarLintUtils.getPluginVersion());

    IProxyService proxyService = SonarLintCorePlugin.getInstance().getProxyService();
    IProxyData[] proxyDataForHost;
    try {
      proxyDataForHost = proxyService.select(new URL(url).toURI());
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IllegalStateException("Invalid URL for server: " + url, e);
    }

    for (IProxyData data : proxyDataForHost) {
      if (data.getHost() != null) {
        builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(data.getHost(), data.getPort())));
        if (data.isRequiresAuthentication()) {
          builder.proxyCredentials(data.getUserId(), data.getPassword());
        }
        break;
      }
    }
    return builder;
  }

  public static boolean checkNotificationsSupported(String url, @Nullable String organization, String username, String password) {
    Builder builder = Server.getConfigBuilderNoCredentials(url, organization);
    if (StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password)) {
      builder.credentials(username, password);
    }

    return SonarQubeNotifications.get().isSupported(builder.build());
  }

  @Override
  public TextSearchIndex<RemoteProject> computeProjectIndex() {
    TextSearchIndex<RemoteProject> index = new TextSearchIndex<>();
    for (RemoteProject project : allProjectsByKey.values()) {
      index.index(project, project.getKey() + " " + project.getName());
    }
    return index;
  }

  @Override
  public Map<String, RemoteProject> getCachedRemoteProjects() {
    return unmodifiableMap(allProjectsByKey);
  }

  @Override
  public Optional<RemoteProject> getRemoteProject(String projectKey, IProgressMonitor monitor) {
    RemoteProject remoteProjectFromStorage = allProjectsByKey.get(projectKey);
    if (remoteProjectFromStorage != null) {
      return Optional.of(remoteProjectFromStorage);
    } else {
      WsHelper helper = new WsHelperImpl();
      Optional<RemoteProject> project = helper.getProject(getConfig(), projectKey, new WrappedProgressMonitor(monitor, "Fetch project name"));
      if (project.isPresent()) {
        allProjectsByKey.put(projectKey, project.get());
      }
      return project;
    }
  }

  @Override
  public List<ISonarLintFile> getServerFileExclusions(ProjectBinding binding, Collection<ISonarLintFile> files, Predicate<ISonarLintFile> testFilePredicate) {
    return client.getExcludedFiles(binding, files, ISonarLintFile::getProjectRelativePath, testFilePredicate);
  }

  @Override
  public void addServerListener(IServerListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeServerListener(IServerListener listener) {
    listeners.remove(listener);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Server)) {
      return false;
    }
    return ((Server) obj).getId().equals(this.getId());
  }

  @Override
  public int hashCode() {
    return getId().hashCode();
  }

  public ConnectedSonarLintEngine getEngine() {
    return client;
  }

  @Override
  public boolean isSonarCloud() {
    return SONARCLOUD_URL.equals(this.host);
  }

  @Override
  public boolean areNotificationsEnabled() {
    return notificationsEnabled;
  }

  public Server setNotificationsEnabled(boolean value) {
    this.notificationsEnabled = value;
    return this;
  }
}
