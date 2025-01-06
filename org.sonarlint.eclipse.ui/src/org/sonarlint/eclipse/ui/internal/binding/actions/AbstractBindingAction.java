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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/** Every action corresponding to the binding of a project on the BindingsView should inherit from this one! */
public abstract class AbstractBindingAction extends SelectionProviderAction {
  protected final Shell shell;
  protected List<ISonarLintProject> selectedProjects;

  protected AbstractBindingAction(Shell shell, ISelectionProvider provider, String text, ImageDescriptor image) {
    super(provider, text);
    setImageDescriptor(image);
    this.shell = shell;
  }

  @Override
  public void selectionChanged(IStructuredSelection sel) {
    if (disable(sel)) {
      setEnabled(false);
      return;
    }
    selectedProjects = new ArrayList<>();
    var enabled = false;
    var iterator = sel.iterator();
    while (iterator.hasNext()) {
      var obj = iterator.next();
      if (obj instanceof ISonarLintProject) {
        var project = (ISonarLintProject) obj;
        selectedProjects.add(project);
        enabled = true;
      } else {
        setEnabled(false);
        return;
      }
    }
    setEnabled(enabled);
  }

  @Override
  public final void run() {
    // It is possible that the project is created and added to the connection view on workbench
    // startup. As a result, when the user switches to the connection view, the project is
    // selected, but the selectionChanged event is not called, which results in selectedProjects
    // being null. When selectedProjects is null the project will not be processed
    //
    // To handle the case where selectedProjects is null, the selectionChanged method is called
    // to ensure selectedProjects will be populated.
    if (selectedProjects == null) {
      var sel = getStructuredSelection();
      if (sel != null) {
        selectionChanged(sel);
      }
    }

    if (selectedProjects != null) {
      doRun();
    }
  }

  /** Used to check if the action should be disabled based on the selection */
  protected abstract boolean disable(IStructuredSelection sel);

  /** Runs the actual actions after the checks succeeded */
  protected abstract void doRun();
}
