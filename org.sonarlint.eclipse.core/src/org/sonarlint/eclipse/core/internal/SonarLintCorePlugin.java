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
package org.sonarlint.eclipse.core.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;
import org.sonarlint.eclipse.core.AbstractPlugin;
import org.sonarlint.eclipse.core.internal.jobs.LogListener;
import org.sonarlint.eclipse.core.internal.jobs.StandaloneSonarLintClientFacade;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectManager;
import org.sonarlint.eclipse.core.internal.tracking.IssueStore;
import org.sonarlint.eclipse.core.internal.tracking.IssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.IssueTrackerCacheFactory;
import org.sonarlint.eclipse.core.internal.tracking.IssueTrackerRegistry;
import org.sonarlint.eclipse.core.internal.tracking.PersistentIssueTrackerCache;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueUpdater;

public class SonarLintCorePlugin extends AbstractPlugin {

  public static final String PLUGIN_ID = "org.sonarlint.eclipse.core";
  public static final String UI_PLUGIN_ID = "org.sonarlint.eclipse.ui";
  public static final String MARKER_ID = PLUGIN_ID + ".sonarlintProblem";
  public static final String MARKER_CHANGESET_ID = PLUGIN_ID + ".sonarlintChangeSetProblem";

  private static SonarLintCorePlugin plugin;
  private static SonarLintProjectManager projectManager;

  private IssueTrackerRegistry issueTrackerRegistry;
  private ServerIssueUpdater serverIssueUpdater;

  private final List<LogListener> logListeners = new ArrayList<>();
  private StandaloneSonarLintClientFacade sonarlint;
  private final ServiceTracker proxyTracker;
  private SonarLintChangeListener sonarLintChangeListener;

  public SonarLintCorePlugin() {
    plugin = this;
    proxyTracker = new ServiceTracker(FrameworkUtil.getBundle(this.getClass()).getBundleContext(), IProxyService.class.getName(), null);
    proxyTracker.open();
  }

  public static SonarLintCorePlugin getDefault() {
    return plugin;
  }

  public void addLogListener(LogListener listener) {
    logListeners.add(listener);
  }

  public void removeLogListener(LogListener listener) {
    logListeners.remove(listener);
  }

  public void error(String msg) {
    for (LogListener listener : logListeners) {
      listener.error(msg);
    }
  }

  public void error(String msg, Throwable t) {
    for (LogListener listener : logListeners) {
      listener.error(msg);
      StringWriter stack = new StringWriter();
      t.printStackTrace(new PrintWriter(stack));
      listener.error(stack.toString());
    }
  }

  public void info(String msg) {
    for (LogListener listener : logListeners) {
      listener.info(msg);
    }
  }

  public void debug(String msg) {
    for (LogListener listener : logListeners) {
      listener.debug(msg);
    }
  }

  public void debug(String msg, Throwable t) {
    for (LogListener listener : logListeners) {
      listener.debug(msg);
      StringWriter stack = new StringWriter();
      t.printStackTrace(new PrintWriter(stack));
      listener.debug(stack.toString());
    }
  }

  public void warn(String msg, Throwable t) {
    for (LogListener listener : logListeners) {
      listener.warn(msg);
      StringWriter stack = new StringWriter();
      t.printStackTrace(new PrintWriter(stack));
      listener.warn(stack.toString());
    }
  }

  public synchronized SonarLintProjectManager getProjectManager() {
    if (projectManager == null) {
      projectManager = new SonarLintProjectManager();
    }
    return projectManager;
  }

  @Override
  public void start(BundleContext context) {
    super.start(context);
    sonarLintChangeListener = new SonarLintChangeListener();
    ResourcesPlugin.getWorkspace().addResourceChangeListener(sonarLintChangeListener, IResourceChangeEvent.POST_CHANGE);

    IssueTrackerCacheFactory factory = (project, localModuleKey) -> {
      Path projectBasePath = Paths.get(project.getLocation().toString());
      Path storeBasePath = StorageManager.getIssuesDir(localModuleKey);
      IssueStore issueStore = new IssueStore(storeBasePath, projectBasePath);
      return new PersistentIssueTrackerCache(issueStore);
    };
    issueTrackerRegistry = new IssueTrackerRegistry(factory);

    serverIssueUpdater = new ServerIssueUpdater(issueTrackerRegistry);
  }

  @Override
  public void stop(BundleContext context) {
    ResourcesPlugin.getWorkspace().removeResourceChangeListener(sonarLintChangeListener);
    if (sonarlint != null) {
      sonarlint.stop();
    }
    proxyTracker.close();

    serverIssueUpdater.shutdown();
    issueTrackerRegistry.shutdown();

    super.stop(context);
  }

  public StandaloneSonarLintClientFacade getDefaultSonarLintClientFacade() {
    if (sonarlint == null) {
      sonarlint = new StandaloneSonarLintClientFacade();
    }
    return sonarlint;
  }

  public IProxyService getProxyService() {
    return (IProxyService) proxyTracker.getService();
  }

  public ServerIssueUpdater getServerIssueUpdater() {
    return serverIssueUpdater;
  }

  public static IssueTracker getOrCreateIssueTracker(IProject project, String localModulePath) {
    return getDefault().issueTrackerRegistry.getOrCreate(project, localModulePath);
  }

  public static void clearIssueTracker(IProject project) {
    getDefault().issueTrackerRegistry.get(project).ifPresent(IssueTracker::clear);
  }
}
