/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its.bots;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class IssuesViewBot {
  private final SWTWorkbenchBot bot;

  public IssuesViewBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    SWTBotMenu windowMenu = bot.menu("Window");
    windowMenu.click();
    SWTBotMenu showViewMenu = windowMenu.menu("Show View");
    showViewMenu.click();
    SWTBotMenu otherMenu = showViewMenu.menu("Other...");
    otherMenu.click();
    SWTBotTree tree = bot.tree(0);
    SWTBotTreeItem tItem = tree.getTreeItem("SonarQube").expand();
    tItem.getNode("SonarQube Issues").doubleClick();
  }

}
