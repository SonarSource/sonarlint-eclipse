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
package org.sonarlint.eclipse.ui.internal.flowlocations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlow;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlowLocation;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.util.LocationsUtils;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toList;

public class SonarLintFlowAnnotator implements SonarLintMarkerSelectionListener, SonarLintFlowSelectionListener, SonarLintFlowLocationSelectionListener {

  public static final String ISSUE_FLOW_ANNOTATION_TYPE = "org.sonarlint.eclipse.issueFlowAnnotationType";

  public static final IPartListener2 PART_LISTENER = new IPartListener2() {

    private final Map<ITextEditor, SonarLintFlowAnnotator> annotators = new ConcurrentHashMap<>();

    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
      var part = partRef.getPart(true);
      if (part instanceof ITextEditor && !annotators.containsKey(part)) {
        annotators.put((ITextEditor) part, new SonarLintFlowAnnotator((ITextEditor) part));
      }
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
      // Nothing to do
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
      var part = partRef.getPart(true);
      if (part instanceof ITextEditor) {
        var annotator = annotators.remove(part);
        if (annotator != null) {
          annotator.dispose();
        }
      }
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
      // Nothing to do
    }

    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
      var part = partRef.getPart(true);
      if (part instanceof ITextEditor) {
        annotators.put((ITextEditor) part, new SonarLintFlowAnnotator((ITextEditor) part));
      }
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
      // Nothing to do
    }

    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
      // Nothing to do
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
      // Nothing to do
    }
  };

  private final ITextEditor textEditor;

  public SonarLintFlowAnnotator(ITextEditor textEditor) {
    this.textEditor = textEditor;
    var doc = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
    doc.addDocumentListener(new IDocumentListener() {

      @Override
      public void documentChanged(DocumentEvent event) {
        SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedMarker().ifPresent(selectedMarker -> {
          var issueFlows = MarkerUtils.getIssueFlows(selectedMarker);
          var view = (IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IssueLocationsView.ID);
          issueFlows.allLocationsAsStream().forEach(l -> {
            var markerPosition = LocationsUtils.getMarkerPosition(l.getMarker(), textEditor);
            if (markerPosition != null && markerPosition.isDeleted() != l.isDeleted()) {
              l.setDeleted(markerPosition.isDeleted());
              if (view != null) {
                view.refreshLabel(l);
              }
            }
          });
        });
      }

      @Override
      public void documentAboutToBeChanged(DocumentEvent event) {
        // Nothing to do
      }
    });
    updateFlowAnnotations(textEditor);
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().addMarkerSelectionListener(this);
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().addFlowSelectionListener(this);
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().addFlowLocationSelectionListener(this);
  }

  protected void dispose() {
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().removeMarkerSelectionListener(this);
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().removeFlowSelectionListener(this);
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().removeFlowLocationSelectionListener(this);
  }

  @Override
  public void markerSelected(Optional<IMarker> marker) {
    updateFlowAnnotations(textEditor);
  }

  @Override
  public void flowSelected(Optional<MarkerFlow> flow) {
    updateFlowAnnotations(textEditor);
  }

  @Override
  public void flowLocationSelected(Optional<MarkerFlowLocation> flowLocation) {
    updateFlowAnnotations(textEditor);
  }

  public static void updateFlowAnnotations(ITextEditor textEditor) {
    var editorInput = textEditor.getEditorInput();
    var annotationModel = textEditor.getDocumentProvider().getAnnotationModel(editorInput);
    if (annotationModel != null) {
      var newAnnotations = createAnnotations(textEditor);
      var existingFlowAnnotations = existingFlowAnnotations(annotationModel);
      if (annotationModel instanceof IAnnotationModelExtension) {
        ((IAnnotationModelExtension) annotationModel).replaceAnnotations(existingFlowAnnotations.toArray(new Annotation[0]), newAnnotations);
      } else {
        removePreviousAnnotations(annotationModel);
        newAnnotations.forEach(annotationModel::addAnnotation);
      }
    }
  }

  private static Map<Annotation, Position> createAnnotations(ITextEditor textEditor) {
    if (!SonarLintUiPlugin.getSonarlintMarkerSelectionService().isShowAnnotationsInEditor()) {
      return emptyMap();
    }
    var markerToUse = SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedMarker().orElse(null);
    if (markerToUse == null) {
      return emptyMap();
    }
    var flowsMarkers = MarkerUtils.getIssueFlows(markerToUse);
    if (flowsMarkers.isEmpty()) {
      return emptyMap();
    }
    var lastSelectedFlow = SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedFlow();
    List<MarkerFlowLocation> locations;
    if (flowsMarkers.isSecondaryLocations()) {
      // Flatten all locations
      locations = flowsMarkers.allLocationsAsStream().collect(toList());
    } else if (lastSelectedFlow.isPresent()) {
      locations = lastSelectedFlow.get().getLocations();
    } else {
      locations = emptyList();
    }
    var result = new HashMap<Annotation, Position>();
    locations.forEach(location -> {
      var marker = location.getMarker();
      if (marker != null && !location.isDeleted()) {
        var markerPosition = LocationsUtils.getMarkerPosition(marker, textEditor);
        if (markerPosition != null && !markerPosition.isDeleted()) {
          var annotation = new Annotation(ISSUE_FLOW_ANNOTATION_TYPE, false, location.getMessage());
          // Copy the position to avoid having it updated twice when document is updated
          result.put(annotation, new Position(markerPosition.getOffset(), markerPosition.getLength()));
        }
      }
    });
    return result;
  }

  private static void removePreviousAnnotations(IAnnotationModel annotationModel) {
    var existingFlowAnnotations = existingFlowAnnotations(annotationModel);
    if (annotationModel instanceof IAnnotationModelExtension) {
      ((IAnnotationModelExtension) annotationModel).replaceAnnotations(existingFlowAnnotations.toArray(new Annotation[0]), Collections.emptyMap());
    } else {
      existingFlowAnnotations.forEach(annotationModel::removeAnnotation);
    }
  }

  private static List<Annotation> existingFlowAnnotations(IAnnotationModel annotationModel) {
    var result = new ArrayList<Annotation>();
    annotationModel.getAnnotationIterator().forEachRemaining(a -> {
      if (ISSUE_FLOW_ANNOTATION_TYPE.equals(a.getType())) {
        result.add(a);
      }
    });
    return result;
  }

}
