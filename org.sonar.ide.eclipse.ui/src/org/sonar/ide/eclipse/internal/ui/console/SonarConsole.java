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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.sonar.ide.eclipse.internal.ui.Messages;
import org.sonar.ide.eclipse.ui.ISonarConsole;

public class SonarConsole extends IOConsole implements ISonarConsole {

  private static final String TITLE = Messages.SonarConsole_title;

  private IOConsoleOutputStream infoStream;
  private IOConsoleOutputStream warnStream;

  // Colors must be disposed
  private Color warnColor;

  public SonarConsole(ImageDescriptor imageDescriptor) {
    super(TITLE, imageDescriptor);
  }

  @Override
  protected void init() {
    super.init();
    initStreams(Display.getDefault());
  }

  private void initStreams(Display display) {
    this.infoStream = newOutputStream();
    this.warnStream = newOutputStream();

    // TODO make colors configurable
    warnColor = new Color(display, new RGB(255, 0, 0));

    getWarnStream().setColor(warnColor);
    getErrorStream().setColor(warnColor);
  }

  @Override
  protected void dispose() {
    super.dispose();

    warnColor.dispose();
  }

  public void debug(String msg) {
    if (showConsoleOnOutput()) {
      bringConsoleToFront();
    }
    write(getDebugStream(), msg);
  }

  public void info(String msg) {
    if (showConsoleOnOutput()) {
      bringConsoleToFront();
    }
    write(getInfoStream(), msg);
  }

  public void warn(String msg) {
    if (showConsoleOnOutput()) {
      bringConsoleToFront();
    }
    write(getWarnStream(), msg);
  }

  public void error(String msg) {
    if (showConsoleOnOutput()) {
      bringConsoleToFront();
    }
    write(getErrorStream(), msg);
  }

  private void write(IOConsoleOutputStream stream, String msg) {
    try {
      stream.write(msg);
      stream.write("\n"); //$NON-NLS-1$
    } catch (IOException e) {
      // Don't log using slf4j - it will cause a cycle
      e.printStackTrace();
    }
  }

  public void bringConsoleToFront() {
    showConsole();
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

  public void closeConsole() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    manager.removeConsoles(new IConsole[] { this });
  }

  private IOConsoleOutputStream getDebugStream() {
    return infoStream;
  }

  private IOConsoleOutputStream getInfoStream() {
    return infoStream;
  }

  private IOConsoleOutputStream getWarnStream() {
    return warnStream;
  }

  private IOConsoleOutputStream getErrorStream() {
    return warnStream;
  }

  private boolean showConsoleOnOutput() {
    return false;
  }

}
