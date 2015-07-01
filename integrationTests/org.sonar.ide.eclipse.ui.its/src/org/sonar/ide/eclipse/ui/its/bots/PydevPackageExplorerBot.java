/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its.bots;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.sonar.ide.eclipse.ui.its.utils.ContextMenuHelper;

public class PydevPackageExplorerBot {
  private final SWTWorkbenchBot bot;
  private final SWTBotView viewBot;

  public PydevPackageExplorerBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    viewBot = bot.viewById("org.python.pydev.navigator.view");
  }

  public PydevPackageExplorerBot expandAndSelect(String... nodes) {
    viewBot.bot().tree().expandNode(nodes).select();
    return this;
  }

  public boolean hasItems() {
    return viewBot.bot().tree().hasItems();
  }

  public void clickContextMenu(String... texts) {
    ContextMenuHelper.clickContextMenu(viewBot.bot().tree(), texts);
  }
}
