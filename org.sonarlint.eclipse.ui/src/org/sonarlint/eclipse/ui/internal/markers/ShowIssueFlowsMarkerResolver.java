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
package org.sonarlint.eclipse.ui.internal.markers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlow;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.LocationsUtils;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView;

import static java.util.Collections.emptyMap;

public class ShowIssueFlowsMarkerResolver implements IMarkerResolution2 {

  public static final String ISSUE_FLOW_ANNOTATION_TYPE = "org.sonarlint.eclipse.issueFlowAnnotationType";
  private final IMarker marker;

  public ShowIssueFlowsMarkerResolver(IMarker marker) {
    this.marker = marker;
  }

  @Override
  public String getDescription() {
    return "Show/Hide all locations to better understand the issue: " + marker.getAttribute(IMarker.MESSAGE, "unknown");
  }

  @Override
  public String getLabel() {
    return "Toggle all issue locations";
  }

  @Override
  public void run(IMarker marker) {
    Display.getDefault().asyncExec(() -> {
      try {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorPart editorPart = IDE.openEditor(page, marker);
        if (editorPart instanceof ITextEditor) {
          ITextEditor textEditor = (ITextEditor) editorPart;
          toggleAnnotations(marker, textEditor);
        }
      } catch (Exception e) {
        SonarLintLogger.get().error("Unable to show issue locations", e);
      }
    });
  }

  private static void updateLocationsView(IMarker marker) {
    try {
      IssueLocationsView view = (IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IssueLocationsView.ID);
      view.setShowAnnotations(true);
      view.setInput(marker);
    } catch (PartInitException e) {
      SonarLintLogger.get().error("Unable to open Issue Locations View", e);
    }
  }

  private static void toggleAnnotations(IMarker marker, ITextEditor textEditor) {
    IEditorInput editorInput = textEditor.getEditorInput();
    IAnnotationModel annotationModel = textEditor.getDocumentProvider().getAnnotationModel(editorInput);
    Map<Annotation, Position> newAnnotations = createAnnotations(marker, textEditor, 1);
    List<Annotation> existingFlowAnnotations = existingFlowAnnotations(annotationModel);
    boolean annotationAlreadyDisplayedForThisMarker = !existingFlowAnnotations.isEmpty()
      && newAnnotations.containsValue(annotationModel.getPosition(existingFlowAnnotations.iterator().next()));
    if (!annotationAlreadyDisplayedForThisMarker) {
      updateLocationsView(marker);
    } else {
      IssueLocationsView view = (IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IssueLocationsView.ID);
      if (view != null) {
        view.setShowAnnotations(false);
      }
      removePreviousAnnotations(annotationModel);
    }
  }

  public static void showAnnotations(IMarker marker, ITextEditor textEditor, int selectedFlow) {
    IEditorInput editorInput = textEditor.getEditorInput();
    IAnnotationModel annotationModel = textEditor.getDocumentProvider().getAnnotationModel(editorInput);
    Map<Annotation, Position> newAnnotations = createAnnotations(marker, textEditor, selectedFlow);
    List<Annotation> existingFlowAnnotations = existingFlowAnnotations(annotationModel);
    if (annotationModel instanceof IAnnotationModelExtension) {
      ((IAnnotationModelExtension) annotationModel).replaceAnnotations(existingFlowAnnotations.toArray(new Annotation[0]), newAnnotations);
    } else {
      removePreviousAnnotations(annotationModel);
      newAnnotations.forEach(annotationModel::addAnnotation);
    }
  }

  private static Map<Annotation, Position> createAnnotations(IMarker marker, ITextEditor textEditor, int selectedFlow) {
    List<MarkerFlow> issueFlow = MarkerUtils.getIssueFlow(marker);
    if (issueFlow.size() >= selectedFlow) {
      Map<Annotation, Position> result = new HashMap<>();
      issueFlow.get(selectedFlow - 1)
        .getLocations().forEach(location -> {
          Position markerPosition = LocationsUtils.getMarkerPosition(location.getMarker(), textEditor);
          if (markerPosition != null && !markerPosition.isDeleted()) {
            Annotation annotation = new Annotation(ISSUE_FLOW_ANNOTATION_TYPE, false, location.getMessage());
            // Copy the position to avoid having it updated twice when document is updated
            result.put(annotation, new Position(markerPosition.getOffset(), markerPosition.getLength()));
          }
        });
      return result;
    }
    return emptyMap();
  }

  private static void removePreviousAnnotations(IAnnotationModel annotationModel) {
    List<Annotation> existingFlowAnnotations = existingFlowAnnotations(annotationModel);
    if (annotationModel instanceof IAnnotationModelExtension) {
      ((IAnnotationModelExtension) annotationModel).replaceAnnotations(existingFlowAnnotations.toArray(new Annotation[0]), Collections.emptyMap());
    } else {
      existingFlowAnnotations.forEach(annotationModel::removeAnnotation);
    }
  }

  public static void removeAnnotations(ITextEditor textEditor) {
    IDocumentProvider documentProvider = textEditor.getDocumentProvider();
    if (documentProvider != null) {
      IAnnotationModel annotationModel = documentProvider.getAnnotationModel(textEditor.getEditorInput());
      removePreviousAnnotations(annotationModel);
    }
  }

  public static void removeAllAnnotations() {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    for (IEditorReference editorRef : page.getEditorReferences()) {
      IEditorPart editorPart = editorRef.getEditor(false);
      if (editorPart instanceof ITextEditor) {
        ITextEditor textEditor = (ITextEditor) editorPart;
        ITextViewer textViewer = textEditor.getAdapter(ITextViewer.class);
        if (textViewer instanceof ISourceViewerExtension5) {
          ((ISourceViewerExtension5) textViewer).updateCodeMinings();
        }
        IDocumentProvider documentProvider = textEditor.getDocumentProvider();
        if (documentProvider != null) {
          IAnnotationModel annotationModel = documentProvider.getAnnotationModel(textEditor.getEditorInput());
          removePreviousAnnotations(annotationModel);
        }
      }
    }
  }

  private static List<Annotation> existingFlowAnnotations(IAnnotationModel annotationModel) {
    List<Annotation> result = new ArrayList<>();
    annotationModel.getAnnotationIterator().forEachRemaining(a -> {
      if (ISSUE_FLOW_ANNOTATION_TYPE.equals(a.getType())) {
        result.add(a);
      }
    });
    return result;
  }

  @Override
  public Image getImage() {
    return SonarLintImages.RESOLUTION_SHOW_LOCATIONS;
  }
}
