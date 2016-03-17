/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.sonarsource.sonarlint.core.SonarLintEngineImpl;
import org.sonarsource.sonarlint.core.client.api.GlobalConfiguration;
import org.sonarsource.sonarlint.core.client.api.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.SonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.SonarLintEngine.State;
import org.sonarsource.sonarlint.core.client.api.StateListener;
import org.sonarsource.sonarlint.core.client.api.analysis.AnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.GlobalUpdateStatus;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteModule;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration.Builder;
import org.sonarsource.sonarlint.core.client.api.connected.ValidationResult;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;

public class Server implements IServer, StateListener {

  private static final String NEED_UPDATE = "Need update";
  private final String id;
  private final String name;
  private final String host;
  private final boolean hasAuth;
  private final SonarLintEngine client;
  private final List<IServerListener> listeners = new ArrayList<>();
  private GlobalUpdateStatus updateStatus;

  public Server(String id, String name, String host, boolean hasAuth) {
    this.id = id;
    this.name = name;
    this.host = host;
    this.hasAuth = hasAuth;
    GlobalConfiguration globalConfig = GlobalConfiguration.builder()
      .setServerId(getId())
      .setVerbose(SonarLintCorePlugin.getDefault().isDebugEnabled())
      .setWorkDir(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonarlint").append("work").append(getId()).toFile().toPath())
      .setStorageRoot(ResourcesPlugin.getWorkspace().getRoot().getLocation().append(".sonarlint").append("storage").toFile().toPath())
      .setLogOutput(new SonarLintLogOutput())
      .build();
    this.client = new SonarLintEngineImpl(globalConfig);
    this.client.addStateListener(this);
    this.updateStatus = client.getUpdateStatus();
  }

  @Override
  public void stateChanged(State state) {
    notifyAllListeners();
  }

  @Override
  public void notifyAllListeners() {
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
    return hasAuth;
  }

  @Override
  public boolean isUpdated() {
    return updateStatus != null;
  }

  @Override
  public String getServerVersion() {
    if (updateStatus == null) {
      return NEED_UPDATE;
    }
    return updateStatus.getServerVersion();
  }

  @Override
  public String getUpdateDate() {
    if (updateStatus == null) {
      return NEED_UPDATE;
    }
    return new SimpleDateFormat().format(updateStatus.getLastUpdateDate());
  }

  @Override
  public String getSonarLintClientState() {
    switch (client.getState()) {
      case UNKNOW:
        return "Unknown";
      case NEVER_UPDATED:
        return NEED_UPDATE;
      case UPDATED:
        return "Version: " + getServerVersion() + ", Last update: " + getUpdateDate();
      case UPDATING:
        return "Updating...";
      default:
        throw new IllegalArgumentException(client.getState().name());
    }
  }

  @Override
  public synchronized void delete() {
    stop();
    ServersManager.getInstance().removeServer(this);
  }

  @Override
  public void startAnalysis(AnalysisConfiguration config, IssueListener issueListener) {
    client.analyze(config, issueListener);
  }

  @Override
  public synchronized String getHtmlRuleDescription(String ruleKey) {
    RuleDetails ruleDetails = client.getRuleDetails(ruleKey);
    return ruleDetails != null ? ruleDetails.getHtmlDescription() : "Not found";
  }

  public void stop() {
    client.stop();
  }

  @Override
  public synchronized void update(IProgressMonitor monitor) {
    List<SonarLintProject> projectsToUpdate = getBoundProjects();
    monitor.beginTask("Update server and all associated projects", projectsToUpdate.size() + 1);
    updateStatus = client.update(getConfig());
    monitor.worked(1);
    for (SonarLintProject projectToUpdate : projectsToUpdate) {
      if (monitor.isCanceled()) {
        return;
      }
      client.updateModule(getConfig(), projectToUpdate.getModuleKey());
      monitor.worked(1);
    }
    monitor.done();
  }

  @Override
  public List<SonarLintProject> getBoundProjects() {
    List<SonarLintProject> result = new ArrayList<>();
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (project.isAccessible()) {
        SonarLintProject sonarProject = SonarLintProject.getInstance(project);
        if (Objects.equals(sonarProject.getServerId(), id)) {
          result.add(sonarProject);
        }
      }
    }
    return result;
  }

  @Override
  public synchronized void updateProject(String moduleKey) {
    client.updateModule(getConfig(), moduleKey);
  }

  @Override
  public IStatus testConnection(String username, String password) {
    try {
      Builder builder = getConfigBuilderNoCredentials();
      if (StringUtils.isNotBlank(username) || StringUtils.isNotBlank(password)) {
        builder.credentials(username, password);
      }
      ValidationResult testConnection = client.validateCredentials(builder.build());
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
    Builder builder = getConfigBuilderNoCredentials();

    if (hasAuth()) {
      builder.credentials(ServersManager.getUsername(this), ServersManager.getPassword(this));
    }
    return builder.build();
  }

  private Builder getConfigBuilderNoCredentials() {
    Builder builder = ServerConfiguration.builder()
      .url(getHost())
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
    return builder;
  }

  @Override
  public TextSearchIndex<RemoteModule> getModuleIndex() {
    Map<String, RemoteModule> allModulesByKey = client.allModulesByKey();
    TextSearchIndex<RemoteModule> index = new TextSearchIndex<>();
    for (RemoteModule module : allModulesByKey.values()) {
      index.index(module, module.getKey() + " " + module.getName());
    }
    return index;
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

}
