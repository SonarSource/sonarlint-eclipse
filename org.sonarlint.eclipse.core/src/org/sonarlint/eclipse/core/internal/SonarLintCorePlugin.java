/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.sonarlint.eclipse.core.internal.event.AnalysisListenerManager;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.jobs.StandaloneSonarLintClientFacade;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectManager;
import org.sonarlint.eclipse.core.internal.tracking.IssueStore;
import org.sonarlint.eclipse.core.internal.tracking.IssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.IssueTrackerCacheFactory;
import org.sonarlint.eclipse.core.internal.tracking.IssueTrackerRegistry;
import org.sonarlint.eclipse.core.internal.tracking.PersistentIssueTrackerCache;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueUpdater;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class SonarLintCorePlugin extends Plugin {

  public static final String PLUGIN_ID = "org.sonarlint.eclipse.core";
  public static final String UI_PLUGIN_ID = "org.sonarlint.eclipse.ui";
  public static final String MARKER_ID = PLUGIN_ID + ".sonarlintProblem";
  public static final String MARKER_CHANGESET_ID = PLUGIN_ID + ".sonarlintChangeSetProblem";

  private static SonarLintCorePlugin plugin;
  private static SonarLintProjectManager projectManager;

  private IssueTrackerRegistry issueTrackerRegistry;
  private ServerIssueUpdater serverIssueUpdater;

  private StandaloneSonarLintClientFacade sonarlint;
  private final ServiceTracker<IProxyService, IProxyService> proxyTracker;
  private final SonarLintExtensionTracker extensionTracker = new SonarLintExtensionTracker();

  private AnalysisListenerManager analysisListenerManager = new AnalysisListenerManager();

  public SonarLintCorePlugin() {
    plugin = this;
    proxyTracker = new ServiceTracker<>(FrameworkUtil.getBundle(this.getClass()).getBundleContext(), IProxyService.class, null);
    proxyTracker.open();
  }

  public static SonarLintCorePlugin getDefault() {
    return plugin;
  }

  public synchronized SonarLintProjectManager getProjectManager() {
    if (projectManager == null) {
      projectManager = new SonarLintProjectManager();
    }
    return projectManager;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    extensionTracker.start();

    IssueTrackerCacheFactory factory = (project, localModuleKey) -> {
      Path storeBasePath = StorageManager.getIssuesDir(localModuleKey);
      IssueStore issueStore = new IssueStore(storeBasePath, project);
      return new PersistentIssueTrackerCache(issueStore);
    };
    issueTrackerRegistry = new IssueTrackerRegistry(factory);

    serverIssueUpdater = new ServerIssueUpdater(issueTrackerRegistry);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    if (sonarlint != null) {
      sonarlint.stop();
    }
    proxyTracker.close();

    issueTrackerRegistry.shutdown();
    extensionTracker.close();

    super.stop(context);
  }

  public StandaloneSonarLintClientFacade getDefaultSonarLintClientFacade() {
    if (sonarlint == null) {
      sonarlint = new StandaloneSonarLintClientFacade();
    }
    return sonarlint;
  }

  public IProxyService getProxyService() {
    return proxyTracker.getService();
  }

  public ServerIssueUpdater getServerIssueUpdater() {
    return serverIssueUpdater;
  }

  public static IssueTracker getOrCreateIssueTracker(ISonarLintProject project, String localModulePath) {
    return getDefault().issueTrackerRegistry.getOrCreate(project, localModulePath);
  }

  public static void clearIssueTracker(ISonarLintProject project) {
    getDefault().issueTrackerRegistry.get(project).ifPresent(IssueTracker::clear);
  }

  public static AnalysisListenerManager getAnalysisListenerManager() {
    return getDefault().analysisListenerManager;
  }

  public static SonarLintExtensionTracker getExtensionTracker() {
    return getDefault().extensionTracker;
  }
}
