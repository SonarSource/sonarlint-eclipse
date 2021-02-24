/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonarlint.eclipse.its.bots;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.ContextMenuHelper;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.sonarlint.eclipse.its.utils.JobHelpers;

public class JavaPackageExplorerBot {
  private final SWTWorkbenchBot bot;
  private final SWTBotView viewBot;

  public JavaPackageExplorerBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    viewBot = bot.viewById(JavaUI.ID_PACKAGES);
  }

  public JavaPackageExplorerBot expandAndSelect(String... nodes) {
    viewBot.bot().tree().expandNode(nodes).select();
    JobHelpers.waitForJobsToComplete(bot);
    return this;
  }

  public JavaPackageExplorerBot expandAndDoubleClick(String... nodes) {
    viewBot.bot().tree().expandNode(nodes).doubleClick();
    return this;
  }

  public boolean hasItems() {
    return viewBot.bot().tree().hasItems();
  }

  public void clickContextMenu(String... texts) {
    new SWTBotMenu(ContextMenuHelper.contextMenu(viewBot.bot().tree(), texts)).click();
  }

  public void openSonarLintProperties(String projectName) {
    viewBot.bot().tree().select(projectName);
    clickContextMenu("Properties");
  }

  public void excludeFile(String... nodes) {
    viewBot.bot().tree().expandNode(nodes).contextMenu("SonarLint").menu("Exclude").click();
  }

  public void triggerManualAnalysis(String... nodes) {
    viewBot.bot().tree().expandNode(nodes).contextMenu("SonarLint").menu("Analyze").click();
  }

  public boolean isManualAnalysisEnabled(String... nodes) {
    return viewBot.bot().tree().expandNode(nodes).contextMenu("SonarLint").menu("Analyze").isEnabled();
  }

  public boolean isExcludeEnabled(String... nodes) {
    return viewBot.bot().tree().expandNode(nodes).contextMenu("SonarLint").menu("Exclude").isEnabled();
  }
}
