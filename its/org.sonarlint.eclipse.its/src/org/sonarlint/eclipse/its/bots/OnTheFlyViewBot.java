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

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public class OnTheFlyViewBot {
  private final SWTWorkbenchBot bot;

  public OnTheFlyViewBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    SWTBotMenu windowMenu = bot.menu("Window");
    windowMenu.click();
    SWTBotMenu showViewMenu = windowMenu.menu("Show View");
    showViewMenu.click();
    SWTBotMenu otherMenu = showViewMenu.menu("Other...");
    otherMenu.click();
    SWTBotShell showViewDialog = bot.shell("Show View");
    SWTBotTree tree = showViewDialog.bot().tree(0);
    SWTBotTreeItem tItem = tree.getTreeItem("SonarLint").expand();
    try {
      tItem.getNode("SonarLint On-The-Fly").doubleClick();
    } catch (Exception e) {
      // https://bugs.eclipse.org/bugs/show_bug.cgi?id=519306
    }

  }

  public SWTBotView show() {
    SWTBotView view = bot.viewById("org.sonarlint.eclipse.ui.views.issues.IssuesView");
    view.show();
    return view;
  }

}
