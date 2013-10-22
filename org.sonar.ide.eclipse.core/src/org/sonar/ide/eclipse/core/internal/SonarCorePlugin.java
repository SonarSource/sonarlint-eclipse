/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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

import java.util.ArrayList;
import java.util.List;

public class SonarCorePlugin extends AbstractPlugin {
  public static final String PLUGIN_ID = "org.sonar.ide.eclipse.core";

  public static final String MARKER_ID = PLUGIN_ID + ".sonarProblem";
  public static final String NEW_ISSUE_MARKER_ID = PLUGIN_ID + ".sonarProblemNewIssue";

  /**
   * Minimal supported version of SonarQube server for local analysis.
   */
  public static final String LOCAL_MODE_MINIMAL_SONAR_VERSION = "3.6"; //$NON-NLS-1$
  /**
   * Minimal supported version of SonarQube server for incremental analysis.
   */
  public static final String INCREMENTAL_MODE_MINIMAL_SONAR_VERSION = "4.0"; //$NON-NLS-1$

  private static SonarCorePlugin plugin;

  public SonarCorePlugin() {
    plugin = this;
  }

  public static SonarCorePlugin getDefault() {
    return plugin;
  }

  private ServersManager serversManager;
  private final List<LogListener> logListeners = new ArrayList<LogListener>();

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

  @Override
  public void start(BundleContext context) {
    super.start(context);

    serversManager = new ServersManager();
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

  public static ISonarResource createSonarResource(IResource resource, String key, String name) {
    return new SonarResource(resource, key, name);
  }

  public static ISonarFile createSonarFile(IFile file, String key, String name) {
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
  public static SonarProject createSonarProject(IProject project, String url, String key) throws CoreException {
    SonarProject sonarProject = SonarProject.getInstance(project);
    sonarProject.setUrl(url);
    sonarProject.setKey(key);
    sonarProject.save();
    SonarNature.enableNature(project);
    return sonarProject;
  }

}
