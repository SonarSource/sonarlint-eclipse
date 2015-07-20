/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its.bots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.exceptions.QuickFixNotFoundException;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.matchers.AbstractMatcher;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.swtbot.swt.finder.results.WidgetResult;
import org.eclipse.swtbot.swt.finder.utils.MessageFormat;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.waits.WaitForObjectCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.eclipse.swtbot.swt.finder.widgets.TimeoutException;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.sonar.ide.eclipse.ui.its.utils.JobHelpers;

import static org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable.syncExec;
import static org.eclipse.swtbot.swt.finder.matchers.WidgetMatcherFactory.widgetOfType;

/**
 * Most code to play with content assist was copied from {@link org.eclipse.swtbot.eclipse.finder.widgets.SWTBotEclipseEditor}
 *
 */
public class ConfigureProjectsWizardBot {
  private final SWTWorkbenchBot bot;
  private SWTBotButton finishButton;
  private SWTBotTable table;
  private SWTBotText text;

  public ConfigureProjectsWizardBot(SWTWorkbenchBot bot) {
    this.bot = bot;
    bot.shell("Associate with SonarQube").activate();
    finishButton = bot.button("&Finish");
    bot.waitUntil(Conditions.widgetIsEnabled(finishButton), 1000 * 30);
    table = bot.table();
  }

  public String getStatus() {
    return bot.text(0).getText();
  }

  public void cancel() {
    SWTBotShell shell = bot.shell("Associate with SonarQube").activate();
    bot.button("Cancel").click();
    bot.waitUntil(Conditions.shellCloses(shell));
  }

  public void finish() {
    SWTBotShell shell = bot.shell("Associate with SonarQube").activate();
    finishButton.click();
    bot.waitUntil(Conditions.shellCloses(shell), 20000);

    // Wait for remote issues to be fetched
    JobHelpers.waitForJobsToComplete(bot);
  }

  public String getAssociatedProjectText(int rowIndex) {
    return table.cell(rowIndex, 1);
  }

  public void editRow(int rowIndex) {
    table.click(rowIndex, 1);
    text = bot.text();
  }

  public void typeText(String textToType) {
    this.text.setText(textToType);
  }

  /**
   * Gets the auto completion proposal matching the given text..
   *
   * @param insertText the proposal text to type before auto completing
   * @return the list of proposals
   * @throws TimeoutException if the autocomplete shell did not close in time.
   * @since 1.2
   */
  @SuppressWarnings("all")
  public List<String> getAutoCompleteProposals(String insertText) {
    typeText(insertText);
    activateAutoCompleteShell();
    WaitForObjectCondition<SWTBotTable> autoCompleteAppears = autoCompleteAppears(tableWithRowIgnoringCase(insertText));
    waitUntil(autoCompleteAppears);
    final SWTBotTable autoCompleteTable = autoCompleteAppears.get(0);
    List<String> proposals = getRows(autoCompleteTable);
    return proposals;
  }

  /**
   * Auto completes the given proposal.
   *
   * @param insertText the text to be inserted before activating the auto-complete.
   * @param proposalText the auto-completion proposal to select from the list.
   */
  public void autoCompleteProposal(String insertText, String proposalText) {
    typeText(insertText);
    activateAutoCompleteShell();
    WaitForObjectCondition<SWTBotTable> autoCompleteTable = autoCompleteAppears(tableWithRow(insertText));
    waitUntil(autoCompleteTable);
    selectProposal(autoCompleteTable.get(0), proposalText);
  }

  /**
   * Attempst to applys the quick fix.
   * <p>
   * FIXME: this needs a lot of optimization.
   * </p>
   *
   * @param proposalTable the table containing the quickfix.
   * @param proposalText the name of the quickfix to apply.
   */
  private void selectProposal(SWTBotTable proposalTable, String proposalText) {
    if (proposalTable.containsItem(proposalText)) {
      selectProposal(proposalTable, proposalTable.indexOf(proposalText));
      return;
    }
    throw new QuickFixNotFoundException("Quickfix options not found. Giving up."); //$NON-NLS-1$
  }

