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

package org.sonar.ide.eclipse.console;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * @author Jérémie Lagarde
 */
public class SonarConsolePageParticipant implements IConsolePageParticipant {

  private RemoveConsoleAction removeConsoleAction;

  public void init(IPageBookViewPage page, IConsole console) {
    this.removeConsoleAction = new RemoveConsoleAction();
    IActionBars bars = page.getSite().getActionBars();
    bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, removeConsoleAction);
  }

  public void dispose() {
    this.removeConsoleAction = null;
  }

  public void activated() {
  }

  public void deactivated() {
  }

  @SuppressWarnings("unchecked")
  public Object getAdapter(Class adapter) {
    return null;
  }
}