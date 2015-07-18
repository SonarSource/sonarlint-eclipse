/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.build.SonarRunner;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
import org.junit.ClassRule;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.servers.SonarServersManager;
import org.sonar.ide.eclipse.ui.its.utils.FakeSonarConsole;
import org.sonar.wsclient.SonarClient;
import org.sonar.wsclient.issue.IssueQuery;
import org.sonar.wsclient.issue.Issues;

import static org.junit.Assert.assertTrue;

/**
 * Common test case for sonar-ide/eclipse projects.
 */
public abstract class AbstractSQEclipseTest {

  @ClassRule
  public static Orchestrator orchestrator = SQEclipseTestSuite.ORCHESTRATOR;

  private static boolean initialized = false;

  protected static final IProgressMonitor monitor = new NullProgressMonitor();
  protected static IWorkspace workspace;

  protected static File projectsWorkdir;
  private static final ReadWriteLock copyProjectLock = new ReentrantReadWriteLock();
  private static FakeSonarConsole listener;

  protected static File getProject(String projectName) throws IOException {
    File destDir = new File(projectsWorkdir, projectName);
    return getProject(projectName, destDir);
  }

  @BeforeClass
  public static void prepare() {
    if (!initialized) {
      // Analyse projects
      orchestrator.getServer().provisionProject("org.sonar-ide.tests:reference", "reference");
      orchestrator.getServer().associateProjectToQualityProfile("org.sonar-ide.tests:reference", "java", "it-profile");
      orchestrator.executeBuild(MavenBuild.create(new File("projects/reference/pom.xml")).setCleanPackageSonarGoals());

      // Not possible to provision a project with a branch before 5.0
      orchestrator.executeBuild(MavenBuild.create(new File("projects/branch/pom.xml")).setCleanPackageSonarGoals().setProfile("it-profile"));
      orchestrator.getServer().associateProjectToQualityProfile("org.sonar-ide.tests:reference:BRANCH-0.9", "java", "it-profile");

      orchestrator.getServer().provisionProject("org.sonar-ide.tests:multimodule", "Multimodule");
      orchestrator.getServer().associateProjectToQualityProfile("org.sonar-ide.tests:multimodule", "java", "it-profile");
      orchestrator.executeBuild(MavenBuild.create(new File("projects/multimodule/pom.xml")).setCleanPackageSonarGoals());

      // Used by ConfigureProjectTest
      orchestrator.getServer().provisionProject("p2", "P2");
      orchestrator.executeBuild(SonarRunner.create(new File("projects/shortname/")));

      // create a false positive review on a file
      SonarClient wsClient = SonarClient.builder()
        .url(orchestrator.getServer().getUrl())
        .login("admin")
        .password("admin")
        .build();

      Issues issues = wsClient.issueClient().find(IssueQuery.create().components("org.sonar-ide.tests:reference:src/main/java/com/foo/ClassWithFalsePositive.java"));
      wsClient.issueClient().doTransition(issues.list().get(0).key(), "falsepositive");
      initialized = true;
    }
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
  protected static File getProject(String projectdir, File destDir) throws IOException {
    copyProjectLock.writeLock().lock();
    try {
      File projectFolder = new File("projects", projectdir);
      assertTrue("Project " + projectdir + " folder not found.\n" + projectFolder.getAbsolutePath(), projectFolder.isDirectory());
      FileUtils.copyDirectory(projectFolder, destDir);
      return destDir;
    } finally {
      copyProjectLock.writeLock().unlock();
    }
  }

  @BeforeClass
  final static public void prepareWorkspace() throws Exception {
    projectsWorkdir = new File("target/projects-target");

    workspace = ResourcesPlugin.getWorkspace();
    final IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(false);
    workspace.setDescription(description);

    cleanWorkspace();

    listener = new FakeSonarConsole();
    SonarCorePlugin.getDefault().addLogListener(listener);
  }

  @AfterClass
  final static public void end() throws Exception {
    // cleanWorkspace();
    SonarCorePlugin.getDefault().removeLogListener(listener);
    final IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(true);
    workspace.setDescription(description);
  }

  private static void cleanWorkspace() throws Exception {
    ((SonarServersManager) (SonarCorePlugin.getServersManager())).clean();
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
  public static IProject importEclipseProject(final String projectdir, final String projectName) throws IOException, CoreException {
    final IWorkspaceRoot root = workspace.getRoot();

    File dst = new File(root.getLocation().toFile(), projectName);
    getProject(projectdir, dst);

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
    }, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);

    return addedProjectList.get(0);
  }

  public static void configureProject(String name) throws Exception {
    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
    SonarCorePlugin.createSonarProject(project, getSonarServerUrl(), "org.sonar-ide.tests:reference");
  }

  public static String getSonarServerUrl() {
    return orchestrator.getServer().getUrl();
  }

}
