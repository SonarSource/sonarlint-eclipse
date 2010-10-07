package org.sonar.ide.eclipse.ui.tests.utils;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTree;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;

public final class SwtBotUtils {

  public static void closeViewQuietly(SWTWorkbenchBot bot, String id) {
    try {
      bot.viewById(id).close();
    } catch (WidgetNotFoundException e) {
      // ignore
    }
  }

  public static void openPerspective(SWTWorkbenchBot bot, String id) {
    bot.perspectiveById(id).activate();
  }

  public static SWTBotTree selectProject(SWTWorkbenchBot bot, String projectName) {
    SWTBotTree tree = bot.viewById(JavaUI.ID_PACKAGES).bot().tree();
    SWTBotTreeItem treeItem = null;
    treeItem = tree.getTreeItem(projectName);
    treeItem.select();
    return tree;
  }

  private SwtBotUtils() {
  }

}
