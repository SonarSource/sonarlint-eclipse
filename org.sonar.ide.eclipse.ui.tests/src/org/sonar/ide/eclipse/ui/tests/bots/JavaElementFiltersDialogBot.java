package org.sonar.ide.eclipse.ui.tests.bots;

import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

public class JavaElementFiltersDialogBot {
  private SWTBot bot = new SWTBot();

  protected JavaElementFiltersDialogBot() {
    bot.shell("Java Element Filters").activate();
  }

  public JavaElementFiltersDialogBot check(String filter) {
    bot.table().getTableItem(filter).check();
    return this;
  }

  public void ok() {
    SWTBotShell shell = bot.shell("Java Element Filters").activate();
    bot.button("OK").click();
    bot.waitUntil(Conditions.shellCloses(shell));
  }

}
