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
package org.sonarlint.eclipse.ui.internal;

import java.util.List;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;

import static org.sonarlint.eclipse.ui.internal.util.PlatformUtils.doIfSonarLintFileInEditor;

/**
 * Responsible to trigger analysis when editor are opened
 */
public class OpenEditorAnalysisTrigger implements IPartListener2 {
  @Override
  public void partOpened(IWorkbenchPartReference partRef) {
    doIfSonarLintFileInEditor(partRef, (f, p) -> scheduleUpdate(p, f));
  }

  private static void scheduleUpdate(IEditorPart editorPart, ISonarLintFile sonarLintFile) {
    if (editorPart instanceof ITextEditor) {
      var doc = ((ITextEditor) editorPart).getDocumentProvider().getDocument(editorPart.getEditorInput());
      scheduleUpdate(new FileWithDocument(sonarLintFile, doc));
    } else {
      scheduleUpdate(new FileWithDocument(sonarLintFile, null));
    }
  }

  private static void scheduleUpdate(FileWithDocument fileWithDoc) {
    var file = fileWithDoc.getFile();
    var project = file.getProject();
    if (!SonarLintCorePlugin.loadConfig(project).isAutoEnabled()) {
      return;
    }

    var request = new AnalyzeProjectRequest(project, List.of(fileWithDoc), TriggerType.EDITOR_OPEN, false);
    AnalysisJobsScheduler.scheduleAutoAnalysisIfEnabled(request);
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
    // Nothing to do
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
