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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.vcs.VcsService;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.views.issues.OnTheFlyIssuesView;

import static org.sonarlint.eclipse.ui.internal.util.PlatformUtils.doIfSonarLintFileInEditor;

public class SonarLintToolbarContribution extends WorkbenchWindowControlContribution {

  private Label label;

  @Override
  protected Control createControl(Composite parent) {

    var page = new Composite(parent, SWT.NONE);

    var gridLayout = new GridLayout(1, false);
    gridLayout.marginHeight = 3;
    page.setLayout(gridLayout);

    label = new Label(page, SWT.NONE);
    label.setImage(SonarLintImages.SONARLINT_ICON_IMG);
    label.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDown(MouseEvent event) {
        if (event.button == 1) {
          try {
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(OnTheFlyIssuesView.ID);
          } catch (PartInitException e) {
            SonarLintLogger.get().error("Unable to open On-The-Fly View", e);
          }
        }
      }
    });

    label.addMouseTrackListener(new MouseTrackAdapter() {
      @Override
      public void mouseEnter(MouseEvent e) {
        updateTooltip();
      }
    });
    return label;
  }

  private void updateTooltip() {
    var activePart = getWorkbenchWindow().getActivePage().getActiveEditor();
    if (activePart == null) {
      refreshLabel(null);
      return;
    }
    doIfSonarLintFileInEditor(activePart, (f, p) -> refreshLabel(f));
  }

  private void refreshLabel(@Nullable ISonarLintFile f) {
    if (f != null) {
      new UpdateSonarLintToolbarJob(f).schedule();
    } else {
      updateLabelTooltipInUIThread("Open a file to analyze it");
    }
  }

  private class UpdateSonarLintToolbarJob extends Job {

    private final ISonarLintFile slFile;

    public UpdateSonarLintToolbarJob(ISonarLintFile openedFileInEditor) {
      super("Refresh SonarLint toolbar for " + openedFileInEditor.getName());
      setPriority(Job.DECORATE);
      setSystem(true);
      this.slFile = openedFileInEditor;
    }

    @Override
    public IStatus run(IProgressMonitor monitor) {
      ISonarLintProject project = slFile.getProject();
      Optional<ResolvedBinding> binding = SonarLintCorePlugin.getServersManager().resolveBinding(project);
      if (binding.isEmpty()) {
        updateLabelTooltipInUIThread(slFile.getResource().getName());
      } else {
        var serverBranch = VcsService.getServerBranch(project);
        updateLabelTooltipInUIThread(slFile.getResource().getName() + " - Branch: " + Optional.ofNullable(serverBranch).orElse("<main>"));
      }
      return Status.OK_STATUS;
    }

  }

  private void updateLabelTooltipInUIThread(String text) {
    getWorkbenchWindow().getShell().getDisplay().syncExec(() -> label.setToolTipText("SonarLint - " + text));
  }

}
