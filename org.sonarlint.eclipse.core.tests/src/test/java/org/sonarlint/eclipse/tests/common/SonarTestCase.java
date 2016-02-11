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
package org.sonarlint.eclipse.tests.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.assertTrue;

/**
 * Common test case for sonar-ide/eclipse projects.
 */
public abstract class SonarTestCase {

  protected static final IProgressMonitor MONITOR = new NullProgressMonitor();
  protected static IWorkspace workspace;

  protected static File projectsSource;
  protected static File projectsWorkdir;
  private static final ReadWriteLock COPY_PROJECT_LOCK = new ReentrantReadWriteLock();

  protected static File getProject(String projectName) throws IOException {
    File destDir = new File(projectsWorkdir, projectName);
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
    COPY_PROJECT_LOCK.writeLock().lock();
    try {
      File projectFolder = new File(projectsSource, projectName);
      assertTrue("Project " + projectName + " folder not found.\n" + projectFolder.getAbsolutePath(), projectFolder.isDirectory());
      if (destDir.isDirectory()) {
        System.out.println("Directory for project already exists: " + destDir);
      }
      Files.copy(projectFolder.toPath(), destDir.toPath());
      EnumSet<FileVisitOption> opts = EnumSet.of(FileVisitOption.FOLLOW_LINKS);
      TreeCopier tc = new TreeCopier(projectFolder.toPath(), destDir.toPath(), true, true);
      Files.walkFileTree(projectFolder.toPath(), opts, Integer.MAX_VALUE, tc);
      return destDir;
    } finally {
      COPY_PROJECT_LOCK.writeLock().unlock();
    }
  }

  /**
   * A {@code FileVisitor} that copies a file-tree ("cp -r")
   */
  static class TreeCopier implements FileVisitor<Path> {
    private final Path source;
    private final Path target;
    private final boolean force;
    private final boolean preserve;

    TreeCopier(Path source, Path target, boolean force, boolean preserve) {
      this.source = source;
      this.target = target;
      this.force = force;
      this.preserve = preserve;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
      // before visiting entries in a directory we copy the directory
      // (okay if directory already exists).
      CopyOption[] options = (preserve) ? new CopyOption[] {COPY_ATTRIBUTES} : new CopyOption[0];

      Path newdir = target.resolve(source.relativize(dir));
      try {
        Files.copy(dir, newdir, options);
      } catch (FileAlreadyExistsException x) {
        // ignore
      } catch (IOException x) {
        System.err.format("Unable to create: %s: %s%n", newdir, x);
        return SKIP_SUBTREE;
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
      copyFile(file, target.resolve(source.relativize(file)), force, preserve);
      return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      // fix up modification time of directory when done
      if (exc == null && preserve) {
        Path newdir = target.resolve(source.relativize(dir));
        try {
          FileTime time = Files.getLastModifiedTime(dir);
          Files.setLastModifiedTime(newdir, time);
        } catch (IOException x) {
          System.err.format("Unable to copy all attributes to: %s: %s%n", newdir, x);
        }
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
      if (exc instanceof FileSystemLoopException) {
        System.err.println("cycle detected: " + file);
      } else {
        System.err.format("Unable to copy: %s: %s%n", file, exc);
      }
      return CONTINUE;
    }
  }

  /**
   * Copy source file to target location. If {@code prompt} is true then
   * prompt user to overwrite target if it exists. The {@code preserve}
   * parameter determines if file attributes should be copied/preserved.
   */
  static void copyFile(Path source, Path target, boolean force, boolean preserve) {
    CopyOption[] options = (preserve) ? new CopyOption[] {COPY_ATTRIBUTES, REPLACE_EXISTING} : new CopyOption[] {REPLACE_EXISTING};
    if (force || Files.notExists(target)) {
      try {
        Files.copy(source, target, options);
      } catch (IOException x) {
        System.err.format("Unable to copy: %s: %s%n", source, x);
      }
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

    cleanWorkspace();
  }

  @AfterClass
  final static public void end() throws Exception {
    final IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(true);
    workspace.setDescription(description);
  }

  private static void cleanWorkspace() throws Exception {
    final IWorkspaceRoot root = workspace.getRoot();
    for (final IProject project : root.getProjects()) {
      project.delete(true, true, MONITOR);
    }
  }

  /**
   * Import test project into the Eclipse workspace
   *
   * @return created projects
   */
  public static IProject importEclipseProject(final String projectdir) throws IOException, CoreException {
    final IWorkspaceRoot root = workspace.getRoot();

    final String projectName = projectdir;
    File dst = new File(root.getLocation().toFile(), projectName);
    getProject(projectName, dst);

    final IProject project = workspace.getRoot().getProject(projectName);
    final List<IProject> addedProjectList = new ArrayList<IProject>();

    workspace.run(new IWorkspaceRunnable() {

      @Override
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
    }, workspace.getRoot(), IWorkspace.AVOID_UPDATE, MONITOR);
    JobHelpers.waitForJobsToComplete();
    return addedProjectList.get(0);
  }

}
