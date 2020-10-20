/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.codemining;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.AbstractCodeMiningProvider;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.ui.internal.markers.ShowIssueFlowsMarkerResolver;
import org.sonarlint.eclipse.ui.internal.util.LocationsUtils;

public class SonarLintCodeMiningProvider extends AbstractCodeMiningProvider {

  @Nullable
  private static IMarker currentMarker;
  private static int selectedFlow;

  @Override
  public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer, IProgressMonitor monitor) {
    IMarker markerToUse = currentMarker;
    if (markerToUse == null) {
      return null;
    }
    return CompletableFuture.supplyAsync(() -> {
      monitor.isCanceled();
      ITextEditor textEditor = super.getAdapter(ITextEditor.class);

      List<ICodeMining> minings = new ArrayList<>();

      IEditorInput editorInput = textEditor.getEditorInput();
      IDocument doc = textEditor.getDocumentProvider().getDocument(editorInput);

      ShowIssueFlowsMarkerResolver.getSelectedFlow(markerToUse, selectedFlow).getLocations().forEach(p -> {
        try {
          @Nullable
          Position position = ShowIssueFlowsMarkerResolver.getMarkerPosition(p.getMarker(), textEditor);
          if (position != null && !position.isDeleted()) {
            minings.add(new SonarLintFlowMessageCodeMining(p, doc, position, this));
            minings.add(new SonarLintFlowNumberCodeMining(p, position, this));
          }
        } catch (BadLocationException e) {
          SonarLintLogger.get().error("Unable to create code mining", e);
        }
      });

      monitor.isCanceled();
      return minings;
    });
  }

  public static void setCurrentMarker(@Nullable IMarker currentMarker, int selectedFlow) {
    SonarLintCodeMiningProvider.currentMarker = currentMarker;
    SonarLintCodeMiningProvider.selectedFlow = selectedFlow;
    if (currentMarker != null) {
      ITextEditor editorFound = LocationsUtils.findOpenEditorFor(currentMarker);
      if (editorFound != null) {
        ITextViewer textViewer = editorFound.getAdapter(ITextViewer.class);
        if (textViewer instanceof ISourceViewerExtension5) {
          ((ISourceViewerExtension5) textViewer).updateCodeMinings();
        }
      }
    }

  }

}
