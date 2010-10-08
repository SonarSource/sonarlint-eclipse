package org.sonar.ide.eclipse.ui.tests.bots;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.sonar.ide.eclipse.ui.tests.utils.ContextMenuHelper;

public class PackageExplorerBot {
  private SWTBot bot;

  public PackageExplorerBot() {
    bot = new SWTWorkbenchBot().viewById(JavaUI.ID_PACKAGES).bot();
  }

  public PackageExplorerBot expandAndSelect(String... nodes) {
    bot.tree().expandNode(nodes).select();
    return this;
  }

  public void clickContextMenu(String... texts) {
    ContextMenuHelper.clickContextMenu(bot.tree(), texts);
  }
}
