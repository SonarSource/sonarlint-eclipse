/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.utils.SWTBotPreferences;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetOfType.widgetOfType;

public class ServerConnectionWizardBot {
  private static final String ORGANIZATION_LABEL = "Organization:";
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

  public void setToken(String token) {
    wizardBot.textWithLabel("Token:").setText(token);
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

  public void waitForOrganizationsToBeFetched() {
    wizardBot.waitUntilWidgetAppears(new DefaultCondition() {

      @Override
      public boolean test() throws Exception {
        return wizardBot.textWithLabel(ORGANIZATION_LABEL).isVisible();
      }

      @Override
      public String getFailureMessage() {
        return "Expected organization step";
      }
    });
  }

  public void waitForNotificationSupportCheckToBeFetched() {
    wizardBot.waitUntilWidgetAppears(new DefaultCondition() {

      @Override
      public boolean test() throws Exception {
        try {
          // note: actually we expect this to throw always,
          // because swtbot doesn't match invisible elements
          return !wizardBot.checkBox(0).isVisible();
        } catch (WidgetNotFoundException e) {
          return true;
        }
      }

      @Override
      public String getFailureMessage() {
        return "Expected notifications checkbox to be missing";
      }
    });
  }

  public String getOrganization() {
    return wizardBot.textWithLabel(ORGANIZATION_LABEL).getText();
  }

  public void typeOrganizationAndSelectFirst(String orgaFragment) {
    SWTBotText orgaTextField = wizardBot.textWithLabel(ORGANIZATION_LABEL);
    orgaTextField.typeText(orgaFragment, 100);
    SWTBotTable proposalsTable = getCompletionProposalsTable(wizardBot, orgaTextField);
    selectProposal(proposalsTable, 0);
  }

  private static SWTBotTable getCompletionProposalsTable(SWTBot bot, SWTBotText textField) {

    bot.sleep(100); // Wait for auto-completion shell to be displayed
    List<Shell> shells = bot.shells("");
    Table proposalsTable = null;

    long timeout = SWTBotPreferences.TIMEOUT;
    SWTBotPreferences.TIMEOUT = 200;
    boolean findInvisibleControls = bot.getFinder().shouldFindInvisibleControls();
    bot.getFinder().setShouldFindInvisibleControls(true);

    try {
      for (Shell shell : shells) {
        try {
          proposalsTable = bot.widget(widgetOfType(Table.class), shell);
        } catch (WidgetNotFoundException ex) {
          continue;
        }
        break;
      }
    } finally {
      bot.getFinder().setShouldFindInvisibleControls(findInvisibleControls);
      SWTBotPreferences.TIMEOUT = timeout;
    }

    if (proposalsTable == null) {
      throw new RuntimeException("Did not find any completion proposals table ...");
    }
    return new SWTBotTable(proposalsTable);
  }

  private static void selectProposal(final SWTBotTable proposalsTable, final int proposalIndex) {
    UIThreadRunnable.asyncExec(new VoidResult() {

      @Override
      public void run() {
        Table table = proposalsTable.widget;
        table.setSelection(proposalIndex);
        Event event = new Event();
        event.type = SWT.Selection;
        event.widget = table;
        event.item = table.getItem(proposalIndex);
        table.notifyListeners(SWT.Selection, event);
        table.notifyListeners(SWT.DefaultSelection, event);
      }
    });
  }

}
