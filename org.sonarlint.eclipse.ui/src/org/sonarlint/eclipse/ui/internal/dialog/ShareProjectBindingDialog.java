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
package org.sonarlint.eclipse.ui.internal.dialog;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/**
 *  Shown when user select a project to share its Connected Mode configuration. This shows different information based
 *  on what kind of project is selected for sharing.
 */
public class ShareProjectBindingDialog extends MessageDialog {
  private static final String TITLE = "Share Connected Mode configuration?";
  private static final String MESSAGE_BASE = "A configuration file will be created in the project directory, making "
    + "it easier for other team members to configure the binding for the same project.";
  private static final String MESSAGE_USE_ROOT_PROJECT = "\nWhen choosing to save on the Root Project level, this will "
    + "affect it and all its sub-projects.";
  private static final String MESSAGE_USE_THIS_PROJECT_NOT_ROOT = "\nWhen choosing to save on this Project level, this "
    + "will only affect it, but no other projects.";
  private static final String MESSAGE_USE_THIS_PROJECT_ROOT = "\nWhen choosing to save on this Project level, this "
    + "will affect it and all its sub-projects.";

  public ShareProjectBindingDialog(Shell parentShell, boolean isRootProject, boolean hasRootProject) {
    super(parentShell, TITLE, null, getMessage(isRootProject, hasRootProject), QUESTION,
      getButtonLabels(hasRootProject), 0);
  }

  private static String getMessage(boolean isRootProject, boolean hasRootProject) {
    if (isRootProject) {
      return MESSAGE_BASE + MESSAGE_USE_THIS_PROJECT_ROOT;
    } else if (hasRootProject) {
      return MESSAGE_BASE + MESSAGE_USE_ROOT_PROJECT + MESSAGE_USE_THIS_PROJECT_NOT_ROOT;
    }
    return MESSAGE_BASE;
  }

  private static String[] getButtonLabels(boolean hasRootProject) {
    return hasRootProject
      ? new String[] {"Save to Root Project", "Save to Project", "Learn more"}
      : new String[] {"Save to Project", "Learn more"};
  }
}
