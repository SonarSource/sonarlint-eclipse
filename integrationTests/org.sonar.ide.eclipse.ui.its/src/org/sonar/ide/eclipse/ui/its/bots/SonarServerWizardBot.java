/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its.bots;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;

public class SonarServerWizardBot {
  private final SWTWorkbenchBot bot;

  /**
   * Can be constructed only from {@link SonarServerPreferencesBot}.
   */
  protected SonarServerWizardBot(SWTWorkbenchBot bot, boolean edit) {
    this.bot = bot;
    if (edit) {
      bot.shell("Edit SonarQube Server").activate();
    } else {
      bot.shell("Add SonarQube Server").activate();
    }
  }

  public SonarServerWizardBot setServerUrl(String url) {
    bot.textWithLabel("SonarQube Server URL:").setText(url);
    return this;
  }

  public String getServerId() {
    return bot.textWithLabel("SonarQube Server ID:").getText();
  }

  public String getServerUrl() {
    return bot.textWithLabel("SonarQube Server URL:").getText();
  }

  public String getUsername() {
    return bot.textWithLabel("Username:").getText();
  }

  public String getPassword() {
    return bot.textWithLabel("Password:").getText();
  }

  public String getStatus() {
    return bot.text(4).getText();
  }

  public SonarServerWizardBot testConnection() {
    SWTBotButton button = bot.button("Test connection");
    button.click();
    bot.waitUntil(Conditions.widgetIsEnabled(button), 1000 * 30);
    return this;
  }

  public void finish() {
    bot.button("Finish").click();
  }
}
