package org.sonar.ide.eclipse.ui.tests.bots;

import org.eclipse.swtbot.swt.finder.SWTBot;

public class ImportProjectBot {
  private SWTBot bot = new SWTBot();

  public ImportProjectBot() {
    bot.menu("File").menu("Import...").click();
    bot.shell("Import").activate();
    bot.tree().expandNode("General").select("Existing Projects into Workspace");
    bot.button("Next >").click();
  }

  public ImportProjectBot setPath(String path) {
    bot.text().setText(path);
    bot.button("Refresh").click();
    return this;
  }

  public void finish() {
    bot.button("Finish").click();
    bot.sleep(5000);
  }
}
