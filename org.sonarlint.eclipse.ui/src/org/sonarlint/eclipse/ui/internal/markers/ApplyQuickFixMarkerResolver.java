/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFix;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerTextEdit;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.actions.JobUtils;
import org.sonarlint.eclipse.ui.internal.util.LocationsUtils;

public class ApplyQuickFixMarkerResolver extends SortableMarkerResolver {

  private final MarkerQuickFix fix;

  public ApplyQuickFixMarkerResolver(MarkerQuickFix fix, int relevance) {
    super(relevance);
    this.fix = fix;
  }

  @Override
  public String getDescription() {
    return "Automatically modifies the code to fix the issue";
  }

  @Override
  public String getLabel() {
    return fix.getMessage();
  }

  @Override
  public void run(IMarker marker) {
    ISonarLintFile file = Adapters.adapt(marker.getResource(), ISonarLintFile.class);
    if (file == null) {
      return;
    }
    ITextEditor openEditor = openEditor(file, marker);
    Display.getDefault().asyncExec(() -> {
      if (fix.isValid()) {
        IDocument document = applyIn(openEditor, fix);
        SonarLintCorePlugin.getTelemetry().addQuickFixAppliedForRule(MarkerUtils.getRuleKey(marker).toString());
        scheduleAnalysis(new FileWithDocument(file, document));
      } else {
        SonarLintLogger.get().debug("Quick fix is not valid anymore");
      }
    });
  }

  private static void scheduleAnalysis(FileWithDocument fileWithDoc) {
    ISonarLintFile file = fileWithDoc.getFile();
    AnalyzeProjectRequest request = new AnalyzeProjectRequest(file.getProject(), Arrays.asList(fileWithDoc), TriggerType.QUICK_FIX);
    JobUtils.scheduleAutoAnalysisIfEnabled(request);
  }

  @Nullable
  private static ITextEditor openEditor(ISonarLintFile file, IMarker marker) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    IEditorPart textEditor;
    try {
      textEditor = IDE.openEditor(page, marker);
    } catch (PartInitException e) {
      SonarLintLogger.get().error("Unable to open the text editor");
      return null;
    }
    if (!(textEditor instanceof ITextEditor)) {
      SonarLintLogger.get().error("Unable to open a text editor");
      return null;
    } else {
      // After opening editor with IDE.openEditor, the marker position is selected.
      // Clear the selection
      ITextViewer viewer = textEditor.getAdapter(ITextViewer.class);
      if (viewer != null) {
        viewer.setSelectedRange(viewer.getSelectedRange().x, 0);
      }
      return (ITextEditor) textEditor;
    }
  }

  private static IDocument applyIn(ITextEditor openEditor, MarkerQuickFix fix) {
    IDocument document = openEditor.getDocumentProvider().getDocument(openEditor.getEditorInput());
    // IDocumentExtension4 appeared before oldest supported version, no need to check
    IDocumentExtension4 extendedDocument = (IDocumentExtension4) document;
    DocumentRewriteSession session = null;
    try {
      session = extendedDocument.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
      List<MarkerTextEdit> textEdits = fix.getTextEdits();
      // We only want to select edited location when there is only one text edit
      boolean selectUpdatedText = textEdits.size() == 1;
      textEdits.forEach(textEdit -> apply(openEditor, extendedDocument, textEdit, selectUpdatedText));
    } catch (Exception e) {
      SonarLintLogger.get().error("Cannot apply the quick fix", e);
    } finally {
      if (session != null) {
        extendedDocument.stopRewriteSession(session);
      }
    }
    return document;
  }

  private static void apply(ITextEditor textEditor, IDocumentExtension4 document, MarkerTextEdit textEdit, boolean selectUpdatedText) {
    IMarker editMarker = textEdit.getMarker();
    try {
      if (editMarker.exists()) {
        int start = MarkerUtilities.getCharStart(editMarker);
        int end = MarkerUtilities.getCharEnd(editMarker);
        // look up the current range of the marker when the document has been edited
        Position markerPosition = LocationsUtils.getMarkerPosition(editMarker, textEditor);
        if (markerPosition != null) {
          if (markerPosition.isDeleted()) {
            // do nothing if position has been deleted
            SonarLintLogger.get().debug("One quick fix edit position is not valid anymore");
            return;
          } else {
            // use position instead of marker values
            start = markerPosition.getOffset();
            end = markerPosition.getOffset() + markerPosition.getLength();
          }
        }
        document.replace(start, end - start, textEdit.getNewText(), document.getModificationStamp() + 1);
        ITextViewer viewer = textEditor.getAdapter(ITextViewer.class);
        if (viewer != null) {
          viewer.setSelectedRange(start, selectUpdatedText ? textEdit.getNewText().length() : 0);
          viewer.revealRange(start, textEdit.getNewText().length());
        }
      }
    } catch (Exception e) {
      SonarLintLogger.get().error("Quick fix location does not exist", e);
    }
  }

  @Override
  public Image getImage() {
    return SonarLintImages.RESOLUTION_QUICKFIX_CHANGE;
  }
}
