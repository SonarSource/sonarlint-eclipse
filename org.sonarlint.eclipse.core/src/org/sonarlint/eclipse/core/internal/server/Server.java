package org.sonarlint.eclipse.core.internal.server;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintLogOutput;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarsource.sonarlint.core.SonarLintClientImpl;
import org.sonarsource.sonarlint.core.client.api.GlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.SonarLintClient;
import org.sonarsource.sonarlint.core.client.api.analysis.AnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalSyncStatus;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteModule;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration.Builder;
import org.sonarsource.sonarlint.core.client.api.connected.ValidationResult;

public class Server implements IServer {

  private final String id;
  private final String name;
  private final String host;
  private final String username;
  private final String password;
  private final SonarLintClient client;
  private final List<IServerListener> listeners = new ArrayList<>();
  private State state;
  private GlobalSyncStatus syncStatus;

  public Server(String id, String name, String host, String username, String password) {
    this.id = id;
    this.name = name;
    this.host = host;
    this.username = username;
    this.password = password;
    GlobalConfiguration globalConfig = GlobalConfiguration.builder()
      .setServerId(getId())
      .setVerbose(SonarLintCorePlugin.getDefault().isDebugEnabled())
      .setWorkDir(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonarlint").append(getId()).toFile().toPath())
      .setLogOutput(new SonarLintLogOutput())
      .build();
    this.client = new SonarLintClientImpl(globalConfig);
    changeState(State.STOPPED);
  }

  private void changeState(State state) {
    this.state = state;
    for (IServerListener listener : listeners) {
      listener.serverChanged(this);
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public boolean hasAuth() {
    return StringUtils.isNotBlank(getUsername());
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getServerVersion() {
    if (!isStarted()) {
      return "Unavailable";
    }
    if (syncStatus == null) {
      return "Not synced";
    }
    return syncStatus.getServerVersion();
  }

  private boolean isStarted() {
    return state == State.STARTED_NOT_SYNCED || state == State.STARTED_SYNCED;
  }

  @Override
  public String getSyncDate() {
    if (!isStarted()) {
      return "Unavailable";
    }
    if (syncStatus == null) {
      return "Not synced";
    }
    return new SimpleDateFormat().format(syncStatus.getLastSyncDate());
  }

  @Override
  public State getState() {
    return state;
  }

  @Override
  public synchronized void delete() {
    stop();
    SonarLintCorePlugin.getDefault().removeServer(this);
  }

  @Override
  public synchronized void startAnalysis(AnalysisConfiguration config, IssueListener issueListener) {
    if (!isStarted()) {
      tryStart();
    }
    if (!isStarted()) {
      return;
    }
    client.analyze(config, issueListener);
  }

  private void tryStart() {
    changeState(State.STARTING);
    try {
      SonarLintCorePlugin.getDefault().info("Starting SonarLint for server " + getName());
      client.start();
      syncStatus = client.getSyncStatus();
      changeState(syncStatus == null ? State.STARTED_NOT_SYNCED : State.STARTED_SYNCED);
    } catch (Exception e) {
      SonarLintCorePlugin.getDefault().error("Unable to start SonarLint for server " + getName(), e);
      changeState(State.STOPPED);
    }
  }

  @Override
  public synchronized String getHtmlRuleDescription(String ruleKey) {
    if (!isStarted()) {
      tryStart();
    }
    if (!isStarted()) {
      return "Unavailable";
    }
    RuleDetails ruleDetails = client.getRuleDetails(ruleKey);
    return ruleDetails != null ? ruleDetails.getHtmlDescription() : "Not found";
  }

  public synchronized void stop() {
    if (client != null && isStarted()) {
      client.stop();
    }
    changeState(State.STOPPED);
  }

  @Override
  public synchronized void sync(IProgressMonitor monitor) {
    if (isStarted()) {
      stop();
    }
    changeState(State.SYNCING);
    try {
      List<SonarLintProject> projectsToSync = new ArrayList<>();
      for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
        if (project.isAccessible()) {
          SonarLintProject sonarProject = SonarLintProject.getInstance(project);
          if (sonarProject.getServerId().equals(id)) {
            projectsToSync.add(sonarProject);
          }
        }
      }
      monitor.beginTask("Sync server and all associated projects", projectsToSync.size() + 1);
      client.sync(getConfig());
      monitor.worked(1);
      for (SonarLintProject projectToSync : projectsToSync) {
        if (monitor.isCanceled()) {
          return;
        }
        client.syncModule(getConfig(), projectToSync.getModuleKey());
        monitor.worked(1);
      }
      monitor.done();
    } finally {
      tryStart();
    }
  }

  @Override
  public synchronized void syncProject(String moduleKey) {
    if (isStarted()) {
      stop();
    }
    changeState(State.SYNCING);
    try {
      client.syncModule(getConfig(), moduleKey);
    } finally {
      tryStart();
    }
  }

  @Override
  public IStatus testConnection() {
    try {
      ValidationResult testConnection = client.validateCredentials(getConfig());
      if (testConnection.status()) {
        return new Status(IStatus.OK, SonarLintCorePlugin.PLUGIN_ID, "Successfully connected!");
      } else {
        return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, testConnection.statusCode() + ": " + testConnection.message());
      }
    } catch (Exception e) {
      if (e.getCause() instanceof UnknownHostException) {
        return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, "Unknown host: " + getHost());
      }
      SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
      return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, e.getMessage(), e);
    }
  }

  private ServerConfiguration getConfig() {
    Builder builder = ServerConfiguration.builder()
      .url(getHost())
      .credentials(getUsername(), getPassword())
      .userAgent("SonarLint Eclipse " + SonarLintCorePlugin.getDefault().getBundle().getVersion().toString());

    IProxyService proxyService = SonarLintCorePlugin.getDefault().getProxyService();
    IProxyData[] proxyDataForHost;
    try {
      proxyDataForHost = proxyService.select(new URL(host).toURI());
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IllegalStateException("Invalid URL for server " + id + ": " + host, e);
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
    return builder.build();
  }

  @Override
  public List<RemoteModule> findModules(String keyOrPartialName) {
    if (!isStarted()) {
      tryStart();
    }
    if (!isStarted()) {
      return Collections.emptyList();
    }
    return client.searchModule(getConfig(), keyOrPartialName);
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
  public void setVerbose(boolean verbose) {
    client.setVerbose(verbose);
  }

}
