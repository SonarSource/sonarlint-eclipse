package org.sonar.ide.eclipse.ui.tests.bots;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;

public class ConfigureProjectsWizardBot {
  private SWTBot bot = new SWTBot();

  public ConfigureProjectsWizardBot() {
    bot.shell("Associate with Sonar").activate();
  }

  public String getStatus() {
    return bot.text(0).getText();
  }

  public void cancel() {
    bot.button("Cancel").click();
  }

  public void finish() {
    SWTBotButton button = bot.button("&Finish");
    button.click();
    bot.waitUntil(Conditions.widgetIsEnabled(button), 1000 * 30);
  }

  public void find() {
    SWTBotButton button = bot.button("Find on server");
    button.click();
    bot.waitUntil(Conditions.widgetIsEnabled(button), 1000 * 30);
  }
}
