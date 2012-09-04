/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.tests.common;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.wsclient.Host;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.Assert.assertTrue;

/**
 * Common test case for sonar-ide/eclipse projects.
 */
public abstract class SonarTestCase {

  private static final Logger LOG = LoggerFactory.getLogger(SonarTestCase.class);

  protected static final IProgressMonitor monitor = new NullProgressMonitor();
  protected static IWorkspace workspace;
  protected static SonarUiPlugin plugin;

  protected static File projectsSource;
  protected static File projectsWorkdir;
  private static final ReadWriteLock copyProjectLock = new ReentrantReadWriteLock();

  protected static File getProject(String projectName) throws IOException {
    File destDir = new File(projectsWorkdir, projectName); // TODO include testName
    return getProject(projectName, destDir);
  }

  /**
   * Installs specified project to specified directory.
   * 
   * @param projectName
   *          name of project
   * @param destDir
   *          destination directory
   * @return project directory
   * @throws IOException
   *           if unable to prepare project directory
   */
  protected static File getProject(String projectName, File destDir) throws IOException {
    copyProjectLock.writeLock().lock();
    try {
      File projectFolder = new File(projectsSource, projectName);
      assertTrue("Project " + projectName + " folder not found.\n" + projectFolder.getAbsolutePath(), projectFolder.isDirectory());
      if (destDir.isDirectory()) {
        LoggerFactory.getLogger(SonarTestCase.class).warn("Directory for project already exists: {}", destDir);
      }

      // TODO interpolate files
      // FileUtils.copyDirectory(projectFolder, destDir, HiddenFileFilter.VISIBLE);
      FileUtils.copyDirectory(projectFolder, destDir);
      return destDir;
    } finally {
      copyProjectLock.writeLock().unlock();
    }
  }

  @BeforeClass
  final static public void prepareWorkspace() throws Exception {
    // Override default location "target/projects-source"
    projectsSource = new File("testdata");
    projectsWorkdir = new File("target/projects-target");

    workspace = ResourcesPlugin.getWorkspace();
    final IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(false);
    workspace.setDescription(description);

    plugin = SonarUiPlugin.getDefault();
    cleanWorkspace();
  }

  @AfterClass
  final static public void end() throws Exception {
    // cleanWorkspace();

    final IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(true);
    workspace.setDescription(description);
  }

  private static void cleanWorkspace() throws Exception {
    // Job.getJobManager().suspend();
    // waitForJobs();

    final List<Host> hosts = new ArrayList<Host>();
    hosts.addAll(SonarCorePlugin.getServersManager().getHosts());
    for (final Host host : hosts) {
      SonarCorePlugin.getServersManager().removeServer(host.getHost());
    }
    final IWorkspaceRoot root = workspace.getRoot();
    for (final IProject project : root.getProjects()) {
      project.delete(true, true, monitor);
    }
  }

  /**
   * Import test project into the Eclipse workspace
   * 
   * @return created projects
   */
  public static IProject importEclipseProject(final String projectdir) throws IOException, CoreException {
    LOG.info("Importing Eclipse project : " + projectdir);
    final IWorkspaceRoot root = workspace.getRoot();

    final String projectName = projectdir;
    File dst = new File(root.getLocation().toFile(), projectName);
    getProject(projectName, dst);

    final IProject project = workspace.getRoot().getProject(projectName);
    final List<IProject> addedProjectList = new ArrayList<IProject>();

    workspace.run(new IWorkspaceRunnable() {

      public void run(final IProgressMonitor monitor) throws CoreException {
        // create project as java project
        if (!project.exists()) {
          final IProjectDescription projectDescription = workspace.newProjectDescription(project.getName());
          projectDescription.setLocation(null);
          project.create(projectDescription, monitor);
          project.open(IResource.NONE, monitor);
        } else {
          project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
        addedProjectList.add(project);
      }
    }, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);
    JobHelpers.waitForJobsToComplete();
    LOG.info("Eclipse project imported");
    return addedProjectList.get(0);
  }

}
