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
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Version;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class FileExclusionsTest extends AbstractSonarLintTest {
  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldExcludeFile() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-simple", "java-simple");
    JobHelpers.waitForJobsToComplete(bot);

    JavaPackageExplorerBot javaBot = new JavaPackageExplorerBot(bot);
    javaBot
      .expandAndDoubleClick("java-simple", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-simple/src/hello/Hello.java", 9, "Replace this use of System.out or System.err by a logger."));

    bot.editorByTitle("Hello.java").close();

    // Exclude file
    javaBot.excludeFile("java-simple", "src", "hello", "Hello.java");

    // Seems that isEnabled of swtbot doesn't work on older Eclipse
    if (platformVersion().compareTo(new Version("4.6")) >= 0) {
      assertThat(javaBot.isExcludeEnabled("java-simple", "src", "hello", "Hello.java")).isFalse();
      assertThat(javaBot.isManualAnalysisEnabled("java-simple", "src", "hello", "Hello.java")).isFalse();
    }

    javaBot.expandAndDoubleClick("java-simple", "src", "hello", "Hello.java");
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

    // Trigger manual analysis of the project
    javaBot.triggerManualAnalysis("java-simple");
    JobHelpers.waitForJobsToComplete(bot);

    markers = Arrays.asList(project.findMember("src/hello/Hello.java").findMarkers(MARKER_REPORT_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).isEmpty();
  }

}
