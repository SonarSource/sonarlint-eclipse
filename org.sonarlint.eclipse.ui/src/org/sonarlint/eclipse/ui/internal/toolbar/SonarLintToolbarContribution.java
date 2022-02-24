/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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

package org.sonarlint.eclipse.ui.internal.toolbar;

import java.util.Optional;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.vcs.VcsService;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.SelectionUtils;

import static org.sonarlint.eclipse.ui.internal.util.PlatformUtils.doIfSonarLintFileInEditor;

public class SonarLintToolbarContribution extends WorkbenchWindowControlContribution implements ISelectionListener, IPartListener2 {

  private Label label;

  @Override
  protected Control createControl(Composite parent) {
    var page = new Composite(parent, SWT.NONE);

    var gridLayout = new GridLayout(1, false);
    gridLayout.marginHeight = 3;
    page.setLayout(gridLayout);

    label = new Label(page, SWT.NONE);
    label.setImage(SonarLintImages.SONARLINT_ICON_IMG);

    var activePart = getWorkbenchWindow().getActivePage().getActivePart();
    doIfSonarLintFileInEditor(activePart, (f, p) -> slFileSelected(f));
    getWorkbenchWindow().getSelectionService().addSelectionListener(this);
    getWorkbenchWindow().getActivePage().addPartListener(this);
    return label;
  }

  @Override
  public void dispose() {
    getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
    getWorkbenchWindow().getActivePage().removePartListener(this);
    super.dispose();
  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    var element = SelectionUtils.getSingleElement(selection);
    if (element != null) {
      var slFile = Adapters.adapt(element, ISonarLintFile.class);
      if (slFile != null) {
        slFileSelected(slFile);
        return;
      }
    }
    IEditorPart activeEditor = part.getSite().getPage().getActiveEditor();
    if (activeEditor == null) {
      return;
    }
    var editorFile = Adapters.adapt(activeEditor.getEditorInput(), IFile.class);
    if (editorFile != null) {
      var editorSlFile = Adapters.adapt(editorFile, ISonarLintFile.class);
      slFileSelected(editorSlFile);
    }
  }

  private void slFileSelected(ISonarLintFile slFile) {
    ISonarLintProject project = slFile.getProject();
    Optional<ResolvedBinding> binding = SonarLintCorePlugin.getServersManager().resolveBinding(project);
    if (binding.isEmpty()) {
      label.setToolTipText("");
      return;
    }
    var serverBranch = VcsService.getServerBranch(project);
    String toolTipText;
    if (serverBranch != null) {
      toolTipText = slFile.getResource().getName() + " - Branch: " + serverBranch;
    } else {
      toolTipText = "Can not get server branch.";
    }
    label.setToolTipText(toolTipText);
  }

  @Override
  public void partActivated(IWorkbenchPartReference partRef) {
    doIfSonarLintFileInEditor(partRef, (f, p) -> slFileSelected(f));
  }

}
