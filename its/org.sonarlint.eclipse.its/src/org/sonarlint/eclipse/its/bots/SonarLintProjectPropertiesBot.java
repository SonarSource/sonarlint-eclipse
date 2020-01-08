/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2020 SonarSource SA
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
import org.eclipse.swtbot.eclipse.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.sonarlint.eclipse.its.AbstractSonarLintTest;

public class SonarLintProjectPropertiesBot {
  private final SWTWorkbenchBot bot;
  private SWTBotShell shell;

  public SonarLintProjectPropertiesBot(SWTWorkbenchBot bot, String projectName) {
    this.bot = bot;
    shell = bot.shell("Properties for " + projectName);
    shell.activate();
    bot.tree().select("SonarLint");
  }

  public SonarLintProjectPropertiesBot clickAutoAnalysis() {
    shell.activate();
    bot.checkBox().click();
    return this;
  }

  public void ok() {
    if (AbstractSonarLintTest.isOxygenOrGreater()) {
      bot.button("Apply and Close").click();
    } else {
      bot.button("OK").click();
    }
    bot.waitUntil(Conditions.shellCloses(shell));
  }

}
