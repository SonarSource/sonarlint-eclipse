/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its.bots;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

public class JavaElementFiltersDialogBot {
  private final SWTWorkbenchBot bot;

  protected JavaElementFiltersDialogBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    bot.shell("Java Element Filters").activate();
  }

  public JavaElementFiltersDialogBot check(String filter) {
    bot.table().getTableItem(filter).check();
    return this;
  }

  public JavaElementFiltersDialogBot uncheck(String filter) {
    bot.table().getTableItem(filter).uncheck();
    return this;
  }

  public void ok() {
    SWTBotShell shell = bot.shell("Java Element Filters").activate();
    bot.button("OK").click();
    bot.waitUntil(Conditions.shellCloses(shell));
  }

}
