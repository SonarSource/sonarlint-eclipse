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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFix;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerTextEdit;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.LocationsUtils;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;

public class ApplyQuickFixMarkerResolver implements IMarkerResolution2 {

  private final MarkerQuickFix fix;

  public ApplyQuickFixMarkerResolver(MarkerQuickFix fix) {
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
    IDocument document = getDocument(file);
    Display.getDefault().asyncExec(() -> {
      applyIn(document, fix);
      SonarLintCorePlugin.getTelemetry().addQuickFixAppliedForRule(MarkerUtils.getRuleKey(marker).toString());
    });
  }

  private static IDocument getDocument(ISonarLintFile file) {
    IDocument doc;
    IEditorPart editorPart = PlatformUtils.findEditor(file);
    if (editorPart instanceof ITextEditor) {
      doc = ((ITextEditor) editorPart).getDocumentProvider().getDocument(editorPart.getEditorInput());
    } else {
      doc = file.getDocument();
    }
    return doc;
  }

  private static void applyIn(IDocument document, MarkerQuickFix fix) {
    // IDocumentExtension4 appeared before oldest supported version, no need to check
    IDocumentExtension4 extendedDocument = (IDocumentExtension4) document;
    DocumentRewriteSession session = null;
    try {
      session = extendedDocument.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
      fix.getTextEdits().forEach(textEdit -> apply(extendedDocument, textEdit));
    } catch (Exception e) {
      SonarLintLogger.get().error("Cannot apply the quick fix", e);
    } finally {
      if (session != null) {
        extendedDocument.stopRewriteSession(session);
      }
    }
  }

  private static void apply(IDocumentExtension4 document, MarkerTextEdit textEdit) {
    IMarker editMarker = textEdit.getMarker();
    try {
      ITextEditor textEditor = LocationsUtils.findOpenEditorFor(editMarker);
      if (textEditor == null) {
        // TODO handle closed editors
        return;
      }
      Position markerPosition = LocationsUtils.getMarkerPosition(editMarker, textEditor);
      if (markerPosition != null) {
        document.replace(markerPosition.getOffset(), markerPosition.getLength(), textEdit.getNewText(), document.getModificationStamp() + 1);
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
