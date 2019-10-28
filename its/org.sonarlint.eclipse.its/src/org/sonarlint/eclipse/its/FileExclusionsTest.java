/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.concurrent.TimeUnit;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.junit.Test;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assume.assumeTrue;

public class FileExclusionsTest extends AbstractSonarLintTest {

  @Test
  public void should_exclude_file() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-simple", "java-simple");
    JobHelpers.waitForJobsToComplete(bot);

    JavaPackageExplorerBot javaBot = new JavaPackageExplorerBot(bot);
    javaBot
      .expandAndDoubleClick("java-simple", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);

    assertThat(getMarkers(project, "src/hello/Hello.java")).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-simple/src/hello/Hello.java", 9, "Replace this use of System.out or System.err by a logger."));

    bot.editorByTitle("Hello.java").close();

    // Exclude file
    javaBot.excludeFile("java-simple", "src", "hello", "Hello.java");
    // Give time for markers to be deleted
    TimeUnit.SECONDS.sleep(5);
    assertThat(getMarkers(project, "src/hello/Hello.java")).isEmpty();

    // Seems that isEnabled of swtbot doesn't work on older Eclipse
    if (isNeonOrGreater()) {
      assertThat(javaBot.isExcludeEnabled("java-simple", "src", "hello", "Hello.java")).isFalse();
      assertThat(javaBot.isManualAnalysisEnabled("java-simple", "src", "hello", "Hello.java")).isFalse();
    }

    javaBot.expandAndDoubleClick("java-simple", "src", "hello", "Hello.java");
    JobHelpers.waitForJobsToComplete(bot);
    assertThat(getMarkers(project, "src/hello/Hello.java")).isEmpty();

    SWTBotEclipseEditor editor = bot.editorByTitle("Hello.java").toTextEditor();
    editor.navigateTo(8, 29);
    editor.insertText("2");
    editor.save();
    JobHelpers.waitForJobsToComplete(bot);

    assertThat(getMarkers(project, "src/hello/Hello.java")).isEmpty();

    // Trigger manual analysis of the project
    javaBot.triggerManualAnalysis("java-simple");
    bot.shell("Confirmation").activate();
    bot.button("OK").click();
    JobHelpers.waitForJobsToComplete(bot);

    assertThat(getMarkers(project, "src/hello/Hello.java")).isEmpty();
  }

  private IMarker[] getMarkers(IProject project, String path) throws CoreException {
    return project.findMember(path).findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE);
  }

  @Test
  public void should_add_new_entry() {
    // The preference menu seems very flaky in Luna
    assumeTrue(isMarsOrGreater());

    bot.menu("Window").menu("Preferences").click();
    bot.shell("Preferences").activate();
    bot.tree().getTreeItem("SonarLint").select().expand().click()
      .getNode("File Exclusions").select().click();

    SWTBotButton newButton = bot.button("New...");

    if (isNeonOrGreater()) {
      // mars is not able to find the dialog "Create Exclusion" :(

      newButton.click();
      bot.shell("Create Exclusion").activate();
      String value = "foo";
      bot.text().setText(value);
      bot.button("OK").click();
      bot.shell("Preferences").activate();
      assertThat(bot.table().cell(0, 1)).isEqualTo(value);

      bot.table().click(0, 1);
      bot.button("Remove").click();
    }

    bot.button("Cancel").click();
  }

}
