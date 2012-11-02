/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.tests.bots;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.sonar.ide.eclipse.ui.tests.utils.ContextMenuHelper;

public class PackageExplorerBot {
  private final SWTBotView viewBot;

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
