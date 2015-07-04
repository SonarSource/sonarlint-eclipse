/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its.bots;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.sonar.ide.eclipse.ui.its.utils.ContextMenuHelper;
import org.sonar.ide.eclipse.ui.its.utils.JobHelpers;

public class JavaPackageExplorerBot {
  private final SWTWorkbenchBot bot;
  private final SWTBotView viewBot;

  public JavaPackageExplorerBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    viewBot = bot.viewById(JavaUI.ID_PACKAGES);
  }

  public JavaElementFiltersDialogBot filters() {
    viewBot.menu("&Filters...").click();
    return new JavaElementFiltersDialogBot(bot);
  }

  public JavaPackageExplorerBot expandAndSelect(String... nodes) {
    viewBot.bot().tree().expandNode(nodes).select();
    JobHelpers.waitForJobsToComplete(bot);
    return this;
  }

  public boolean hasItems() {
    return viewBot.bot().tree().hasItems();
  }

  public void clickContextMenu(String... texts) {
    ContextMenuHelper.clickContextMenu(viewBot.bot().tree(), texts);
  }
}
