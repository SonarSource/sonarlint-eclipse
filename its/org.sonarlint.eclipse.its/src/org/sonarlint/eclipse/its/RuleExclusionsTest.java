/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2018 SonarSource SA
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
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.junit.Test;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.bots.OnTheFlyViewBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class RuleExclusionsTest extends AbstractSonarLintTest {
  private static final String PREF_RULE_EXCLUSIONS = "ruleExclusions";

  @Test
  public void deactivate_rule() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-exclude-rules", "java-exclude-rules");
    JobHelpers.waitForJobsToComplete(bot);

    SWTBotView onTheFly = new OnTheFlyViewBot(bot).show();

    JavaPackageExplorerBot javaBot = new JavaPackageExplorerBot(bot);
    javaBot.expandAndDoubleClick("java-exclude-rules", "src", "hello", "Hello3.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello3.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-exclude-rules/src/hello/Hello3.java", 9, "Replace this use of System.out or System.err by a logger."),
      tuple("/java-exclude-rules/src/hello/Hello3.java", 12, "Refactor this method to reduce its Cognitive Complexity from 24 to the 15 allowed."));

    new JavaPackageExplorerBot(bot).expandAndSelect("java-exclude-rules", "src", "hello", "Hello3.java");

    onTheFly.bot().tree().select(1).contextMenu("Deactivate rule").click();

    JobHelpers.waitForJobsToComplete(bot);
    markers = Arrays.asList(project.findMember("src/hello/Hello3.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-exclude-rules/src/hello/Hello3.java", 9, "Replace this use of System.out or System.err by a logger."));

    if (isMarsOrGreater()) {
      reactivateRuleUsingUI();
    } else {
      // The preference menu seems very flaky in Luna
      clearRulesProgrammatically();
    }

    JobHelpers.waitForJobsToComplete(bot);
    markers = Arrays.asList(project.findMember("src/hello/Hello3.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-exclude-rules/src/hello/Hello3.java", 9, "Replace this use of System.out or System.err by a logger."),
      tuple("/java-exclude-rules/src/hello/Hello3.java", 12, "Refactor this method to reduce its Cognitive Complexity from 24 to the 15 allowed."));
  }

  void reactivateRuleUsingUI() {
    bot.menu("Window").menu("Preferences").click();
    bot.shell("Preferences").activate();
    bot.tree().getTreeItem("SonarLint").select().expand().click()
      .getNode("Rules Configuration").select().click();

    assertThat(bot.table().cell(0, 0)).isEqualTo("squid:S3776");
    assertThat(bot.table().cell(0, 1)).isEqualTo("Cognitive Complexity of methods should not be too high");
    bot.table().click(0, 0);

    bot.button("Remove").click();
    bot.button("Apply").click();
    if (isOxygenOrGreater()) {
      bot.button("Apply and Close").click();
    } else {
      bot.button("OK").click();
    }
  }

  private void clearRulesProgrammatically() {
    ConfigurationScope.INSTANCE.getNode(UI_PLUGIN_ID).put(PREF_RULE_EXCLUSIONS, "");

    // Make a change and save file to trigger analysis
    SWTBotEclipseEditor editor = bot.editorByTitle("Hello3.java").toTextEditor();
    editor.navigateTo(8, 29);
    editor.insertText("2");
    editor.save();
  }
}
