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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.junit.Test;
import org.python.pydev.ui.perspective.PythonPerspectiveFactory;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.bots.PydevPackageExplorerBot;
import org.sonarlint.eclipse.its.bots.SonarLintProjectPropertiesBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assume.assumeTrue;

public class StandaloneAnalysisTest extends AbstractSonarLintTest {

  @Test
  public void shouldAnalyseJava() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-simple", "java-simple");
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-simple", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple(9, "Replace this usage of System.out or System.err by a logger."));

    bot.editorByTitle("Hello.java").close();

    // Disable auto analysis on open
    new JavaPackageExplorerBot(bot)
      .openSonarLintProperties("java-simple");
    new SonarLintProjectPropertiesBot(bot, "java-simple")
      .clickAutoAnalysis()
      .ok();
    project.deleteMarkers(MARKER_ID, true, IResource.DEPTH_INFINITE);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-simple", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);
    markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).isEmpty();

    // Disable auto-analysis on save
    SWTBotEclipseEditor editor = bot.editorByTitle("Hello.java").toTextEditor();
    editor.navigateTo(8, 29);
    editor.insertText("2");
    editor.save();
    JobHelpers.waitForJobsToComplete(bot);

    markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).isEmpty();

    // Trigger manual analysis
    new JavaPackageExplorerBot(bot)
      .triggerManualAnalysis("java-simple", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple(9, "Replace this usage of System.out or System.err by a logger."));
  }

  @Test
  public void shouldAnalyseJavaJunit() throws Exception {
    assumeTrue(supportJunit());
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-junit", "java-junit");
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-junit", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple(11, "Replace this usage of System.out or System.err by a logger."),
      tuple(15, "Remove this unnecessary cast to \"int\".")); // Test that sonar.java.libraries is set

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-junit", "src", "hello", "HelloTest.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> testMarkers = Arrays.asList(project.findMember("src/hello/HelloTest.java").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(testMarkers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple(10, "Fix or remove this skipped unit test"));
  }

  @Test
  public void shouldAnalyseJava8() throws Exception {
    assumeTrue(supportJava8());
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java8", "java8");
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java8", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple(13, "Make this anonymous inner class a lambda")); // Test that sonar.java.source is set
  }

  // SONARIDE-349
  // SONARIDE-350
  // SONARIDE-353
  @Test
  public void shouldAnalyseJavaWithDependentProject() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    importEclipseProject("java/java-dependent-projects/java-dependent-project", "java-dependent-project");
    IProject mainProject = importEclipseProject("java/java-dependent-projects/java-main-project", "java-main-project");
    JobHelpers.waitForJobsToComplete(bot);

    final IWorkspaceRoot root = workspace.getRoot();
    File toBeDeleted = new File(root.getLocation().toFile(), "java-main-project/libs/toBeDeleted.jar");
    assertThat(toBeDeleted.delete()).as("Unable to delete JAR to test SONARIDE-350").isTrue();

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-main-project", "src", "use", "UseUtils.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(mainProject.findMember("src/use/UseUtils.java").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple(9, "Remove this unnecessary cast to \"int\".")); // Test that sonar.java.libraries is set on dependent project
  }

  @Test
  public void shouldAnalysePython() throws Exception {
    SwtBotUtils.openPerspective(bot, PythonPerspectiveFactory.PERSPECTIVE_ID);
    IProject project = importEclipseProject("python", "python");

    bot.shell("Python not configured").activate();
    bot.button("Don't ask again").click();

    JobHelpers.waitForJobsToComplete(bot);

    new PydevPackageExplorerBot(bot)
      .expandAndOpen("python", "src", "root", "nested", "exemple.py");

    bot.shell("Default Eclipse preferences for PyDev").activate();
    bot.button("OK").click();

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/root/nested/exemple.py").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple(9, "Merge this if statement with the enclosing one."),
      tuple(10, "Replace print statement by built-in function."),
      tuple(9, "Replace \"<>\" by \"!=\"."),
      tuple(1, "Remove this commented out code."));
  }

}
