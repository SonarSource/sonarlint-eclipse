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

public class SonarServerPreferencesBot {
  private final SWTWorkbenchBot bot;

  public SonarServerPreferencesBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    bot.menu("Window").menu("Preferences").click();
    bot.shell("Preferences").activate();
    bot.tree().expandNode("SonarQube").select("Servers");
  }

  public int getServersCount() {
    return bot.table().rowCount();
  }

  public SonarServerPreferencesBot select(String id) {
    bot.table().getTableItem(id).select();
    return this;
  }

  public SonarServerWizardBot add() {
    bot.button("Add...").click();
    bot.waitUntil(Conditions.shellIsActive("Add SonarQube Server"));
    return new SonarServerWizardBot(bot, false);
  }

  public SonarServerWizardBot edit() {
    bot.button("Edit...").click();
    bot.waitUntil(Conditions.shellIsActive("Edit SonarQube Server"));
    return new SonarServerWizardBot(bot, true);
  }

  public SonarServerPreferencesBot remove() {
    bot.button("Remove").click();
    bot.waitUntil(Conditions.shellIsActive("Remove SonarQube server configuration"));
    bot.button("OK").click();
    return this;
  }

  public void ok() {
    SWTBotShell shell = bot.shell("Preferences").activate();
    bot.button("OK").click();
    bot.waitUntil(Conditions.shellCloses(shell));
  }
}
