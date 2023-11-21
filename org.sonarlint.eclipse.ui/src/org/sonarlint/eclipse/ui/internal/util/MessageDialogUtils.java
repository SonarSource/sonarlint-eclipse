/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
import org.eclipse.ui.PlatformUI;

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
}
