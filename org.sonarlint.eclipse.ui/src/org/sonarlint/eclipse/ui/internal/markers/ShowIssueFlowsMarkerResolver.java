/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.swt.graphics.Image;
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
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils.ExtraPosition;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.views.IssueLocationsView;

public class ShowIssueFlowsMarkerResolver implements IMarkerResolution2 {

  private static final String ISSUE_FLOW_ANNOTATION_TYPE = "org.sonarlint.eclipse.issueFlowAnnotationType";

  @Override
  public String getDescription() {
    return "Show/Hide all locations to better understand the issue";
  }

  @Override
  public String getLabel() {
    return "Toggle all issue locations";
  }

  @Override
  public void run(IMarker marker) {
    try {
      IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      IEditorPart editorPart = IDE.openEditor(page, marker);
      if (editorPart instanceof ITextEditor) {
        ITextEditor textEditor = (ITextEditor) editorPart;
        toggleAnnotations(marker, textEditor, false);
        updateLocationsView(marker);
      }
    } catch (Exception e) {
      SonarLintLogger.get().error("Unable to show issue locations", e);
    }
  }

  private static void updateLocationsView(IMarker marker) {
    try {
      IssueLocationsView view = (IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IssueLocationsView.ID);
      view.setInput(marker);
    } catch (PartInitException e) {
      SonarLintLogger.get().error("Unable to open Issue Locations View", e);
    }
  }

  public static void toggleAnnotations(IMarker marker, ITextEditor textEditor, boolean forceShow) {
    IEditorInput editorInput = textEditor.getEditorInput();
    IAnnotationModel annotationModel = textEditor.getDocumentProvider().getAnnotationModel(editorInput);
    IDocument doc = textEditor.getDocumentProvider().getDocument(editorInput);
    Map<Annotation, Position> newAnnotations = createAnnotations(marker, doc);
    List<Annotation> existingFlowAnnotations = existingFlowAnnotations(annotationModel);
    if (!forceShow && !existingFlowAnnotations.isEmpty() && newAnnotations.containsValue(annotationModel.getPosition(existingFlowAnnotations.iterator().next()))) {
      removePreviousAnnotations(annotationModel);
    } else {
      if (annotationModel instanceof IAnnotationModelExtension) {
        ((IAnnotationModelExtension) annotationModel).replaceAnnotations(existingFlowAnnotations.toArray(new Annotation[0]), newAnnotations);
      } else {
        removePreviousAnnotations(annotationModel);
        newAnnotations.forEach(annotationModel::addAnnotation);
      }
    }
  }

  private static Map<Annotation, Position> createAnnotations(IMarker marker, IDocument doc) {
    Position[] positions;
    try {
      positions = doc.getPositions(MarkerUtils.SONARLINT_EXTRA_POSITIONS_CATEGORY);
    } catch (BadPositionCategoryException e) {
      SonarLintLogger.get().error("Unable to read positions", e);
      return Collections.emptyMap();
    }
    return Arrays.asList(positions)
      .stream()
      .map(p -> (ExtraPosition) p)
      .filter(p -> p.getMarkerId() == marker.getId() && !p.isDeleted)
      .collect(Collectors.toMap(p -> new Annotation(ISSUE_FLOW_ANNOTATION_TYPE, false, p.getMessage()),
        p -> new Position(p.getOffset(), p.getLength())));
  }

  private static void removePreviousAnnotations(IAnnotationModel annotationModel) {
    existingFlowAnnotations(annotationModel).forEach(annotationModel::removeAnnotation);
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
      if (editorPart != null && editorPart instanceof ITextEditor) {
        ITextEditor textEditor = (ITextEditor) editorPart;
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
      // Cast are required for Eclipse prior 4.6
      if (ISSUE_FLOW_ANNOTATION_TYPE.equals(((Annotation) a).getType())) {
        result.add((Annotation) a);
      }
    });
    return result;
  }

  @Override
  public Image getImage() {
    return SonarLintImages.IMG_ISSUE;
  }
}
