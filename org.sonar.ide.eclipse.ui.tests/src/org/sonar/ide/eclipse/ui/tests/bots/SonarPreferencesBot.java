package org.sonar.ide.eclipse.ui.tests.bots;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

public class SonarPreferencesBot {
  private SWTBot bot = new SWTBot();

  public SonarPreferencesBot() {
    bot.menu("Window").menu("Preferences").click();
    bot.shell("Preferences").activate();
    bot.tree().select("Sonar");
  }

  public int getServersCount() {
    return bot.table().rowCount();
  }

  public SonarPreferencesBot select(String url) {
    bot.table().getTableItem(url).select();
    return this;
  }

  public SonarServerWizardBot add() {
    bot.button("Add...").click();
    bot.waitUntil(Conditions.shellIsActive("Add Sonar Server"));
    return new SonarServerWizardBot(false);
  }

  public SonarServerWizardBot edit() {
    bot.button("Edit...").click();
    bot.waitUntil(Conditions.shellIsActive("Edit Sonar Server"));
    return new SonarServerWizardBot(true);
  }

  public SonarPreferencesBot remove() {
    bot.button("Remove").click();
    bot.waitUntil(Conditions.shellIsActive("Remove sonar server connection"));
    bot.button("OK").click();
    return this;
  }

  public void ok() {
    SWTBotShell shell = bot.shell("Preferences").activate();
    bot.button("OK").click();
    bot.waitUntil(Conditions.shellCloses(shell));
  }
}
