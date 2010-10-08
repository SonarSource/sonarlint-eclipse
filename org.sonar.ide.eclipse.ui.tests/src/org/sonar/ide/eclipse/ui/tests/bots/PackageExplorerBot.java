package org.sonar.ide.eclipse.ui.tests.bots;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.sonar.ide.eclipse.ui.tests.utils.ContextMenuHelper;

public class PackageExplorerBot {
  private SWTBotView viewBot;

  public PackageExplorerBot() {
    viewBot = new SWTWorkbenchBot().viewById(JavaUI.ID_PACKAGES);
  }

  public JavaElementFiltersDialogBot filters() {
    viewBot.menu("&Filters...").click();
    return new JavaElementFiltersDialogBot();
  }

  public PackageExplorerBot expandAndSelect(String... nodes) {
    viewBot.bot().tree().expandNode(nodes).select();
    return this;
  }

  public boolean hasItems() {
    return viewBot.bot().tree().hasItems();
  }

  public void clickContextMenu(String... texts) {
    ContextMenuHelper.clickContextMenu(viewBot.bot().tree(), texts);
  }
}
