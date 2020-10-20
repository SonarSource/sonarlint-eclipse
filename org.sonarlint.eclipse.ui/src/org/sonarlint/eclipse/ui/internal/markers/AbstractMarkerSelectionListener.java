/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.sonarlint.eclipse.ui.internal.util.SelectionUtils;

public interface AbstractMarkerSelectionListener extends ISelectionListener {

  @Override
  default void selectionChanged(IWorkbenchPart part, ISelection selection) {
    IMarker selectedMarker = SelectionUtils.findSelectedSonarLintMarker(selection);
    if (selectedMarker != null) {
      sonarlintIssueMarkerSelected(selectedMarker);
    }
  }

  void sonarlintIssueMarkerSelected(IMarker selectedMarker);

  default void startListeningForSelectionChanges(IWorkbenchPage page) {
    page.addPostSelectionListener(this);
  }

  default void stopListeningForSelectionChanges(IWorkbenchPage page) {
    page.removePostSelectionListener(this);
  }

}
