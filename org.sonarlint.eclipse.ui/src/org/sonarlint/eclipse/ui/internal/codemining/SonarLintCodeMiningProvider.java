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
package org.sonarlint.eclipse.ui.internal.codemining;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlow;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlowLocation;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.flowlocations.SonarLintFlowLocationSelectionListener;
import org.sonarlint.eclipse.ui.internal.flowlocations.SonarLintFlowSelectionListener;
import org.sonarlint.eclipse.ui.internal.flowlocations.SonarLintMarkerSelectionListener;
import org.sonarlint.eclipse.ui.internal.util.LocationsUtils;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class SonarLintCodeMiningProvider extends AbstractCodeMiningProvider
  implements SonarLintMarkerSelectionListener, SonarLintFlowSelectionListener, SonarLintFlowLocationSelectionListener {

  private boolean hasMinings = false;

  private final IPartListener2 partListener;

  public SonarLintCodeMiningProvider() {
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().addMarkerSelectionListener(this);
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().addFlowSelectionListener(this);
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().addFlowLocationSelectionListener(this);
    partListener = new IPartListener2() {

      @Override
      public void partClosed(IWorkbenchPartReference partRef) {
        // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=568243
        IWorkbenchPart part = partRef.getPart(true);
        if (part instanceof IEditorPart) {
          IEditorPart editorPart = (IEditorPart) part;
          ITextEditor myTextEditor = SonarLintCodeMiningProvider.this.getAdapter(ITextEditor.class);
          if (Objects.equals(editorPart.getEditorSite(), myTextEditor.getEditorSite())) {
            dispose();
          }
        }
      }
    };
    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().addPartListener(partListener);
  }

  @Override
  public void dispose() {
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().removeMarkerSelectionListener(this);
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().removeFlowSelectionListener(this);
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().removeFlowLocationSelectionListener(this);
    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListener);
    super.dispose();
  }

  @Override
  public void markerSelected(Optional<IMarker> marker) {
    forceRefreshCodeMiningsIfNecessary(marker, m -> {
      List<MarkerFlow> flowsMarkers = MarkerUtils.getIssueFlows(m);
      return flowsMarkers.stream().flatMap(f -> f.getLocations().stream());
    });
  }

  @Override
  public void flowSelected(Optional<MarkerFlow> flow) {
    forceRefreshCodeMiningsIfNecessary(flow, f -> f.getLocations().stream());
  }

  @Override
  public void flowLocationSelected(Optional<MarkerFlowLocation> flowLocation) {
    forceRefreshCodeMiningsIfNecessary(flowLocation, Stream::of);
  }

  private <G> void forceRefreshCodeMiningsIfNecessary(Optional<G> selected, Function<G, Stream<MarkerFlowLocation>> flowLocationExtractor) {
    // Don't force refresh if uncecessary
    boolean shouldRefresh = false;
    if (hasMinings) {
      // Always refresh if it previously returned some minings, to clear/update them
      shouldRefresh = true;
    }
    if (selected.isPresent()) {
      // Only refresh if at least one marker flow location is on the same file than this editor
      Stream<MarkerFlowLocation> allFlowLocations = flowLocationExtractor.apply(selected.get());
      ITextEditor textEditor = super.getAdapter(ITextEditor.class);
      IFileEditorInput editorInput = textEditor.getEditorInput().getAdapter(IFileEditorInput.class);
      if (editorInput != null && LocationsUtils.hasAtLeastOneLocationOnTheSameResourceThanEditor(allFlowLocations, editorInput)) {
        shouldRefresh = true;
      }
    }
    if (shouldRefresh) {
      forceRefreshCodeMinings();
    }
  }

  @Override
  public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer, IProgressMonitor monitor) {
    if (!SonarLintUiPlugin.getSonarlintMarkerSelectionService().isShowAnnotationsInEditor()) {
      return CompletableFuture.completedFuture(emptyList());
    }
    IMarker markerToUse = SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedMarker().orElse(null);
    if (markerToUse == null) {
      return CompletableFuture.completedFuture(emptyList());
    }
    // Return fast if the marker is not for the current editor
    ITextEditor textEditor = super.getAdapter(ITextEditor.class);
    IFileEditorInput editorInput = textEditor.getEditorInput().getAdapter(IFileEditorInput.class);
    if (editorInput == null || !editorInput.getFile().equals(markerToUse.getResource())) {
      return CompletableFuture.completedFuture(emptyList());
    }
    List<MarkerFlow> flowsMarkers = MarkerUtils.getIssueFlows(markerToUse);
    if (flowsMarkers.isEmpty()) {
      return CompletableFuture.completedFuture(emptyList());
    }
    boolean isSecondaryLocation = MarkerUtils.isSecondaryLocations(flowsMarkers);
    Optional<MarkerFlow> lastSelectedFlow = SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedFlow();
    if (!isSecondaryLocation && !lastSelectedFlow.isPresent()) {
      return CompletableFuture.completedFuture(emptyList());
    }

    return CompletableFuture.supplyAsync(() -> {
      monitor.isCanceled();

      IDocument doc = textEditor.getDocumentProvider().getDocument(editorInput);
      List<MarkerFlowLocation> locations;
      if (isSecondaryLocation) {
        // Flatten all locations
        locations = flowsMarkers.stream().flatMap(f -> f.getLocations().stream()).collect(toList());
      } else if (lastSelectedFlow.isPresent()) {
        locations = lastSelectedFlow.get().getLocations();
      } else {
        locations = emptyList();
      }
      List<ICodeMining> minings = createMiningsForLocations(textEditor, locations, doc);
      hasMinings = !minings.isEmpty();
      monitor.isCanceled();
      return minings;
    });
  }

  private List<ICodeMining> createMiningsForLocations(ITextEditor textEditor, List<MarkerFlowLocation> locations, IDocument doc) {
    int number = 1;
    List<ICodeMining> result = new ArrayList<>();
    for (MarkerFlowLocation l : locations) {
      try {
        @Nullable
        Position position = LocationsUtils.getMarkerPosition(l.getMarker(), textEditor);
        if (position != null && !position.isDeleted()) {
          result.add(new SonarLintFlowMessageCodeMining(l, doc, position, this));
          result.add(
            new SonarLintFlowLocationNumberCodeMining(l, position, this, number,
              l.equals(SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedFlowLocation().orElse(null))));
        }
        number++;
      } catch (BadLocationException e) {
        SonarLintLogger.get().error("Unable to create code mining", e);
      }
    }
    return result;
  }

  private void forceRefreshCodeMinings() {
    ITextEditor textEditor = super.getAdapter(ITextEditor.class);
    ITextViewer textViewer = textEditor.getAdapter(ITextViewer.class);
    if (textViewer instanceof ISourceViewerExtension5) {
      ((ISourceViewerExtension5) textViewer).updateCodeMinings();
    }
  }

}
