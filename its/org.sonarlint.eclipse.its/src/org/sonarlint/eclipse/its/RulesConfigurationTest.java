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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotLink;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.After;
import org.junit.Test;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.bots.OnTheFlyViewBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assume.assumeTrue;

public class RulesConfigurationTest extends AbstractSonarLintTest {

  @After
  public void cleanup() {
    try {
      bot.shell("Preferences").close();
    } catch (Exception e) {
      // Ignore
    }
  }

  @Test
  public void deactivate_rule() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-exclude-rules", "java-exclude-rules");
    JobHelpers.waitForJobsToComplete(bot);

    SWTBotView onTheFly = new OnTheFlyViewBot(bot).show();

    JavaPackageExplorerBot javaBot = new JavaPackageExplorerBot(bot);
    javaBot.expandAndDoubleClick("java-exclude-rules", "src", "hello", "Hello3.java");
    JobHelpers.waitForJobsToComplete(bot);

    checkIssueIsDefault(project);

    new JavaPackageExplorerBot(bot).expandAndSelect("java-exclude-rules", "src", "hello", "Hello3.java");

    onTheFly.bot().tree().select(1).contextMenu("Deactivate rule").click();

    JobHelpers.waitForJobsToComplete(bot);
    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello3.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-exclude-rules/src/hello/Hello3.java", 9, "Replace this use of System.out or System.err by a logger."));

    reactivateRuleUsingUI();

    JobHelpers.waitForJobsToComplete(bot);
    // Ugly hack: give a bit more time for the UI to refresh
    bot.sleep(1000);
    checkIssueIsDefault(project);
    changeRuleParamValue(project);
    checkIssueChanged(project);

    openComplexityRule();
    paramRestoreDefaultLink().click();
    bot.button("Apply").click();
    JobHelpers.waitForJobsToComplete(bot);
    bot.activeShell().close();
    checkIssueIsDefault(project);
  }

  @Test
  public void ruleParametersGlobalDefaults() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("java/java-exclude-rules", "java-exclude-rules");
    JobHelpers.waitForJobsToComplete(bot);

    JavaPackageExplorerBot javaBot = new JavaPackageExplorerBot(bot);
    javaBot.expandAndDoubleClick("java-exclude-rules", "src", "hello", "Hello3.java");
    JobHelpers.waitForJobsToComplete(bot);
    checkIssueIsDefault(project);
    changeRuleParamValue(project);
    checkIssueChanged(project);

    openComplexityRule();
    bot.button("Restore Defaults").click();
    bot.button("Apply").click();
    JobHelpers.waitForJobsToComplete(bot);

    bot.activeShell().close();

    checkIssueIsDefault(project);
  }

  @Test
  public void ruleParametersActivationRoundTrip() {
    openComplexityRule();

    assertThat(bot.spinner().isEnabled()).isTrue();

    bot.treeWithId("slRuleTree")
      .getTreeItem("Java").getNode(0)
      .contextMenu("Deactivate").click();

    assertThat(bot.spinner().isEnabled()).isFalse();

    bot.treeWithId("slRuleTree")
      .getTreeItem("Java").getNode(0)
      .contextMenu("Activate").click();

    assertThat(bot.spinner().isEnabled()).isTrue();

    bot.activeShell().close();
  }

  @Test
  public void defaultLinkVisibilityRoundTrip() {
    bot.getFinder().setShouldFindInvisibleControls(true);
    openComplexityRule();
    assertThat(paramRestoreDefaultLink().isVisible()).isFalse();
    bot.spinner().setSelection(10);
    assertThat(paramRestoreDefaultLink().isVisible()).isTrue();
    bot.spinner().setSelection(15);
    assertThat(paramRestoreDefaultLink().isVisible()).isFalse();
    bot.activeShell().close();
  }

  @Test
  public void open_rules_configuration() {
    assumeTrue(isMarsOrGreater());

    openRulesConfiguration();

    SWTBotTree tree = bot.tree(1);
    assertThat(tree.getAllItems()).hasSize(5 /* HTML, Java, JavaScript, PHP, Python - no TypeScript */);
    SWTBotTreeItem htmlNode = tree.getAllItems()[0];

    assertThat(htmlNode.getText()).isEqualTo("HTML");

    htmlNode.expand();
    assertThat(htmlNode.cell(0, 0)).isEqualTo("\"<!DOCTYPE>\" declarations should appear before \"<html>\" tags");

    bot.button("Cancel").click();
  }

  private SWTBotLink paramRestoreDefaultLink() {
    return bot.link("<a>Restore defaults</a>");
  }

  void openRulesConfiguration() {
    bot.menu("Window").menu("Preferences").click();
    bot.shell("Preferences").activate();
    bot.tree().getTreeItem("SonarLint").select().expand().click()
      .getNode("Rules Configuration").select().click();
  }

  void reactivateRuleUsingUI() {
    openRulesConfiguration();

    bot.button("Restore Defaults").click();
    if (isOxygenOrGreater()) {
      bot.button("Apply and Close").click();
    } else {
      bot.button("OK").click();
    }

  }

  void openComplexityRule() {
    openRulesConfiguration();
    bot.textWithId("slRuleTreeFilter").setText("java:S3776");

    // wait for the tree filtering complete
    bot.waitUntil(new ICondition() {

      private SWTBotTree ruleTree;

      @Override
      public boolean test() throws Exception {
        return ruleTree.getAllItems().length == 1 && ruleTree.getTreeItem("Java").getItems().length == 1;
      }

      @Override
      public void init(SWTBot bot) {
        ruleTree = bot.treeWithId("slRuleTree");
      }

      @Override
      public String getFailureMessage() {
        return Arrays.stream(ruleTree.getAllItems())
          .map(SWTBotTreeItem::getText)
          .collect(Collectors.joining(","));
      }
    });

    bot.treeWithId("slRuleTree")
      .getTreeItem("Java").getNode(0)
      .select().click();
  }

  void changeRuleParamValue(IProject project) throws CoreException {
    openComplexityRule();
    bot.spinner().setSelection(10);
    bot.button("Apply").click();
    JobHelpers.waitForJobsToComplete(bot);
    bot.activeShell().close();
  }

  static void checkIssueChanged(IProject project) throws CoreException {
    List<IMarker> markers = Arrays.asList(project.findMember("src/hello/Hello3.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-exclude-rules/src/hello/Hello3.java", 9, "Replace this use of System.out or System.err by a logger."),
      tuple("/java-exclude-rules/src/hello/Hello3.java", 12, "Refactor this method to reduce its Cognitive Complexity from 24 to the 10 allowed."));
  }

  void checkIssueIsDefault(IProject project) throws CoreException {
    List<IMarker> markers;
    markers = Arrays.asList(project.findMember("src/hello/Hello3.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/java-exclude-rules/src/hello/Hello3.java", 9, "Replace this use of System.out or System.err by a logger."),
      tuple("/java-exclude-rules/src/hello/Hello3.java", 12, "Refactor this method to reduce its Cognitive Complexity from 24 to the 15 allowed."));
  }

}
