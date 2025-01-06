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

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.job.ShareProjectBindingJob;

/** Action corresponding to the context menu option to change a project binding */
public class ProjectShareBindingAction extends AbstractBindingAction {
  public ProjectShareBindingAction(Shell shell, ISelectionProvider selectionProvider) {
    super(shell, selectionProvider, Messages.actionShareBinding, SonarLintImages.SHARE_IMG);
  }

  @Override
  protected boolean disable(IStructuredSelection sel) {
    // Currently only single selection is supported, if at some point it should be possible to share the configuration
    // for multiple, non-related projects simultaneously, it also has to be changed for the ShareBindingCommand in the
    // UI plugin.xml ('<count value="1" />' to '<count value="=" />')!
    return sel.isEmpty() || sel.size() > 1;
  }

  @Override
  protected void doRun() {
    var job = new ShareProjectBindingJob(shell, selectedProjects.get(0));
    job.schedule();
  }
}
