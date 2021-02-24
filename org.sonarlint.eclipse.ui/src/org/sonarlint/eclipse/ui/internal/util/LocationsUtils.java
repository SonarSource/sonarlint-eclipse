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
package org.sonarlint.eclipse.ui.internal.util;

import java.util.stream.Stream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlowLocation;

public class LocationsUtils {

  private LocationsUtils() {
    // Utility class
  }

  @Nullable
  public static ITextEditor findOpenEditorFor(IMarker sonarlintMarker) {
    // Find IFile and open Editor
    // Super defensing programming because we don't really understand what is initialized at startup (SLE-122)
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      return null;
    }
    IWorkbenchPage page = window.getActivePage();
    if (page == null) {
      return null;
    }
    for (IEditorReference editor : page.getEditorReferences()) {
      IEditorInput editorInput;
      try {
        editorInput = editor.getEditorInput();
      } catch (PartInitException e) {
        SonarLintLogger.get().error("Unable to restore editor", e);
        continue;
      }
      if (editorInput instanceof IFileEditorInput && ((IFileEditorInput) editorInput).getFile().equals(sonarlintMarker.getResource())) {
        IEditorPart editorPart = editor.getEditor(false);
        if (editorPart instanceof ITextEditor) {
          return (ITextEditor) editorPart;
        }
      }
    }
    return null;
  }

  public static @Nullable Position getMarkerPosition(IMarker marker, ITextEditor textEditor) {
    // look up the current range of the marker when the document has been edited
    IAnnotationModel model = textEditor.getDocumentProvider().getAnnotationModel(textEditor.getEditorInput());
    if (model instanceof AbstractMarkerAnnotationModel) {
      AbstractMarkerAnnotationModel markerModel = (AbstractMarkerAnnotationModel) model;
      return markerModel.getMarkerPosition(marker);
    }
    return null;
  }

  public static boolean hasAtLeastOneLocationOnTheSameResourceThanEditor(Stream<MarkerFlowLocation> allFlowLocations, IFileEditorInput editorInput) {
    return allFlowLocations.map(MarkerFlowLocation::getMarker).filter(m -> m != null && m.exists()).map(IMarker::getResource).anyMatch(r -> r.equals(editorInput.getFile()));
  }
}
