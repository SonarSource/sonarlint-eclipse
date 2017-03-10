/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarlint.eclipse.its;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.ui.JavaUI;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class StandaloneAnalysisTest extends AbstractSonarLintTest {

  @Test
  public void shouldAnalyseJava() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-simple", "java-simple");
    JobHelpers.waitForJobsToComplete(bot);

    // SonarJava requires bytecode, so project should be compiled
    project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-simple", "src", "hello", "Hello.java");

    List<IMarker> markers = Arrays.asList(project.findMarkers(MARKER_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers).hasSize(1);
    assertThat(markers.get(0).getAttribute(IMarker.LINE_NUMBER, -1)).isEqualTo(9);
    assertThat(markers.get(0).getAttribute(IMarker.MESSAGE, "")).isEqualTo("Replace this usage of System.out or System.err by a logger.");
  }

  // SONARIDE-348, SONARIDE-370
  @Test
  public void shouldPassAdditionalArguments() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    // SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));
    // IProject project = importEclipseProject("java/java-simple", "java-simple-additional-args");
    // JobHelpers.waitForJobsToComplete(bot);
    //
    // // Configure extra args on project
    // sonarProject.getExtraProperties().add(new SonarProperty("sonar.exclusions", "**/Pmd.java"));
    // // Force default profile to ensure analysis is correctly done
    // sonarProject.getExtraProperties().add(new SonarProperty("sonar.profile", "it-profile"));
    // sonarProject.save();
    //
    // // Configure JVM arguments (SONARIDE-370)
    // new SonarPreferencesBot(bot).setJvmArguments("-Xms512m -Xmx1024m").ok();
    //
    // // FindBugs requires bytecode, so project should be compiled
    // project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    //
    // new JavaPackageExplorerBot(bot)
    // .expandAndSelect("java-simple-additional-args")
    // .clickContextMenu("SonarQube", "Analyze");
    //
    // JobHelpers.waitForJobsToComplete(bot);
    //
    // List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    // assertThat(markers.size(), is(3));
    //
    // assertThat(markers, hasItem(new IsMarker("src/Findbugs.java", 5)));
    // assertThat(markers, hasItem(new IsMarker("src/Checkstyle.java", 1)));
    //
    // // SONARIDE-209 Divide source code for tests and main source code
    // assertThat(markers, hasItem(new IsMarker("test/PmdTest.java", 14)));
    //
    // // Configure wrong JVM arguments to test if it is taken into account (SONARIDE-370)
    // new SonarPreferencesBot(bot).setJvmArguments("-Xmx10m").ok();
    //
    // new JavaPackageExplorerBot(bot)
    // .expandAndSelect("java-simple-additional-args")
    // .clickContextMenu("SonarQube", "Analyze");
    //
    // JobHelpers.waitForJobsToComplete(bot);
    //
    // // No marker as local analysis should have failed
    // markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    // assertThat(markers.size(), is(0));
    //
    // // Restore JVM arguments
    // new SonarPreferencesBot(bot).setJvmArguments("").ok();
  }

  // SONARIDE-359
  @Test
  public void shouldAnalyseJavaWithOptionalSrcFolders() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    // SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));
    // IProject project = importEclipseProject("java/java-simple-optional-src", "java-simple-optional-src");
    // JobHelpers.waitForJobsToComplete(bot);
    //
    // // Enable Sonar Nature
    // SonarProject sonarProject = SonarCorePlugin.createSonarProject(project, getSonarServerUrl(), "bar:foo");
    // // Force default profile to ensure analysis is correctly done
    // sonarProject.getExtraProperties().add(new SonarProperty("sonar.profile", "it-profile"));
    // sonarProject.save();
    //
    // // FindBugs requires bytecode, so project should be compiled
    // project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    //
    // new JavaPackageExplorerBot(bot)
    // .expandAndSelect("java-simple-optional-src")
    // .clickContextMenu("SonarQube", "Analyze");
    //
    // JobHelpers.waitForJobsToComplete(bot);
    //
    // List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    // assertThat(markers.size(), is(3));
  }

  // SONARIDE-349
  @Test
  public void shouldAnalyseJavaWithSeveralOutputFolders() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    // SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));
    // IProject project = importEclipseProject("java/java-several-output-folders", "java-several-output-folders");
    // JobHelpers.waitForJobsToComplete(bot);
    //
    // // Enable Sonar Nature
    // SonarProject sonarProject = SonarCorePlugin.createSonarProject(project, getSonarServerUrl(), "bar:foo");
    // // Force default profile to ensure analysis is correctly done
    // sonarProject.getExtraProperties().add(new SonarProperty("sonar.profile", "it-profile"));
    // sonarProject.save();
    //
    // // FindBugs requires bytecode, so project should be compiled
    // project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    //
    // new JavaPackageExplorerBot(bot)
    // .expandAndSelect("java-several-output-folders")
    // .clickContextMenu("SonarQube", "Analyze");
    //
    // JobHelpers.waitForJobsToComplete(bot);
    //
    // List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    // assertThat(markers.size(), is(2));
  }

  // SONARIDE-349
  // SONARIDE-350
  // SONARIDE-353
  @Test
  public void shouldAnalyseJavaWithDependentProject() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    // SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));
    // IProject depProject = importEclipseProject("java/java-dependent-projects/java-dependent-project", "java-dependent-project");
    // IProject mainProject = importEclipseProject("java/java-dependent-projects/java-main-project", "java-main-project");
    // JobHelpers.waitForJobsToComplete(bot);
    //
    // // Enable Sonar Nature
    // SonarProject sonarProject = SonarCorePlugin.createSonarProject(mainProject, getSonarServerUrl(), "bar:foo");
    // // Force default profile to ensure analysis is correctly done
    // sonarProject.getExtraProperties().add(new SonarProperty("sonar.profile", "it-profile"));
    // sonarProject.save();
    //
    // // FindBugs requires bytecode, so projects should be compiled
    // depProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    // mainProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
    //
    // final IWorkspaceRoot root = workspace.getRoot();
    // File toBeDeleted = new File(root.getLocation().toFile(), "java-main-project/libs/toBeDeleted.jar");
    // assertTrue("Unable to delete JAR to test SONARIDE-350", toBeDeleted.delete());
    //
    // new JavaPackageExplorerBot(bot)
    // .expandAndSelect("java-main-project")
    // .clickContextMenu("SonarQube", "Analyze");
    //
    // JobHelpers.waitForJobsToComplete(bot);
    //
    // List<IMarker> markers = Arrays.asList(mainProject.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    // assertThat(markers.size(), is(2));
  }

  @Test
  public void shouldAnalysePython() throws Exception {
    // SwtBotUtils.openPerspective(bot, PythonPerspectiveFactory.PERSPECTIVE_ID);
    // SonarCorePlugin.getServersManager().addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));
    // IProject project = importEclipseProject("python", "python");
    //
    // bot.shell("Python not configured").activate();
    // bot.sleep(1000);
    // bot.button("Don't ask again").click();
    //
    // JobHelpers.waitForJobsToComplete(bot);
    //
    // // Enable Sonar Nature
    // SonarProject sonarProject = SonarCorePlugin.createSonarProject(project, getSonarServerUrl(), "python");
    // // Force default profile to ensure analysis is correctly done
    // sonarProject.getExtraProperties().add(new SonarProperty("sonar.profile", "it-profile"));
    // sonarProject.save();
    //
    // new PydevPackageExplorerBot(bot)
    // .expandAndSelect("python")
    // .clickContextMenu("SonarQube", "Analyze");
    //
    // JobHelpers.waitForJobsToComplete(bot);
    //
    // List<IMarker> markers = Arrays.asList(project.findMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    // assertThat(markers.size(), is(2));
    //
    // assertThat(markers, hasItem(new IsMarker("src/root/nested/exemple.py", 9)));
    // assertThat(markers, hasItem(new IsMarker("src/root/nested/exemple.py", 10)));
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
      return Objects.equals(actualPath, path) && (actualLine == line);
    }

    @Override
    public void describeTo(Description description) {
    }

  }

}
