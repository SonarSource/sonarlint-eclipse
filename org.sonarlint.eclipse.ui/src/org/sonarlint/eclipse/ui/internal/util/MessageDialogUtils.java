/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.util;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.telemetry.LinkTelemetry;

/** When we want to display simple dialogs we can use the JFace MessageDialog */
public class MessageDialogUtils {
  private static final String OPEN_IN_IDE_TITLE = "Open in IDE";

  private MessageDialogUtils() {
    // utility class
  }

  /** For the "Open in IDE" feature we want to display an information */
  public static void openInIdeInformation(String message) {
    Display.getDefault().syncExec(() -> MessageDialog.openInformation(
      PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), OPEN_IN_IDE_TITLE, message));
  }

  /** For the "Open in IDE" feature we want to display an error message */
  public static void openInIdeError(String message) {
    Display.getDefault().syncExec(() -> MessageDialog.openError(
      PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), OPEN_IN_IDE_TITLE, message));
  }

  /** For the "Open in IDE" feature we want to display a yes/no question for the user to answer */
  public static void openInIdeQuestion(String message, Runnable yesHandler) {
    Display.getDefault().asyncExec(() -> {
      var result = MessageDialog.openQuestion(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
        OPEN_IN_IDE_TITLE, message);
      if (result) {
        yesHandler.run();
      }
    });
  }

  /** When not in UI thread, run it in UI thread */
  public static void enhancedWithConnectedModeInformation(String title, String message) {
    Display.getDefault().asyncExec(() -> enhancedWithConnectedModeInformation(
      PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), title, message));
  }

  /** For notifying about features enhances with connected mode we want to display some information */
  public static void enhancedWithConnectedModeInformation(Shell shell, String title, String message) {
    var result = new MessageDialog(shell, title, null,
      message, MessageDialog.INFORMATION,
      new String[] {"Learn more", "Try SonarCloud for free", "Don't ask again"}, 0).open();

    // The result corresponds to the index in the array; totally confusing as the pre-selected button (in our case
    // "Learn more") is always the rightmost one.
    // INFO: When just closing the dialog, result will be greater than the array size, funny :D
    if (result == 0) {
      BrowserUtils.openExternalBrowserWithTelemetry(LinkTelemetry.CONNECTED_MODE_DOCS, shell.getDisplay());
    } else if (result == 1) {
      BrowserUtils.openExternalBrowserWithTelemetry(LinkTelemetry.SONARCLOUD_SIGNUP_PAGE, shell.getDisplay());
    } else if (result == 2) {
      SonarLintGlobalConfiguration.setIgnoreEnhancedFeatureNotifications();
    }
  }

  public static void connectedModeOnlyInformation(String title, String message, LinkTelemetry learnMoreLink) {
    Display.getDefault().asyncExec(() -> connectedModeOnlyInformation(
      PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), title, message, learnMoreLink));
  }

  /** For notifying about features only in connected mode, link to the specific information */
  public static void connectedModeOnlyInformation(Shell shell, String title, String message, LinkTelemetry learnMoreLink) {
    var result = new MessageDialog(shell, title, null,
      message, MessageDialog.INFORMATION,
      new String[] {"Learn more", "Close"}, 0).open();

    if (result == 0) {
      BrowserUtils.openExternalBrowserWithTelemetry(learnMoreLink, shell.getDisplay());
    }
  }
}
