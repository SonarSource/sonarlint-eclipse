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
package org.sonarlint.eclipse.ui.internal.dialog;

import java.util.List;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 *  When more than one project hierarchy is found and the user has to choose the one to use, this is displayed. As
 *  different providers might be named the same we will disable duplicates and also only offer the user to choose one
 *  provider at the time.
 */
public class ProjectHierarchySelectionDialog extends ElementListSelectionDialog {
  public ProjectHierarchySelectionDialog(Shell shell, List<String> hierarchies) {
    super(shell, new LabelProvider());
    setTitle("Choose hierarchical project provider to use:");
    setAllowDuplicates(false);
    setMultipleSelection(false);
    setElements(hierarchies.toArray());
  }
}
