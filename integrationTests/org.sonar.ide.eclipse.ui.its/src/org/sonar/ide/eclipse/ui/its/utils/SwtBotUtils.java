/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its.utils;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;

public final class SwtBotUtils {

  private SwtBotUtils() {
  }

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

}
