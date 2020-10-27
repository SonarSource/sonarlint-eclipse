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
package org.sonarlint.eclipse.ui.internal;

import java.util.Arrays;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.ui.internal.binding.actions.JobUtils;

/**
 * Responsible to trigger analysis when editor are opened
 */
public class OpenEditorAnalysisTrigger implements IPartListener2 {
  @Override
  public void partOpened(IWorkbenchPartReference partRef) {
    IWorkbenchPart part = partRef.getPart(true);
    if (part instanceof IEditorPart) {
      IEditorPart editorPart = (IEditorPart) part;
      IEditorInput input = ((IEditorPart) part).getEditorInput();
      if (input instanceof IFileEditorInput) {
        IFile file = ((IFileEditorInput) input).getFile();
        ISonarLintFile sonarLintFile = Adapters.adapt(file, ISonarLintFile.class);
        if (sonarLintFile != null) {
          scheduleUpdate(editorPart, sonarLintFile);
        }
      }
    }
  }

  private static void scheduleUpdate(IEditorPart editorPart, ISonarLintFile sonarLintFile) {
    if (editorPart instanceof ITextEditor) {
      IDocument doc = ((ITextEditor) editorPart).getDocumentProvider().getDocument(editorPart.getEditorInput());
      scheduleUpdate(new FileWithDocument(sonarLintFile, doc));
    } else {
      scheduleUpdate(new FileWithDocument(sonarLintFile, null));
    }
  }

  private static void scheduleUpdate(FileWithDocument fileWithDoc) {
    ISonarLintFile file = fileWithDoc.getFile();
    if (!SonarLintCorePlugin.loadConfig(file.getProject()).isAutoEnabled()) {
      return;
    }
    AnalyzeProjectRequest request = new AnalyzeProjectRequest(file.getProject(), Arrays.asList(fileWithDoc), TriggerType.EDITOR_OPEN);
    JobUtils.scheduleAutoAnalysisIfEnabled(request);
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
