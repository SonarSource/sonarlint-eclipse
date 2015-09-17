/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonarlint.eclipse.ui.internal.console;

import java.io.IOException;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.sonarlint.eclipse.core.SonarEclipseException;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.jobs.LogListener;
import org.sonarlint.eclipse.ui.internal.ISonarLintConsole;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class SonarLintConsole extends IOConsole implements LogListener, ISonarLintConsole, IPropertyChangeListener {

  public static final String P_DEBUG_OUTPUT = "debugOutput"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE = "showConsole"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE_NEVER = "never"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE_ON_OUTPUT = "onOutput"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE_ON_ERROR = "onError"; //$NON-NLS-1$

  private static final String TITLE = Messages.SonarConsole_title;

  private IOConsoleOutputStream infoStream;
  private IOConsoleOutputStream warnStream;
  private IOConsoleOutputStream debugStream;

  // Colors must be disposed
  private Color warnColor;
  private Color debugColor;

  private boolean initialized = false;

  public SonarLintConsole(ImageDescriptor imageDescriptor) {
    super(TITLE, imageDescriptor);
    initStreams(Display.getDefault());
  }

  private void initStreams(Display display) {
    if (!initialized) {
      this.infoStream = newOutputStream();
      this.warnStream = newOutputStream();
      this.debugStream = newOutputStream();

      // TODO make colors configurable
      warnColor = new Color(display, new RGB(255, 0, 0));
      debugColor = new Color(display, new RGB(0, 0, 255));

      getWarnStream().setColor(warnColor);
      getDebugStream().setColor(debugColor);

      // install font
      setFont(JFaceResources.getFontRegistry().get("pref_console_font")); //$NON-NLS-1$

      initialized = true;
    }
  }

  @Override
  protected void dispose() {
    super.dispose();

    warnColor.dispose();
    debugColor.dispose();
  }

  @Override
  public void info(String msg) {
    if (isShowConsoleOnOutput()) {
      bringConsoleToFront();
    }
    write(getInfoStream(), msg);
  }

  @Override
  public void error(String msg) {
    if (isShowConsoleOnOutput() || isShowConsoleOnError()) {
      bringConsoleToFront();
    }
    write(getWarnStream(), msg);
  }

  @Override
  public void debug(String msg) {
    if (isDebugEnabled()) {
      if (isShowConsoleOnOutput()) {
        bringConsoleToFront();
      }
      write(getDebugStream(), msg);
    }
  }

  private void write(IOConsoleOutputStream stream, String msg) {
    if (msg == null) {
      return;
    }
    try {
      stream.write(msg);
    } catch (IOException e) {
      throw new SonarEclipseException("Unable to write in console", e);
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
    manager.addConsoles(new IConsole[] {this});
  }

  public void closeConsole() {
    SonarLintCorePlugin.getDefault().removeLogListener(this);
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    manager.removeConsoles(new IConsole[] {this});
  }

  private IOConsoleOutputStream getInfoStream() {
    return infoStream;
  }

  private IOConsoleOutputStream getWarnStream() {
    return warnStream;
  }

  public IOConsoleOutputStream getDebugStream() {
    return debugStream;
  }

  private static String getShowConsolePreference() {
    return Platform.getPreferencesService().getString(SonarLintUiPlugin.PLUGIN_ID, P_SHOW_CONSOLE, P_SHOW_CONSOLE_ON_OUTPUT, null);
  }

  private static boolean isShowConsoleOnOutput() {
    return StringUtils.equals(getShowConsolePreference(), P_SHOW_CONSOLE_ON_OUTPUT);
  }

  private static boolean isShowConsoleOnError() {
    return StringUtils.equals(getShowConsolePreference(), P_SHOW_CONSOLE_ON_ERROR);
  }

  public static boolean isDebugEnabled() {
    return SonarLintUiPlugin.getDefault().getPreferenceStore().getBoolean(SonarLintConsole.P_DEBUG_OUTPUT);
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    // font changed
    setFont(JFaceResources.getFontRegistry().get("pref_console_font")); //$NON-NLS-1$
  }

}
