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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
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
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarImages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.core.SonarLogger;
import org.sonar.ide.eclipse.preferences.PreferenceConstants;
import org.sonar.ide.ui.ISonarConsole;

import java.io.IOException;

/**
 * 
 * Console that shows the communications with sonar servers.
 * 
 * @author Jérémie Lagarde
 */
public class SonarConsole extends IOConsole implements ISonarConsole, IPropertyChangeListener {

  public static final String SONAR_CONSOLE_TYPE = "SONAR"; //$NON-NLS-1$
  private ConsoleDocument document = null;
  private boolean visible = false;
  private boolean initialized = false;

  // created colors for each line type - must be disposed at shutdown
  private Color requestColor;
  private Color responseColor;
  private Color errorColor;

  // streams for each command type - each stream has its own color
  private IOConsoleOutputStream requestStream;
  private IOConsoleOutputStream responseStream;
  private IOConsoleOutputStream errorStream;

  // preferences for showing the Sonar console when communication with sonar
  // server is provided
  private boolean showOnError;
  private boolean showOnMessage;

  public SonarConsole() {
    super(Messages.getString("console.view.title"), SONAR_CONSOLE_TYPE, SonarImages.getImageDescriptor(SonarImages.IMG_SONARCONSOLE)); //$NON-NLS-1$
    document = new ConsoleDocument();
    SonarPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
  }

  @Override
  protected void init() {
    super.init();
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        JFaceResources.getFontRegistry().addListener(SonarConsole.this);
        initializeStreams();
        dumpConsole();
      }
    });
  }

  private void initializeStreams() {
    synchronized (document) {
      if ( !initialized) {
        requestStream = newOutputStream();
        responseStream = newOutputStream();
        errorStream = newOutputStream();
        // install colors
        requestColor = createColor(Display.getDefault(), PreferenceConstants.P_CONSOLE_REQUEST_COLOR);
        requestStream.setColor(requestColor);
        responseColor = createColor(Display.getDefault(), PreferenceConstants.P_CONSOLE_RESPONSE_COLOR);
        responseStream.setColor(responseColor);
        errorColor = createColor(Display.getDefault(), PreferenceConstants.P_CONSOLE_ERROR_COLOR);
        errorStream.setColor(errorColor);
        // install font
        setFont(JFaceResources.getFontRegistry().get("pref_console_font"));
        initialized = true;
      }
    }
  }

  private void initLimitOutput() {
    IPreferenceStore store = SonarPlugin.getDefault().getPreferenceStore();
    if (store.getBoolean(PreferenceConstants.P_CONSOLE_LIMIT_OUTPUT)) {
      setWaterMarks(1000, store.getInt(PreferenceConstants.P_CONSOLE_HIGH_WATER_MARK));
    } else {
      setWaterMarks( -1, 0);
    }
  }

  private void dumpConsole() {
    synchronized (document) {
      visible = true;
      ConsoleDocument.ConsoleLine[] lines = document.getLines();
      for (int i = 0; i < lines.length; i++) {
        ConsoleDocument.ConsoleLine line = lines[i];
        appendLine(line.type, line.line);
      }
      document.clear();
    }
  }

  private void appendLine(int type, String line) {
    synchronized (document) {
      if (visible) {
        try {
          switch (type) {
            case ConsoleDocument.REQUEST:
              requestStream.write(line);
              requestStream.write('\n');
              break;
            case ConsoleDocument.RESPONSE:
              responseStream.write(" ==> " + line); //$NON-NLS-1$
              responseStream.write('\n');
              break;
            case ConsoleDocument.ERROR:
              errorStream.write(" [E] " + line); //$NON-NLS-1$
              errorStream.write('\n');
              break;
          }
        } catch (IOException e) {
          SonarLogger.log(e);
        }
      } else {
        document.appendConsoleLine(type, line);
      }
    }
  }

  private void bringConsoleToFront() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    if ( !visible) {
      manager.addConsoles(new IConsole[] { this });
    }
    manager.showConsoleView(this);
  }

  @Override
  protected void dispose() {
    // Here we can't call super.dispose() because we actually want the
    // partitioner to remain
    // connected, but we won't show lines until the console is added to the
    // console manager
    // again.
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        visible = false;
        JFaceResources.getFontRegistry().removeListener(SonarConsole.this);
      }
    });
  }

  /**
   * Clean-up created fonts.
   */
  public void shutdown() {
    // Call super dispose because we want the partitioner to be
    // disconnected.
    super.dispose();
    if (requestColor != null) {
      requestColor.dispose();
    }
    if (responseColor != null) {
      responseColor.dispose();
    }
    if (errorColor != null) {
      errorColor.dispose();
    }
    SonarPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
  }

  public void propertyChange(PropertyChangeEvent event) {
    String property = event.getProperty();
    // colors
    if (visible) {
      if (property.equals(PreferenceConstants.P_CONSOLE_REQUEST_COLOR)) {
        Color newColor = createColor(Display.getDefault(), PreferenceConstants.P_CONSOLE_REQUEST_COLOR);
        requestStream.setColor(newColor);
        requestColor.dispose();
        requestColor = newColor;
      } else if (property.equals(PreferenceConstants.P_CONSOLE_RESPONSE_COLOR)) {
        Color newColor = createColor(Display.getDefault(), PreferenceConstants.P_CONSOLE_RESPONSE_COLOR);
        responseStream.setColor(newColor);
        responseColor.dispose();
        responseColor = newColor;
      } else if (property.equals(PreferenceConstants.P_CONSOLE_ERROR_COLOR)) {
        Color newColor = createColor(Display.getDefault(), PreferenceConstants.P_CONSOLE_ERROR_COLOR);
        errorStream.setColor(newColor);
        errorColor.dispose();
        errorColor = newColor;
      } else if (property.equals(PreferenceConstants.P_CONSOLE_LIMIT_OUTPUT)) {
        initLimitOutput();
      }
    }
    // show preferences
    if (property.equals(PreferenceConstants.P_CONSOLE_SHOW_ON_MESSAGE)) {
      Object value = event.getNewValue();
      if (value instanceof String) {
        showOnMessage = Boolean.getBoolean((String) event.getNewValue());
      } else {
        showOnMessage = ((Boolean) value).booleanValue();
      }
    }

    // Show on error
    if (property.equals(PreferenceConstants.P_CONSOLE_SHOW_ON_ERROR)) {
      Object value = event.getNewValue();
      if (value instanceof String) {
        showOnError = Boolean.getBoolean((String) event.getNewValue());
      } else {
        showOnError = ((Boolean) value).booleanValue();
      }
    }
  }

  /**
   * Returns a color instance based on data from a preference field.
   */
  private Color createColor(Display display, String preference) {
    RGB rgb = PreferenceConverter.getColor(SonarPlugin.getDefault().getPreferenceStore(), preference);
    return new Color(display, rgb);
  }

  public void logError(String error) {
    if (showOnError) {
      bringConsoleToFront();
    }
    appendLine(ConsoleDocument.ERROR, error);
  }

  public void logError(String error, Throwable ex) {
    if (showOnError) {
      bringConsoleToFront();
    }
    appendLine(ConsoleDocument.ERROR, error);
    for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
      appendLine(ConsoleDocument.ERROR, stackTraceElement.toString());
    }
  }

  public void logRequest(String request) {
    if (showOnMessage) {
      bringConsoleToFront();
    }
    appendLine(ConsoleDocument.REQUEST, request);
  }

  public void logResponse(String response) {
    if (showOnMessage) {
      bringConsoleToFront();
    }
    appendLine(ConsoleDocument.RESPONSE, response);
  }

}
