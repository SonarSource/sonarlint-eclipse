/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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

import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionManager;
import org.sonarlint.eclipse.core.internal.event.AnalysisListenerManager;
import org.sonarlint.eclipse.core.internal.extension.AbstractSonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfigurationManager;
import org.sonarlint.eclipse.core.internal.tracking.ProjectIssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.ProjectIssueTrackers;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class SonarLintCorePlugin extends Plugin {

  public static final String PLUGIN_ID = "org.sonarlint.eclipse.core";
  public static final String UI_PLUGIN_ID = "org.sonarlint.eclipse.ui";
  public static final String MARKER_ON_THE_FLY_ID = PLUGIN_ID + ".sonarlintOnTheFlyProblem";
  public static final String MARKER_ON_THE_FLY_FLOW_ID = PLUGIN_ID + ".sonarlintOnTheFlyFlowLocation";
  public static final String MARKER_ON_THE_FLY_QUICK_FIX_ID = PLUGIN_ID + ".sonarlintOnTheFlyQFLocation";
  public static final String MARKER_REPORT_ID = PLUGIN_ID + ".sonarlintReportProblem";
  public static final String MARKER_REPORT_FLOW_ID = PLUGIN_ID + ".sonarlintReportFlowLocation";
  public static final String MARKER_HOTSPOT_ID = PLUGIN_ID + ".sonarlintHotspot";
  public static final String MARKER_TAINT_ID = PLUGIN_ID + ".sonarlintTaintVulnerability";
  public static final String MARKER_TAINT_FLOW_ID = PLUGIN_ID + ".sonarlintTaintVulnerabilityFlowLocation";

  private static SonarLintCorePlugin plugin;
  private static SonarLintProjectConfigurationManager configManager;

  private ProjectIssueTrackers issueTrackerRegistry;

  private final ServiceTracker<IProxyService, IProxyService> proxyTracker;

  private final AnalysisListenerManager analysisListenerManager = new AnalysisListenerManager();
  private ConnectionManager connectionsManager = null;

  public SonarLintCorePlugin() {
    plugin = this;
    proxyTracker = new ServiceTracker<>(FrameworkUtil.getBundle(this.getClass()).getBundleContext(), IProxyService.class, null);
    proxyTracker.open();
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

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    issueTrackerRegistry = new ProjectIssueTrackers();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(issueTrackerRegistry);
    SonarLintGlobalConfiguration.init();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    SonarLintBackendService.get().stop();
    proxyTracker.close();

    ResourcesPlugin.getWorkspace().removeResourceChangeListener(issueTrackerRegistry);
    issueTrackerRegistry.shutdown();
    if (connectionsManager != null) {
      connectionsManager.stop();
    }
    SonarLintGlobalConfiguration.stop();
    SonarLintExtensionTracker.close();
    AbstractSonarLintExtensionTracker.closeTracker();

    super.stop(context);
  }

  @Nullable
  public IProxyService getProxyService() {
    return proxyTracker.getService();
  }

  public static ProjectIssueTracker getOrCreateIssueTracker(ISonarLintProject project) {
    return getInstance().issueTrackerRegistry.getOrCreate(project);
  }

  public static void clearIssueTracker(ISonarLintProject project) {
    getInstance().issueTrackerRegistry.get(project).ifPresent(ProjectIssueTracker::clear);
  }

  public static AnalysisListenerManager getAnalysisListenerManager() {
    return getInstance().analysisListenerManager;
  }

  public static synchronized ConnectionManager getConnectionManager() {
    if (getInstance().connectionsManager == null) {
      getInstance().connectionsManager = new ConnectionManager();
      getInstance().connectionsManager.init();
    }
    return getInstance().connectionsManager;
  }

  public static SonarLintProjectConfiguration loadConfig(ISonarLintProject project) {
    return getInstance().getProjectConfigManager().load(project.getScopeContext(), project.getName());
  }

  public static void saveConfig(ISonarLintProject project, SonarLintProjectConfiguration config) {
    getInstance().getProjectConfigManager().save(project.getScopeContext(), config);
  }

}
