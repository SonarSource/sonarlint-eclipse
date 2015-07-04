/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its.bots;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.sonar.ide.eclipse.ui.its.utils.JobHelpers;

public class ImportMavenProjectBot {
  private final SWTWorkbenchBot bot;

  public ImportMavenProjectBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    SWTBotMenu menu = bot.menu("File");
    menu = menu.menu("Import...");
    menu.click();
    bot.shell("Import").activate();
    bot.tree().expandNode("Maven").select("Existing Maven Projects");
    bot.button("Next >").click();
  }

  public ImportMavenProjectBot setPath(String path) {
    bot.comboBox().setText(path);
    bot.button("Refresh").click();
    bot.waitUntil(Conditions.widgetIsEnabled(bot.button("Finish")), 10000);
    return this;
  }

  public void finish() {
    bot.button("Finish").click();
    JobHelpers.waitForJobsToComplete(bot);
  }
}
