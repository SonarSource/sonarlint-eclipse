/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2017 SonarSource SA
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

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ServerConnectionWizardBot {
  private static final String CONNECTION_NAME_LABEL = "Connection name:";
  private static final String NEXT_MNEMONIC = "Next >";
  private final SWTWorkbenchBot bot;
  private SWTBot wizardBot;
  private SWTBotShell wizardShell;

  public ServerConnectionWizardBot(SWTWorkbenchBot bot) {
    this.bot = bot;
  }

  public ServerConnectionWizardBot openFromFileNewWizard() {
    bot.menu("File").menu("New", "Other...").click();
    wizardShell = bot.activeShell();
    wizardBot = wizardShell.bot();
    wizardBot.tree().expandNode("SonarLint").select("New Server Connection");
    wizardBot.button(NEXT_MNEMONIC).click();
    return this;
  }

  public void assertTitle(String title) {
    assertThat(wizardShell.getText()).isEqualTo(title);
  }

  public void clickNext() {
    wizardBot.button(NEXT_MNEMONIC).click();
  }

  public boolean isNextEnabled() {
    return wizardBot.button(NEXT_MNEMONIC).isEnabled();
  }

  public void setServerUrl(String url) {
    wizardBot.textWithLabel("URL:").setText(url);
  }

  public void assertErrorMessage(String msg) {
    // There is a space added, see TitleAreaDialog#setErrorMessage(String)
    try {
      wizardBot.text(" " + msg);
    } catch (WidgetNotFoundException e) {
      fail("Expecting error message '" + msg + "'", e);
    }
  }

  public void selectSonarQube() {
    wizardBot.radio(1).click();
  }

  public void selectSonarCloud() {
    wizardBot.radio(0).click();
  }

  public void selectUsernamePassword() {
    wizardBot.radio("Username + Password").click();
  }

  public void selectToken() {
    wizardBot.radio("Token").click();
  }

  public void setUsername(String login) {
    wizardBot.textWithLabel("Username:").setText(login);
  }

  public void setPassword(String password) {
    wizardBot.textWithLabel("Password:").setText(password);
  }

  public String getConnectionName() {
    return wizardBot.textWithLabel(CONNECTION_NAME_LABEL).getText();
  }

  public void setConnectionName(String name) {
    wizardBot.textWithLabel(CONNECTION_NAME_LABEL).setText(name);
  }

  public void clickFinish() {
    wizardBot.button("Finish").click();
  }

}
