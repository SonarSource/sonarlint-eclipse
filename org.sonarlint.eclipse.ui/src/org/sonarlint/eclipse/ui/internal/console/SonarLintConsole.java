/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.console;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class SonarLintConsole extends MessageConsole implements IPropertyChangeListener {

  public static final String P_VERBOSE_OUTPUT = "debugOutput"; //$NON-NLS-1$
  public static final String P_ANALYZER_OUTPUT = "showAnalyzerOutput"; //$NON-NLS-1$
  public static final String P_IDE_TRACING_OUTPUT = "ideSpecificTracing"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE = "showConsole"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE_NEVER = "never"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE_ON_OUTPUT = "onOutput"; //$NON-NLS-1$
  public static final String P_SHOW_CONSOLE_ON_ERROR = "onError"; //$NON-NLS-1$

  public static final String TITLE = Messages.SonarConsole_title;

  private final MessageConsoleStream infoStream;
  private final MessageConsoleStream warnStream;
  private final MessageConsoleStream debugStream;
  private final MessageConsoleStream traceStream;

  public SonarLintConsole(ImageDescriptor imageDescriptor) {
    super(TITLE, imageDescriptor);
    this.infoStream = newMessageStream();
    this.warnStream = newMessageStream();
    this.debugStream = newMessageStream();
    this.traceStream = newMessageStream();
  }

  @Override
  protected void init() {
    super.init();

    JFaceResources.getFontRegistry().addListener(SonarLintConsole.this);

    var display = Display.getDefault();

    var colorRegistry = JFaceResources.getColorRegistry();

    var linkColor = colorRegistry.get(JFacePreferences.HYPERLINK_COLOR);
    if (linkColor == null) {
      linkColor = JFaceColors.getHyperlinkText(display);
    }

    var errorColorColor = colorRegistry.get(JFacePreferences.ERROR_COLOR);
    if (errorColorColor == null) {
      errorColorColor = JFaceColors.getErrorText(display);
    }

    var activeLinkColor = colorRegistry.get(JFacePreferences.ACTIVE_HYPERLINK_COLOR);
    if (activeLinkColor == null) {
      activeLinkColor = JFaceColors.getActiveHyperlinkText(display);
    }

    getWarnStream().setColor(errorColorColor);
    getDebugStream().setColor(linkColor);
    getTraceStream().setColor(activeLinkColor);
  }

  public void bringConsoleToFront() {
    if (PlatformUI.isWorkbenchRunning()) {
      var manager = ConsolePlugin.getDefault().getConsoleManager();
      if (!isVisible()) {
        manager.addConsoles(new IConsole[] {this});
      }
      manager.showConsoleView(this);
    }
  }

  private static boolean isVisible() {
    var conMan = ConsolePlugin.getDefault().getConsoleManager();
    var existing = conMan.getConsoles();
    for (IConsole element : existing) {
      if (SonarLintConsole.TITLE.equals(element.getName())) {
        return true;
      }
    }
    return false;
  }

  public void info(String msg, boolean fromAnalyzer) {
    if (showAnalysisLogs() || !fromAnalyzer) {
      if (isShowConsoleOnOutput()) {
        bringConsoleToFront();
      }
      write(getInfoStream(), msg);
    }
  }

  public void error(String msg, boolean fromAnalyzer) {
    if (showAnalysisLogs() || !fromAnalyzer) {
      if (isShowConsoleOnOutput() || isShowConsoleOnError()) {
        bringConsoleToFront();
      }
      write(getWarnStream(), msg);
    }
  }

  public void error(String msg, Throwable t, boolean fromAnalyzer) {
    if (showAnalysisLogs() || !fromAnalyzer) {
      if (isShowConsoleOnOutput() || isShowConsoleOnError()) {
        bringConsoleToFront();
      }

      var stack = new StringWriter();
      t.printStackTrace(new PrintWriter(stack));

      write(getWarnStream(), msg);
      write(getWarnStream(), stack.toString());
    }
  }

  public void debug(String msg, boolean fromAnalyzer) {
    if (isVerboseEnabled() && (showAnalysisLogs() || !fromAnalyzer)) {
      if (isShowConsoleOnOutput()) {
        bringConsoleToFront();
      }
      write(getDebugStream(), msg);
    }
  }

  public void debug(String msg, Throwable t, boolean fromAnalyzer) {
    var stack = new StringWriter();
    t.printStackTrace(new PrintWriter(stack));
    debug(msg, fromAnalyzer);
    debug(stack.toString(), fromAnalyzer);
  }

  public void traceIdeMessage(String msg) {
    if (showIdeSpecificTracing()) {
      if (isShowConsoleOnOutput()) {
        bringConsoleToFront();
      }
      write(getTraceStream(), msg);
    }
  }

  public void traceIdeMessage(String msg, Throwable t) {
    var stack = new StringWriter();
    t.printStackTrace(new PrintWriter(stack));
    traceIdeMessage(msg);
    traceIdeMessage(stack.toString());
  }

  private static void write(MessageConsoleStream stream, String msg) {
    if (msg == null) {
      return;
    }
    stream.println(msg);
  }

  private MessageConsoleStream getInfoStream() {
    return infoStream;
  }

  private MessageConsoleStream getWarnStream() {
    return warnStream;
  }

  public MessageConsoleStream getDebugStream() {
    return debugStream;
  }

  public MessageConsoleStream getTraceStream() {
    return traceStream;
  }

  private static String getShowConsolePreference() {
    return SonarLintUiPlugin.getDefault().getPreferenceStore().getString(SonarLintConsole.P_SHOW_CONSOLE);
  }

  private static boolean isShowConsoleOnOutput() {
    return P_SHOW_CONSOLE_ON_OUTPUT.equals(getShowConsolePreference());
  }

  private static boolean isShowConsoleOnError() {
    return P_SHOW_CONSOLE_ON_ERROR.equals(getShowConsolePreference());
  }

  public static boolean isVerboseEnabled() {
    return SonarLintUiPlugin.getDefault().getPreferenceStore().getBoolean(SonarLintConsole.P_VERBOSE_OUTPUT);
  }

  public static boolean showAnalysisLogs() {
    return SonarLintUiPlugin.getDefault().getPreferenceStore().getBoolean(SonarLintConsole.P_ANALYZER_OUTPUT);
  }

  public static boolean showIdeSpecificTracing() {
    return SonarLintUiPlugin.getDefault().getPreferenceStore().getBoolean(SonarLintConsole.P_IDE_TRACING_OUTPUT);
  }

  @Override
  protected void dispose() {
    super.dispose();
    JFaceResources.getFontRegistry().removeListener(SonarLintConsole.this);
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    // refresh font to react to font size change
    setFont(null);
  }

}
