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
package org.sonarlint.eclipse.ui.internal.binding.actions;

import java.util.Collections;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingWizard;

public class ConnectionBindProjectsAction extends AbstractConnectionAction {

  public ConnectionBindProjectsAction(Shell shell, ISelectionProvider selectionProvider) {
    super("Bind Projects...", SonarLintImages.SYNCED_IMG, shell, selectionProvider);
  }

  @Override
  protected void doRun(Shell shell, ConnectionFacade selectedConnection) {
    final var dialog = ProjectBindingWizard.createDialogSkipConnectionSelection(shell, Collections.emptyList(), selectedConnection);
    dialog.open();
  }

}
