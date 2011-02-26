/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.ide.eclipse.internal.ui.console;

import java.io.IOException;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.*;
import org.sonar.ide.eclipse.internal.ui.Messages;
import org.sonar.ide.eclipse.ui.ISonarConsole;

public class SonarConsole extends IOConsole implements ISonarConsole {

  private static final String TITLE = Messages.SonarConsole_title;

  public SonarConsole(ImageDescriptor imageDescriptor) {
    super(TITLE, imageDescriptor);
  }

  public void debug(String msg) {
    write(msg);
  }

  public void info(String msg) {
    write(msg);
  }

  public void error(String msg) {
    write(msg);
  }

  private void write(String msg) {
    try {
      IOConsoleOutputStream stream = newOutputStream();
      stream.write(msg);
      stream.write("\n"); //$NON-NLS-1$
      stream.close();
    } catch (IOException e) {
      // Don't log using slf4j - it will cause a cycle
      e.printStackTrace();
    }
  }

  public void showConsole() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    for (IConsole console : manager.getConsoles()) {
      if (this == console) {
        manager.showConsoleView(this);
        return;
      }
    }
    // not found - create a new one
    manager.addConsoles(new IConsole[] { this });
  }

}
