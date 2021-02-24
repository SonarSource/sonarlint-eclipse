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

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;

public class ServersViewBot {
  private final SWTBot bot;

  public ServersViewBot(SWTWorkbenchBot bot) {
    this.bot = bot.viewById("org.sonarlint.eclipse.ui.ServersView").bot();
  }

  public void waitForServerUpdate(String serverName) {
    waitForServerUpdateAndCheckVersion(serverName, null);
  }

  public void waitForServerUpdateAndCheckVersion(String serverName, String version) {
    bot.waitUntil(new DefaultCondition() {
      @Override
      public boolean test() {
        return UIThreadRunnable.syncExec(() -> getFirstServerDescription().matches(serverName + " \\[" +
              (version != null ? "Version: " + substringBefore(version, '-') + "(.*), " : "")
              + "Last storage update: (.*)\\]"));
        }

      @Override
      public String getFailureMessage() {
        return "Server status is: " + getFirstServerDescription();
      }
    }, 20_000);
  }

  private String substringBefore(String string, char separator) {
    int indexOfSeparator = string.indexOf(separator);
    if (indexOfSeparator == -1) {
      return string;
    }
    return string.substring(0, indexOfSeparator);
  }

  private String getFirstServerDescription() {
    return bot.tree().getAllItems()[0].getText();
  }
}
