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
package org.sonarlint.eclipse.ui.internal;

import java.util.List;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintMarkerUpdater;
import org.sonarlint.eclipse.core.internal.jobs.TaintIssuesUpdateJob;

import static org.sonarlint.eclipse.ui.internal.util.PlatformUtils.doIfSonarLintFileInEditor;

public class TaintMarkersPartListener implements IPartListener2 {
  @Override
  public void partOpened(IWorkbenchPartReference partRef) {
    doIfSonarLintFileInEditor(partRef, (f, p) -> {
      var bindingOpt = SonarLintCorePlugin.getServersManager().resolveBinding(f.getProject());
      if (bindingOpt.isPresent()) {
        new TaintIssuesUpdateJob((ConnectedEngineFacade) bindingOpt.get().getEngineFacade(), f.getProject(), List.of(f), bindingOpt.get().getProjectBinding()).schedule();
      }
    });
  }

  @Override
  public void partVisible(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

  @Override
  public void partInputChanged(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

  @Override
  public void partHidden(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

  @Override
  public void partDeactivated(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

  @Override
  public void partClosed(IWorkbenchPartReference partRef) {
    doIfSonarLintFileInEditor(partRef, (f, p) -> SonarLintMarkerUpdater.deleteTaintMarkers(f));
  }

  @Override
  public void partBroughtToTop(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

  @Override
  public void partActivated(IWorkbenchPartReference partRef) {
    // Nothing to do
  }

}
