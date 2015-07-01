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

public class SonarPreferencesBot {
  private final SWTWorkbenchBot bot;

  public SonarPreferencesBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    bot.menu("Window").menu("Preferences").click();
    bot.shell("Preferences").activate();
    bot.tree().select("SonarQube");
  }

  public int getServersCount() {
    return bot.table().rowCount();
  }

  public SonarPreferencesBot setJvmArguments(String jvmArguments) {
    bot.textWithLabel("JVM arguments for preview analysis:").setText(jvmArguments);
    return this;
  }

  public String getJvmArguments() {
    return bot.textWithLabel("JVM arguments for preview analysis:").getText();
  }

  public void ok() {
    SWTBotShell shell = bot.shell("Preferences").activate();
    bot.button("OK").click();
    bot.waitUntil(Conditions.shellCloses(shell));
  }
}
