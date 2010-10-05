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

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.console.IConsoleManager;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.ui.ConsoleManager;
import org.sonar.ide.ui.ISonarConsole;

/**
 * @author Jérémie Lagarde
 */
public class SonarConsoleFactory extends ConsoleManager implements IConsoleFactory {

  public void openConsole() {
    showConsole();
  }

  protected ISonarConsole create() {
    return  SonarPlugin.getDefault().getConsole();
  }
  
  public static void showConsole() {
    IConsole console = SonarPlugin.getDefault().getConsole();
    if (console != null) {
      IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
      IConsole[] existing = manager.getConsoles();
      boolean exists = false;
      for (int i = 0; i < existing.length; ++i) {
        if(console == existing[i])
          exists = true;
      }
      if(! exists)
        manager.addConsoles(new IConsole[] {console});
      manager.showConsoleView(console);
    }
  }
  
  public static void closeConsole() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    SonarConsole console = SonarPlugin.getDefault().getConsole();
    if (console != null) {
      manager.removeConsoles(new IConsole[] {console});
    }
  }
  
}