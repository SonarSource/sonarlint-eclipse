/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2021 SonarSource SA
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
import org.sonarlint.eclipse.its.AbstractSonarLintTest;

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
    wizardBot.tree().expandNode("SonarLint").select("New SonarQube/SonarCloud Connection");
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
    String msgToFind = " " + msg;
    try {
      if (AbstractSonarLintTest.is2020_12OrGreater()) {
        // https://github.com/eclipse/eclipse.platform.ui/commit/40b5475e2790b36228537d6446e470b36386b17c#diff-2fdff255edfd3ae715187b2161f594d0
        wizardBot.label(msgToFind);
      } else {
        wizardBot.text(msgToFind);
      }
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

  public String getOrganization() {
    return wizardBot.textWithLabel(ORGANIZATION_LABEL).getText();
  }

  public void typeOrganizationAndSelectFirst(String orgaFragment) {
    SWTBotText orgaTextField = wizardBot.textWithLabel(ORGANIZATION_LABEL);
    orgaTextField.setText("");
    orgaTextField.typeText(orgaFragment, 100);
    SWTBotTable proposalsTable = getCompletionProposalsTable(wizardBot);
    selectProposal(proposalsTable, 0);
  }

  private static SWTBotTable getCompletionProposalsTable(SWTBot bot) {

    bot.sleep(100); // Wait for auto-completion shell to be displayed
    List<Shell> shells = bot.shells("");
    Table proposalsTable = null;

    long timeout = SWTBotPreferences.TIMEOUT;
    boolean findInvisibleControls = bot.getFinder().shouldFindInvisibleControls();

    try {
      SWTBotPreferences.TIMEOUT = 200;
      bot.getFinder().setShouldFindInvisibleControls(true);
      for (Shell shell : shells) {
        try {
          proposalsTable = bot.widget(widgetOfType(Table.class), shell);
        } catch (WidgetNotFoundException ex) {
          continue;
        }
        break;
      }
    } finally {
      // Restore
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

  public boolean getNotificationEnabled() {
    // checkBoxWithLabel("Receive notifications from SonarCloud") doesn't work :(
    return wizardBot.checkBox().isChecked();
  }

}