  /**
   * Applies the specified quickfix.
   *
   * @param proposalTable the table containing the quickfix.
   * @param proposalIndex the index of the quickfix.
   */
  private void selectProposal(final SWTBotTable proposalTable, final int proposalIndex) {
    UIThreadRunnable.asyncExec(new VoidResult() {
      @Override
      public void run() {
        Table table = proposalTable.widget;
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

  private Matcher<SWTBotTable> tableWithRow(final String itemText) {
    return new AbstractMatcher<SWTBotTable>() {

      @Override
      protected boolean doMatch(Object item) {
        return ((SWTBotTable) item).containsItem(itemText);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("table with item (").appendText(itemText).appendText(")");
      }
    };
  }

  private WaitForObjectCondition<SWTBotTable> autoCompleteAppears(Matcher<SWTBotTable> tableMatcher) {
    return new WaitForObjectCondition<SWTBotTable>(tableMatcher) {
      @Override
      protected List<SWTBotTable> findMatches() {
        SWTBotTable autoCompleteTable = getProposalTable();
        if (matcher.matches(autoCompleteTable)) {
          return Arrays.asList(autoCompleteTable);
        }
        return null;
      }

      @Override
      public String getFailureMessage() {
        return "Could not find auto complete proposal using matcher " + matcher;
      }

    };
  }

  /**
   * Gets the quick fix table.
   *
   * @param proposalShell the shell containing the quickfixes.
   * @return the table containing the quickfix.
   */
  private SWTBotTable getProposalTable() {
    try {
      Table table = bot.widget(widgetOfType(Table.class), activatePopupShell().widget);
      SWTBotTable swtBotTable = new SWTBotTable(table);
      System.out.println(MessageFormat.format("Found table containing proposals -- {0}", getRows(swtBotTable)));
      return swtBotTable;
    } catch (Exception e) {
      throw new QuickFixNotFoundException("Quickfix options not found. Giving up.", e); //$NON-NLS-1$
    }
  }

  /**
   * This activates the popup shell.
   *
   * @return The shell.
   */
  private SWTBotShell activatePopupShell() {
    try {
      Shell mainWindow = syncExec(new WidgetResult<Shell>() {
        @Override
        public Shell run() {
          return text.widget.getShell();
        }
      });

      final List<Shell> shells = bot.shells("", mainWindow);
      Shell widgetShell = syncExec(new WidgetResult<Shell>() {
        @Override
        public Shell run() {
          for (int j = 0; j < shells.size(); j++) {
            Shell s = shells.get(j);
            Control[] children = s.getChildren();
            for (int i = 0; i < children.length; i++) {
              // Select shell which has content assist table
              if (children[i] instanceof Table) {
                return s;
              }
            }
          }
          return shells.get(0);
        }
      });
      SWTBotShell shell = new SWTBotShell(widgetShell);
      shell.activate();
      return shell;
    } catch (Exception e) {
      throw new QuickFixNotFoundException("Quickfix popup not found. Giving up.", e); //$NON-NLS-1$
    }
  }

  private void waitUntil(WaitForObjectCondition<SWTBotTable> table) {
    bot.waitUntil(table, 10000);
  }

  private void activateAutoCompleteShell() {
    text.pressShortcut(Keystrokes.CTRL, Keystrokes.SPACE);
  }

  private Matcher<SWTBotTable> tableWithRowIgnoringCase(final String itemText) {
    final String lowerCaseText = itemText.toLowerCase();
    return new AbstractMatcher<SWTBotTable>() {

      @Override
      protected boolean doMatch(Object item) {
        List<String> rows = getRows((SWTBotTable) item);
        for (String row : rows) {
          if (row.toLowerCase().startsWith(lowerCaseText)) {
            return true;
          }
        }
        return false;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("table with item (").appendText(itemText).appendText(")");
      }
    };
  }

  private List<String> getRows(SWTBotTable table) {
    int rowCount = table.rowCount();
    List<String> result = new ArrayList<String>();
    for (int i = 0; i < rowCount; i++)
      result.add(table.cell(i, 0));
    return result;
  }

}
