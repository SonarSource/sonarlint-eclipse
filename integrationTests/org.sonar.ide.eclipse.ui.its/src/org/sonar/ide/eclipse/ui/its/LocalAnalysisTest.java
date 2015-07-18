/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.ui.JavaUI;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.python.pydev.ui.perspective.PythonPerspectiveFactory;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;
import org.sonar.ide.eclipse.ui.its.bots.ConfigureProjectsWizardBot;
import org.sonar.ide.eclipse.ui.its.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.its.bots.JavaPackageExplorerBot;
import org.sonar.ide.eclipse.ui.its.bots.PydevPackageExplorerBot;
import org.sonar.ide.eclipse.ui.its.bots.SonarPreferencesBot;
import org.sonar.ide.eclipse.ui.its.utils.JobHelpers;
import org.sonar.ide.eclipse.ui.its.utils.SwtBotUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItem;

@SuppressWarnings("restriction")
public class LocalAnalysisTest extends AbstractSQEclipseUITest {

  @Test
  public void shouldAnalyseJava() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));
    IProject project = importEclipseProject("java/java-simple", "java-simple");
    JobHelpers.waitForJobsToComplete(bot);

    // Enable Sonar Nature
    SonarProject sonarProject = SonarCorePlugin.createSonarProject(project, getSonarServerUrl(), "bar:foo");
    // Force default profile to ensure analysis is correctly done
    sonarProject.getExtraProperties().add(new SonarProperty("sonar.profile", "it-profile"));
    sonarProject.save();

    // FindBugs requires bytecode, so project should be compiled
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    new JavaPackageExplorerBot(bot)
      .expandAndSelect("java-simple")
      .clickContextMenu("SonarQube", "Analyze");

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(4));

    assertThat(markers, hasItem(new IsMarker("src/Findbugs.java", 5)));
    assertThat(markers, hasItem(new IsMarker("src/Pmd.java", 2)));
    assertThat(markers, hasItem(new IsMarker("src/Checkstyle.java", 1)));

    // SONARIDE-209 Divide source code for tests and main source code
    assertThat(markers, hasItem(new IsMarker("test/PmdTest.java", 14)));
  }

  // SONARIDE-348, SONARIDE-370
  @Test
  public void shouldPassAdditionalArguments() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));
    IProject project = importEclipseProject("java/java-simple", "java-simple-additional-args");
    JobHelpers.waitForJobsToComplete(bot);

    // Enable Sonar Nature
    SonarProject sonarProject = SonarCorePlugin.createSonarProject(project, getSonarServerUrl(), "bar:foo");

    // Configure extra args on project
    sonarProject.getExtraProperties().add(new SonarProperty("sonar.exclusions", "**/Pmd.java"));
    // Force default profile to ensure analysis is correctly done
    sonarProject.getExtraProperties().add(new SonarProperty("sonar.profile", "it-profile"));
    sonarProject.save();

    // Configure JVM arguments (SONARIDE-370)
    new SonarPreferencesBot(bot).setJvmArguments("-Xms512m -Xmx1024m").ok();

    // FindBugs requires bytecode, so project should be compiled
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    new JavaPackageExplorerBot(bot)
      .expandAndSelect("java-simple-additional-args")
      .clickContextMenu("SonarQube", "Analyze");

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(3));

    assertThat(markers, hasItem(new IsMarker("src/Findbugs.java", 5)));
    assertThat(markers, hasItem(new IsMarker("src/Checkstyle.java", 1)));

    // SONARIDE-209 Divide source code for tests and main source code
    assertThat(markers, hasItem(new IsMarker("test/PmdTest.java", 14)));

    // Configure wrong JVM arguments to test if it is taken into account (SONARIDE-370)
    new SonarPreferencesBot(bot).setJvmArguments("-Xmx10m").ok();

    new JavaPackageExplorerBot(bot)
      .expandAndSelect("java-simple-additional-args")
      .clickContextMenu("SonarQube", "Analyze");

    JobHelpers.waitForJobsToComplete(bot);

    // No marker as local analysis should have failed
    markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(0));

    // Restore JVM arguments
    new SonarPreferencesBot(bot).setJvmArguments("").ok();
  }

  // SONARIDE-359
  @Test
  public void shouldAnalyseJavaWithOptionalSrcFolders() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));
    IProject project = importEclipseProject("java/java-simple-optional-src", "java-simple-optional-src");
    JobHelpers.waitForJobsToComplete(bot);

    // Enable Sonar Nature
    SonarProject sonarProject = SonarCorePlugin.createSonarProject(project, getSonarServerUrl(), "bar:foo");
    // Force default profile to ensure analysis is correctly done
    sonarProject.getExtraProperties().add(new SonarProperty("sonar.profile", "it-profile"));
    sonarProject.save();

    // FindBugs requires bytecode, so project should be compiled
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    new JavaPackageExplorerBot(bot)
      .expandAndSelect("java-simple-optional-src")
      .clickContextMenu("SonarQube", "Analyze");

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(3));
  }

  // SONARIDE-349
  @Test
  public void shouldAnalyseJavaWithSeveralOutputFolders() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));
    IProject project = importEclipseProject("java/java-several-output-folders", "java-several-output-folders");
    JobHelpers.waitForJobsToComplete(bot);

    // Enable Sonar Nature
    SonarProject sonarProject = SonarCorePlugin.createSonarProject(project, getSonarServerUrl(), "bar:foo");
    // Force default profile to ensure analysis is correctly done
    sonarProject.getExtraProperties().add(new SonarProperty("sonar.profile", "it-profile"));
    sonarProject.save();

    // FindBugs requires bytecode, so project should be compiled
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    new JavaPackageExplorerBot(bot)
      .expandAndSelect("java-several-output-folders")
      .clickContextMenu("SonarQube", "Analyze");

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(2));
  }

  // SONARIDE-349
  // SONARIDE-350
  // SONARIDE-353
  @Test
  public void shouldAnalyseJavaWithDependentProject() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));
    IProject depProject = importEclipseProject("java/java-dependent-projects/java-dependent-project", "java-dependent-project");
    IProject mainProject = importEclipseProject("java/java-dependent-projects/java-main-project", "java-main-project");
    JobHelpers.waitForJobsToComplete(bot);

    // Enable Sonar Nature
    SonarProject sonarProject = SonarCorePlugin.createSonarProject(mainProject, getSonarServerUrl(), "bar:foo");
    // Force default profile to ensure analysis is correctly done
    sonarProject.getExtraProperties().add(new SonarProperty("sonar.profile", "it-profile"));
    sonarProject.save();

    // FindBugs requires bytecode, so projects should be compiled
    depProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    mainProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    final IWorkspaceRoot root = workspace.getRoot();
    File toBeDeleted = new File(root.getLocation().toFile(), "java-main-project/libs/toBeDeleted.jar");
    assertTrue("Unable to delete JAR to test SONARIDE-350", toBeDeleted.delete());

    new JavaPackageExplorerBot(bot)
      .expandAndSelect("java-main-project")
      .clickContextMenu("SonarQube", "Analyze");

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(mainProject.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(2));
  }

  // SONARIDE-281
  @Test
  public void shouldNotShowFalsePositive() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));

    new ImportProjectBot(bot).setPath(getProjectPath("reference")).finish();
    JobHelpers.waitForJobsToComplete(bot);

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("reference");

    // FindBugs requires bytecode, so project should be compiled
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    new JavaPackageExplorerBot(bot)
      .expandAndSelect("reference")
      .clickContextMenu("Configure", "Associate with SonarQube...");

    ConfigureProjectsWizardBot projectWizardBot = new ConfigureProjectsWizardBot(bot);
    projectWizardBot.finish();

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(4));
  }

  // SONARIDE-365
  @Test
  public void shouldShowNewViolationsOnModules() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));

    new ImportProjectBot(bot).setPath(getProjectPath("multimodule/module1")).finish();
    JobHelpers.waitForJobsToComplete(bot);

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("multimodule-module1");

    // FindBugs requires bytecode, so project should be compiled
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    new JavaPackageExplorerBot(bot)
      .expandAndSelect("multimodule-module1")
      .clickContextMenu("Configure", "Associate with SonarQube...");

    ConfigureProjectsWizardBot projectWizardBot = new ConfigureProjectsWizardBot(bot);
    projectWizardBot.finish();

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(2));
    assertThat((Boolean) markers.get(0).getAttribute(MarkerUtils.SONAR_MARKER_IS_NEW_ATTR), is(false));

    IFile clazz = project.getFile("src/main/java/com/foo/ClassFromModule1.java");
    FileInputStream modification = null;
    try {
      modification = FileUtils.openInputStream(new File("projects/multimodule/ClassFromModule1.java"));
      clazz.setContents(modification, true, true, monitor);
    } finally {
      IOUtils.closeQuietly(modification);
    }

    // FindBugs requires bytecode, so project should be compiled
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    new JavaPackageExplorerBot(bot)
      .expandAndSelect("multimodule-module1")
      .clickContextMenu("SonarQube", "Analyze");

    JobHelpers.waitForJobsToComplete(bot);

    markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(3));

    markers = Arrays.asList(project.findMarkers(SonarCorePlugin.NEW_ISSUE_MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(1));
  }

  @Test
  public void shouldAnalysePython() throws Exception {
    SwtBotUtils.openPerspective(bot, PythonPerspectiveFactory.PERSPECTIVE_ID);
    SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));
    IProject project = importEclipseProject("python", "python");

    bot.shell("Python not configured").activate();
    bot.sleep(1000);
    bot.button("Don't ask again").click();

    JobHelpers.waitForJobsToComplete(bot);

    // Enable Sonar Nature
    SonarProject sonarProject = SonarCorePlugin.createSonarProject(project, getSonarServerUrl(), "python");
    // Force default profile to ensure analysis is correctly done
    sonarProject.getExtraProperties().add(new SonarProperty("sonar.profile", "it-profile"));
    sonarProject.save();

    new PydevPackageExplorerBot(bot)
      .expandAndSelect("python")
      .clickContextMenu("SonarQube", "Analyze");

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers.size(), is(2));

    assertThat(markers, hasItem(new IsMarker("src/root/nested/exemple.py", 9)));
    assertThat(markers, hasItem(new IsMarker("src/root/nested/exemple.py", 10)));
  }

  static class IsMarker extends BaseMatcher<IMarker> {

    private final String path;
    private final int line;

    public IsMarker(String path, int line) {
      this.path = path;
      this.line = line;
    }

    @Override
    public boolean matches(Object item) {
      IMarker marker = (IMarker) item;
      String actualPath = marker.getResource().getProjectRelativePath().toString();
      int actualLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
      return StringUtils.equals(actualPath, path) && (actualLine == line);
    }

    @Override
    public void describeTo(Description description) {
    }

  }

}
