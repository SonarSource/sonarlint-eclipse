package org.sonar.ide.eclipse.ui.tests.utils;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;

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

  public static boolean waitForClose(SWTBotShell shell) {
    for (int i = 0; i < 50; i++) {
      if ( !shell.isOpen()) {
        return true;
      }
      shell.bot().sleep(200);
    }
    shell.close();
    return false;
  }

  private SwtBotUtils() {
  }

}
