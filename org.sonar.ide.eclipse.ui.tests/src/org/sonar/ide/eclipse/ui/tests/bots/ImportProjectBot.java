/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

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
