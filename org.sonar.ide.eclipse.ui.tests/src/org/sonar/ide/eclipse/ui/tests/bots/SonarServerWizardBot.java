/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.ui.tests.bots;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;

public class SonarServerWizardBot {
  private SWTBot bot = new SWTBot();

  /**
   * Can be constructed only from {@link SonarPreferencesBot}.
   */
  protected SonarServerWizardBot(boolean edit) {
    if (edit) {
      bot.shell("Edit Sonar Server").activate();
    } else {
      bot.shell("Add Sonar Server").activate();
    }
  }

  public SonarServerWizardBot setServerUrl(String url) {
    bot.textWithLabel("Sonar server URL :").setText(url);
    return this;
  }

  public String getServerUrl() {
    return bot.textWithLabel("Sonar server URL :").getText();
  }

  public String getUsername() {
    return bot.textWithLabel("Username :").getText();
  }

  public String getPassword() {
    return bot.textWithLabel("Password :").getText();
  }

  public String getStatus() {
    return bot.text(3).getText();
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
