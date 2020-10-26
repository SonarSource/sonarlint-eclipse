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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlow;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlowLocation;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.ui.internal.markers.AbstractMarkerSelectionListener;
import org.sonarlint.eclipse.ui.internal.util.LocationsUtils;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView.FlowSelectionListener;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class SonarLintCodeMiningProvider extends AbstractCodeMiningProvider implements AbstractMarkerSelectionListener, FlowSelectionListener {

  @Nullable
  private IMarker currentMarker;
  private int selectedFlowNum = -1;
  private final IPartListener2 partListener;

  public SonarLintCodeMiningProvider() {
    startListeningForSelectionChanges(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());
    IssueLocationsView view = (IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IssueLocationsView.ID);
    if (view != null) {
      this.currentMarker = view.getCurrentMarker();
      this.selectedFlowNum = view.getSelectedFlowNum();
      view.addFlowSelectionListener(this);
    }
    partListener = new IPartListener2() {
      @Override
      public void partOpened(IWorkbenchPartReference partRef) {
        IWorkbenchPart part = partRef.getPart(true);
        if (part instanceof IViewPart) {
          IViewPart viewPart = (IViewPart) part;
          if (viewPart instanceof IssueLocationsView) {
            IssueLocationsView issueLocationsView = (IssueLocationsView) viewPart;
            issueLocationsView.addFlowSelectionListener(SonarLintCodeMiningProvider.this);
            SonarLintCodeMiningProvider.this.selectedFlowNum = issueLocationsView.getSelectedFlowNum();
            forceRefreshCodeMinings();
          }
        }
      }

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
    stopListeningForSelectionChanges(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage());
    IssueLocationsView view = (IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IssueLocationsView.ID);
    if (view != null) {
      view.removeFlowSelectionListener(this);
    }
    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().removePartListener(partListener);
    super.dispose();
  }

  @Override
  public void sonarlintIssueMarkerSelected(IMarker selectedMarker) {
    if (!Objects.equals(currentMarker, selectedMarker)) {
      this.currentMarker = selectedMarker;
      this.selectedFlowNum = 1;
      forceRefreshCodeMinings();
    }
  }

  @Override
  public void flowSelected(int flowNumber) {
    if (!Objects.equals(selectedFlowNum, flowNumber)) {
      this.selectedFlowNum = flowNumber;
      forceRefreshCodeMinings();
    }
  }

  @Override
  public CompletableFuture<List<? extends ICodeMining>> provideCodeMinings(ITextViewer viewer, IProgressMonitor monitor) {
    IMarker markerToUse = currentMarker;
    int flowNum = selectedFlowNum;
    if (markerToUse == null || flowNum < 1) {
      return CompletableFuture.completedFuture(emptyList());
    }

    // Return fast if the maker is not for the current editor
    ITextEditor textEditor = super.getAdapter(ITextEditor.class);
    IFileEditorInput editorInput = textEditor.getEditorInput().getAdapter(IFileEditorInput.class);
    if (editorInput == null || !editorInput.getFile().equals(markerToUse.getResource())) {
      return CompletableFuture.completedFuture(emptyList());
    }

    return CompletableFuture.supplyAsync(() -> {
      monitor.isCanceled();

      List<MarkerFlow> flowsMarkers = MarkerUtils.getIssueFlow(markerToUse);
      IDocument doc = textEditor.getDocumentProvider().getDocument(editorInput);
      List<MarkerFlowLocation> locations;
      if (flowsMarkers.stream().allMatch(f -> f.getLocations().size() == 1)) {
        // Flatten all locations
        locations = flowsMarkers.stream().flatMap(f -> f.getLocations().stream()).collect(toList());
      } else if (flowsMarkers.size() >= flowNum) {
        locations = flowsMarkers.get(flowNum - 1).getLocations();
      } else {
        locations = emptyList();
      }
      List<ICodeMining> minings = createMiningsForLocations(textEditor, locations, doc);

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
          result.add(new SonarLintFlowLocationNumberCodeMining(l, position, this, number));
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
