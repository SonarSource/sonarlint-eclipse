/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2020 SonarSource SA
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.bots.ProjectExplorerBot;
import org.sonarlint.eclipse.its.bots.PydevPackageExplorerBot;
import org.sonarlint.eclipse.its.bots.SonarLintProjectPropertiesBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assume.assumeTrue;

public class StandaloneAnalysisTest extends AbstractSonarLintTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldAnalyseJava() throws Exception {
    System.out.println("shouldAnalyseJava");
    Assume.assumeFalse(platformVersion().toString().startsWith("4.4"));

    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-simple", "java-simple");
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-simple", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-simple/src/hello/Hello.java", 9, "Replace this use of System.out or System.err by a logger."));

    bot.editorByTitle("Hello.java").close();

    // Disable auto analysis on open
    new JavaPackageExplorerBot(bot)
      .openSonarLintProperties("java-simple");
    new SonarLintProjectPropertiesBot(bot, "java-simple")
      .clickAutoAnalysis()
      .ok();
    project.deleteMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_INFINITE);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-simple", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);
    markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).isEmpty();

    SWTBotEclipseEditor editor = bot.editorByTitle("Hello.java").toTextEditor();
    editor.navigateTo(8, 29);
    editor.insertText("2");
    editor.save();
    JobHelpers.waitForJobsToComplete(bot);

    markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).isEmpty();

    // Trigger manual analysis of a single file
    new JavaPackageExplorerBot(bot)
      .triggerManualAnalysis("java-simple", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_REPORT_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-simple/src/hello/Hello.java", 9, "Replace this use of System.out or System.err by a logger."));

    // Trigger manual analysis of all files
    new JavaPackageExplorerBot(bot)
      .triggerManualAnalysis("java-simple");

    bot.shell("Confirmation").bot().button("OK").click();

    JobHelpers.waitForJobsToComplete(bot);

    markers = Arrays.asList(project.findMarkers(MARKER_REPORT_ID, true, IResource.DEPTH_INFINITE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-simple/src/hello/Hello.java", 9, "Replace this use of System.out or System.err by a logger."),
      tuple("/java-simple/src/hello/Hello2.java", 9, "Replace this use of System.out or System.err by a logger."));
  }

  @Test
  public void shouldAnalyseJavaJunit() throws Exception {
    System.out.println("shouldAnalyseJavaJunit");
    assumeTrue(supportJunit());
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-junit", "java-junit");
    JobHelpers.waitForJobsToComplete(bot);

    bot.menu("Window").menu("Preferences").click();
    bot.shell("Preferences").activate();
    bot.tree().getTreeItem("SonarLint").select();
    bot.textWithLabel("Test file regular expressions:").setText("**/*TestUtil*");
    bot.button("Apply and Close").click();
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-junit", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-junit/src/hello/Hello.java", 12, "Replace this use of System.out or System.err by a logger."),
      tuple("/java-junit/src/hello/Hello.java", 16, "Remove this unnecessary cast to \"int\".")); // Test that sonar.java.libraries is set

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-junit", "src", "hello", "HelloTestUtil.java");
    JobHelpers.waitForJobsToComplete(bot);
  
    List<IMarker> testUtilMarkers = Arrays.asList(project.findMember("src/hello/HelloTestUtil.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    // File is flagged as test by regexp, only test rules are applied
    assertThat(testUtilMarkers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-junit/src/hello/HelloTestUtil.java", 11, "Remove this use of \"Thread.sleep()\"."));

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-junit", "tests", "hello", "HelloTest.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> testMarkers = Arrays.asList(project.findMember("tests/hello/HelloTest.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(testMarkers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-junit/tests/hello/HelloTest.java", 10, "Fix or remove this skipped unit test"),
      tuple("/java-junit/tests/hello/HelloTest.java", 10, "Add at least one assertion to this test case."));
  }

  @Test
  public void shouldAnalyseJava8() throws Exception {
    System.out.println("shouldAnalyseJava8");

    assumeTrue(supportJava8());
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java8", "java8");
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java8", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java8/src/hello/Hello.java", 13, "Make this anonymous inner class a lambda"),
      tuple("/java8/src/hello/Hello.java", 13, "Refactor the code so this stream pipeline is used.")); // Test that sonar.java.source is set
  }

  // SONARIDE-349
  // SONARIDE-350
  // SONARIDE-353
  @Test
  public void shouldAnalyseJavaWithDependentProject() throws Exception {
    System.out.println("shouldAnalyseJavaWithDependentProject");

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

    List<IMarker> markers = Arrays.asList(mainProject.findMember("src/use/UseUtils.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-main-project/src/use/UseUtils.java", 9, "Remove this unnecessary cast to \"int\".")); // Test that sonar.java.libraries
                                                                                                         // is
                                                                                                         // set on dependent project
  }

  // Need PyDev
  @Category(RequiresExtraDependency.class)
  @Test
  public void shouldAnalysePython() throws Exception {
    System.out.println("shouldAnalysePython");

    // PythonPerspectiveFactory.PERSPECTIVE_ID
    SwtBotUtils.openPerspective(bot, "org.python.pydev.ui.PythonPerspective");
    IProject project = importEclipseProject("python", "python");

    bot.shell("Python not configured").activate();
    bot.button("Don't ask again").click();

    JobHelpers.waitForJobsToComplete(bot);

    new PydevPackageExplorerBot(bot)
      .expandAndOpen("python", "src", "root", "nested", "example.py");

    bot.shell("Default Eclipse preferences for PyDev").activate();
    bot.button("OK").click();

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/root/nested/example.py").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/python/src/root/nested/example.py", 9, "Merge this if statement with the enclosing one."),
      tuple("/python/src/root/nested/example.py", 10, "Replace print statement by built-in function."),
      tuple("/python/src/root/nested/example.py", 9, "Replace \"<>\" by \"!=\"."));
  }

  // Need PDT
  @Category(RequiresExtraDependency.class)
  @Test
  public void shouldAnalysePHP() throws Exception {
    System.out.println("shouldAnalysePHP");

    SwtBotUtils.openPerspective(bot, "org.eclipse.php.perspective");
    IProject project = importEclipseProject("php", "php");

    JobHelpers.waitForJobsToComplete(bot);

    new ProjectExplorerBot(bot)
      .expandAndOpen("php", "foo.php");

    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("foo.php").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/php/foo.php", 9, "This branch duplicates the one on line 5."));

    // SLE-342
    new ProjectExplorerBot(bot)
      .expandAndOpen("php", "foo.inc");

    JobHelpers.waitForJobsToComplete(bot);

    markers = Arrays.asList(project.findMember("foo.inc").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/php/foo.inc", 9, "This branch duplicates the one on line 5."));
  }

  @Test
  public void shouldAnalyseLinkedFile() throws Exception {
    System.out.println("shouldAnalyseLinkedFile");

    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-linked", "java-linked");
    JobHelpers.waitForJobsToComplete(bot);

    File dotProject = new File(project.getLocation().toFile(), ".project");
    String content = FileUtils.readFileToString(dotProject, StandardCharsets.UTF_8);
    FileUtils.write(dotProject, content.replace("${PLACEHOLDER}", new File("projects/java/java-linked-target/hello/HelloLinked.java").getAbsolutePath()), StandardCharsets.UTF_8);
    project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("java-linked", "src", "hello", "HelloLinked.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/HelloLinked.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-linked/src/hello/HelloLinked.java", 13, "Replace this use of System.out or System.err by a logger."));
  }

  // Need RSE
  @Category(RequiresExtraDependency.class)
  @Test
  public void shouldAnalyseVirtualProject() throws Exception {
    System.out.println("shouldAnalyseVirtualProject");

    File remoteProjectDir = temp.newFolder();
    FileUtils.copyDirectory(new File("projects/java/java-simple"), remoteProjectDir);

    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    final IProject rseProject = workspace.getRoot().getProject("Local_java-simple");

    workspace.run(new IWorkspaceRunnable() {

      @Override
      public void run(final IProgressMonitor monitor) throws CoreException {
        final IProjectDescription projectDescription = workspace.newProjectDescription(rseProject.getName());
        URI uri = remoteProjectDir.toURI();
        try {
          projectDescription.setLocationURI(new URI("rse", "LOCALHOST", uri.getPath(), null));
        } catch (URISyntaxException e) {
          throw new IllegalStateException(e);
        }
        rseProject.create(projectDescription, monitor);
        rseProject.open(IResource.NONE, monitor);
      }
    }, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);
    JobHelpers.waitForJobsToComplete(bot);

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("Local_java-simple", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(rseProject.findMember("src/hello/Hello.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/Local_java-simple/src/hello/Hello.java", 9, "Replace this use of System.out or System.err by a logger."));
  }

}
