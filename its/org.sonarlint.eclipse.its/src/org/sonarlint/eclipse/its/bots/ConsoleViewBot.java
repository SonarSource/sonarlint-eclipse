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
package org.sonarlint.eclipse.its.bots;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.matchers.WidgetMatcherFactory;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotToolbarDropDownButton;

public class ConsoleViewBot {
  private final SWTWorkbenchBot bot;

  public ConsoleViewBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    SWTBotMenu windowMenu = bot.menu("Window");
    windowMenu.click();
    SWTBotMenu showViewMenu = windowMenu.menu("Show View");
    showViewMenu.click();
    SWTBotMenu consoleMenu = showViewMenu.menu("Console");
    consoleMenu.click();
  }

  public ConsoleViewBot show() {
    showInternal();
    return this;
  }

  public ConsoleViewBot openSonarLintConsole() {
    SWTBotView view = showInternal();
    SWTBotToolbarDropDownButton b = view.toolbarDropDownButton("Open Console");
    org.hamcrest.Matcher<MenuItem> withRegex = WidgetMatcherFactory.withRegex(".*SonarLint.*");
    b.menuItem(withRegex).click();
    view.setFocus();
    return this;
  }

  public ConsoleViewBot enableVerboseLogs() {
    SWTBotView view = showInternal();
    SWTBotToolbarDropDownButton b = view.toolbarDropDownButton("Configure logs");
    SWTBotMenu verboseMenu = b.menuItem("Verbose output");
    if (!verboseMenu.isChecked()) {
      verboseMenu.click();
    }
    SWTBotMenu analysisLogsMenu = b.menuItem("Analysis logs");
    if (!analysisLogsMenu.isChecked()) {
      analysisLogsMenu.click();
    }
    try {
      b.pressShortcut(KeyStroke.getInstance("ESC"));
    } catch (ParseException e) {
    }
    view.setFocus();
    return this;
  }

  private SWTBotView showInternal() {
    SWTBotView view = bot.viewById("org.eclipse.ui.console.ConsoleView");
    view.show();
    view.setFocus();
    return view;
  }

}
