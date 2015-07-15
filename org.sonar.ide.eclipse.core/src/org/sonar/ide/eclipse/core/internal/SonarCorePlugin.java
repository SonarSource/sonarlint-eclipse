/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.core.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.ArrayUtils;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;
import org.sonar.ide.eclipse.core.AbstractPlugin;
import org.sonar.ide.eclipse.core.internal.jobs.LogListener;
import org.sonar.ide.eclipse.core.internal.resources.SonarFile;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.resources.SonarProjectManager;
import org.sonar.ide.eclipse.core.internal.resources.SonarResource;
import org.sonar.ide.eclipse.core.internal.servers.ISonarServersManager;
import org.sonar.ide.eclipse.core.internal.servers.ServersManager;
import org.sonar.ide.eclipse.core.resources.ISonarFile;
import org.sonar.ide.eclipse.core.resources.ISonarResource;

public class SonarCorePlugin extends AbstractPlugin {
  public static final String PLUGIN_ID = "org.sonar.ide.eclipse.core";

  public static final String MARKER_ID = PLUGIN_ID + ".sonarProblem";
  public static final String NEW_ISSUE_MARKER_ID = PLUGIN_ID + ".sonarProblemNewIssue";

  private static SonarCorePlugin plugin;

  public SonarCorePlugin() {
    plugin = this;
  }

  public static SonarCorePlugin getDefault() {
    return plugin;
  }

  private ISonarServersManager serversManager;
  private final List<LogListener> logListeners = new ArrayList<LogListener>();

  public void addLogListener(final LogListener listener) {
    logListeners.add(listener);
  }

  public void removeLogListener(final LogListener listener) {
    logListeners.remove(listener);
  }

  public void error(final String msg) {
    for (final LogListener listener : logListeners) {
      listener.error(msg);
    }
  }

  public void error(final String msg, final Throwable t) {
    for (final LogListener listener : logListeners) {
      listener.error(msg);
      final StringWriter errors = new StringWriter();
      t.printStackTrace(new PrintWriter(errors));
      listener.error(errors.toString());
    }
  }

  public void info(final String msg) {
    for (final LogListener listener : logListeners) {
      listener.info(msg);
    }
  }

  public void debug(final String msg) {
    for (final LogListener listener : logListeners) {
      listener.debug(msg);
    }
  }

  @SuppressWarnings("nls")
  @Override
  public void start(final BundleContext context) {
    super.start(context);

    serversManager = new ServersManager();
    scheduleStartupJobs();

  }

  public void scheduleStartupJobs() {

    // 1) Server-manager builder back group task.
    final Job cacheBuilder = new Job("Sonar server cache builder")
    {
      @Override
      protected IStatus run(final IProgressMonitor monitor)
      {
        serversManager.getServers();
        return Status.OK_STATUS;
      }
    };
    cacheBuilder.schedule();

    // 2) Backward compatibility project with Sonar nature to make use of SonarBuilder.
    final Job job = new Job("Sonar projects builder set-up") {

      @Override
      protected IStatus run(final IProgressMonitor monitor) {

        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if (workspace != null) {
          final IWorkspaceRoot root = workspace.getRoot();
          if (root != null) {
            final IProject[] projects = root.getProjects(IContainer.INCLUDE_HIDDEN);
            if (ArrayUtils.isNotEmpty(projects)) {
              monitor.beginTask("Configuring Sonar builder for Sonar projects...  ", projects.length);
              for (final IProject iProject : projects) {
                if (SonarNature.hasSonarNature(iProject)) {
                  monitor.subTask(iProject.getName());
                  try {
                    final IProjectDescription description = iProject.getDescription();
                    SonarNature.addSonarBuilder(description);
                    SonarNature.setDescription(iProject, description);

                  } catch (final CoreException exception) {
                    getDefault().debug(exception.getMessage());
                  }
                }
              }
            }
          }
        }
        return Status.OK_STATUS;
      }
    };
    job.setPriority(Job.LONG);
    job.schedule();
  }

  private static SonarProjectManager projectManager;

  public synchronized SonarProjectManager getProjectManager() {
    if (projectManager == null) {
      projectManager = new SonarProjectManager();
    }
    return projectManager;
  }

  public static ISonarServersManager getServersManager() {
    return getDefault().serversManager;
  }

  public static ISonarResource createSonarResource(final IResource resource, final String key, final String name) {
    return new SonarResource(resource, key, name);
  }

  public static ISonarFile createSonarFile(final IFile file, final String key, final String name) {
    return new SonarFile(file, key, name);
  }

  /**
   * Create a new SonarQube project from the given project. Enable SonarQube nature.
   * @param project
   * @param url
   * @param key
   * @param analysedLocally
   * @return
   * @throws CoreException
   */
  public static SonarProject createSonarProject(final IProject project, final String url, final String key) throws CoreException {
    final SonarProject sonarProject = SonarProject.getInstance(project);
    sonarProject.setUrl(url);
    sonarProject.setKey(key);
    sonarProject.save();
    SonarNature.enableNature(project);
    return sonarProject;
  }

}
