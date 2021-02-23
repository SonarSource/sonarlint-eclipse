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
package org.sonarlint.eclipse.core.internal;

import java.nio.file.Path;
import okhttp3.OkHttpClient;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.engine.StandaloneEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacadeManager;
import org.sonarlint.eclipse.core.internal.event.AnalysisListenerManager;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.http.UserAgentInterceptor;
import org.sonarlint.eclipse.core.internal.notifications.NotificationsManager;
import org.sonarlint.eclipse.core.internal.notifications.NotificationsTracker;
import org.sonarlint.eclipse.core.internal.notifications.NotificationsTrackerRegistry;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfigurationManager;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.core.internal.tracking.IssueStore;
import org.sonarlint.eclipse.core.internal.tracking.IssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.IssueTrackerCacheFactory;
import org.sonarlint.eclipse.core.internal.tracking.IssueTrackerRegistry;
import org.sonarlint.eclipse.core.internal.tracking.PersistentIssueTrackerCache;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueUpdater;
import org.sonarlint.eclipse.core.internal.utils.NodeJsManager;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class SonarLintCorePlugin extends Plugin {

  public static final String PLUGIN_ID = "org.sonarlint.eclipse.core";
  public static final String UI_PLUGIN_ID = "org.sonarlint.eclipse.ui";
  public static final String MARKER_ON_THE_FLY_ID = PLUGIN_ID + ".sonarlintOnTheFlyProblem";
  public static final String MARKER_ON_THE_FLY_FLOW_ID = PLUGIN_ID + ".sonarlintOnTheFlyFlowLocation";
  public static final String MARKER_REPORT_ID = PLUGIN_ID + ".sonarlintReportProblem";
  public static final String MARKER_REPORT_FLOW_ID = PLUGIN_ID + ".sonarlintReportFlowLocation";
  public static final String MARKER_HOTSPOT_ID = PLUGIN_ID + ".sonarlintHotspot";

  private static SonarLintCorePlugin plugin;
  private static SonarLintProjectConfigurationManager configManager;
  private static NotificationsManager notificationsManager;

  private IssueTrackerRegistry issueTrackerRegistry;
  private ServerIssueUpdater serverIssueUpdater;

  private StandaloneEngineFacade sonarlint;
  private final ServiceTracker<IProxyService, IProxyService> proxyTracker;

  private final AnalysisListenerManager analysisListenerManager = new AnalysisListenerManager();
  private final SonarLintTelemetry telemetry = new SonarLintTelemetry();
  private ConnectedEngineFacadeManager serversManager = null;

  private NotificationsTrackerRegistry notificationsTrackerRegistry;
  private NodeJsManager nodeJsManager;

  private final OkHttpClient okhttpClient;

  public SonarLintCorePlugin() {
    plugin = this;
    proxyTracker = new ServiceTracker<>(FrameworkUtil.getBundle(this.getClass()).getBundleContext(), IProxyService.class, null);
    proxyTracker.open();
    okhttpClient = new OkHttpClient.Builder()
      .addNetworkInterceptor(new UserAgentInterceptor("SonarLint Eclipse " + SonarLintUtils.getPluginVersion()))
      .build();
  }

  public static SonarLintCorePlugin getInstance() {
    return plugin;
  }

  public synchronized SonarLintProjectConfigurationManager getProjectConfigManager() {
    if (configManager == null) {
      configManager = new SonarLintProjectConfigurationManager();
    }
    return configManager;
  }

  public synchronized NotificationsManager notificationsManager() {
    if (notificationsManager == null) {
      notificationsManager = new NotificationsManager();
    }
    return notificationsManager;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    IssueTrackerCacheFactory factory = project -> {
      Path storeBasePath = StoragePathManager.getIssuesDir(project);
      IssueStore issueStore = new IssueStore(storeBasePath, project);
      return new PersistentIssueTrackerCache(issueStore);
    };
    issueTrackerRegistry = new IssueTrackerRegistry(factory);

    serverIssueUpdater = new ServerIssueUpdater(issueTrackerRegistry);

    notificationsTrackerRegistry = new NotificationsTrackerRegistry();

    nodeJsManager = new NodeJsManager();

    startupAsync();
  }

  public void startupAsync() {
    // SLE-122 Delay a little bit to let the time to the workspace to initialize (and avoid NPE)
    new StartupJob().schedule(2000);
  }

  private class StartupJob extends Job {

    StartupJob() {
      super("SonarLint Core startup");
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
      startTelemetry();
      return Status.OK_STATUS;
    }

    private void startTelemetry() {
      if (SonarLintTelemetry.shouldBeActivated()) {
        telemetry.init();
      } else {
        SonarLintLogger.get().info("Telemetry disabled");
      }
    }
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    telemetry.stop();

    if (sonarlint != null) {
      sonarlint.stop();
    }
    proxyTracker.close();

    issueTrackerRegistry.shutdown();
    if (serversManager != null) {
      serversManager.stop();
    }
    if (notificationsManager != null) {
      notificationsManager.stop();
    }
    SonarLintExtensionTracker.close();

    super.stop(context);
  }

  public StandaloneEngineFacade getDefaultSonarLintClientFacade() {
    if (sonarlint == null) {
      sonarlint = new StandaloneEngineFacade();
    }
    return sonarlint;
  }

  public IProxyService getProxyService() {
    return proxyTracker.getService();
  }

  public ServerIssueUpdater getServerIssueUpdater() {
    return serverIssueUpdater;
  }

  public static IssueTracker getOrCreateIssueTracker(ISonarLintProject project) {
    return getInstance().issueTrackerRegistry.getOrCreate(project);
  }

  public static void clearIssueTracker(ISonarLintProject project) {
    getInstance().issueTrackerRegistry.get(project).ifPresent(IssueTracker::clear);
  }

  public static AnalysisListenerManager getAnalysisListenerManager() {
    return getInstance().analysisListenerManager;
  }

  public static SonarLintTelemetry getTelemetry() {
    return getInstance().telemetry;
  }

  public static NodeJsManager getNodeJsManager() {
    return getInstance().nodeJsManager;
  }

  public static OkHttpClient getOkHttpClient() {
    return getInstance().okhttpClient;
  }

  public static synchronized ConnectedEngineFacadeManager getServersManager() {
    if (getInstance().serversManager == null) {
      getInstance().serversManager = new ConnectedEngineFacadeManager();
      getInstance().serversManager.init();
    }
    return getInstance().serversManager;
  }

  public static NotificationsTracker getOrCreateNotificationsTracker(ISonarLintProject project) {
    return getInstance().notificationsTrackerRegistry.getOrCreate(project);
  }

  public static SonarLintProjectConfiguration loadConfig(ISonarLintProject project) {
    return getInstance().getProjectConfigManager().load(project.getScopeContext(), project.getName());
  }

  public static void saveConfig(ISonarLintProject project, SonarLintProjectConfiguration config) {
    getInstance().getProjectConfigManager().save(project.getScopeContext(), config);
  }
}
