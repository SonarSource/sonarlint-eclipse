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
import org.junit.Test;
import org.python.pydev.ui.perspective.PythonPerspectiveFactory;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.bots.PydevPackageExplorerBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class StandaloneAnalysisTest extends AbstractSonarLintTest {

  @Test
  public void shouldAnalyseJava() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-simple", "java-simple");
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-simple", "src", "hello", "Hello.java");

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple(13, "Replace this usage of System.out or System.err by a logger."),
      tuple(15, "Make this anonymous inner class a lambda"), // Test that sonar.java.source is set
      tuple(16, "Add the \"@Override\" annotation above this method signature"),
      tuple(24, "Remove this unnecessary cast to \"int\".")); // Test that sonar.java.libraries is set

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-simple", "src", "hello", "HelloTest.java");

    List<IMarker> testMarkers = Arrays.asList(project.findMember("src/hello/HelloTest.java").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(testMarkers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple(10, "Fix or remove this skipped unit test"));
  }

  // SONARIDE-349
  // SONARIDE-350
  // SONARIDE-353
  @Test
  public void shouldAnalyseJavaWithDependentProject() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject depProject = importEclipseProject("java/java-dependent-projects/java-dependent-project", "java-dependent-project");
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
      .expandAndDoubleClick("python", "src", "root", "nested", "exemple.py");

    // Starting from PyDev 5.5 there is a new modal window to close
    if (!bot.shells("Default Eclipse preferences for PyDev").isEmpty()) {
      bot.shell("Default Eclipse preferences for PyDev").activate();
      bot.button("OK").click();
    }

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/root/nested/exemple.py").findMarkers(MARKER_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple(9, "Merge this if statement with the enclosing one."),
      tuple(10, "Replace print statement by built-in function."),
      tuple(9, "Replace \"<>\" by \"!=\"."),
      tuple(1, "Remove this commented out code."));
  }

}
