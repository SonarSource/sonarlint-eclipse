/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its.bots;

import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.sonar.ide.eclipse.ui.its.utils.JobHelpers;

public class ImportProjectBot {
  private final SWTWorkbenchBot bot;

  public ImportProjectBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    SWTBotMenu menu = bot.menu("File");
    menu = menu.menu("Import...");
    menu.click();
    bot.shell("Import").activate();
    bot.tree().expandNode("General").select("Existing Projects into Workspace");
    bot.button("Next >").click();
  }

  public ImportProjectBot setPath(String path) {
    Control textOrCombo = bot.getFocusedWidget();
    if (textOrCombo instanceof Combo) {
      // Eclipse 4.3+
      bot.comboBox().setText(path);
    }
    else {
      // Before Eclipse 4.3
      bot.text().setText(path);
    }
    bot.button("Refresh").click();
    bot.waitUntil(Conditions.widgetIsEnabled(bot.button("Finish")), 10000);
    return this;
  }

  public void finish() {
    bot.button("Finish").click();
    JobHelpers.waitForJobsToComplete(bot);
  }
}
