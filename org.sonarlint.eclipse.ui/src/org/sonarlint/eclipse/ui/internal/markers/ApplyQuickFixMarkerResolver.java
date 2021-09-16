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
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFix;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFixes;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.LocationsUtils;

public class ApplyQuickFixMarkerResolver extends WorkbenchMarkerResolution {

  private final String fixMessage;

  public ApplyQuickFixMarkerResolver(MarkerQuickFix fix) {
    this.fixMessage = fix.getMessage();
  }

  @Override
  public String getDescription() {
    return "Automatically modifies the code to fix the issue";
  }

  @Override
  public String getLabel() {
    return fixMessage;
  }

  @Override
  public void run(IMarker marker) {
    ISonarLintFile file = Adapters.adapt(marker.getResource(), ISonarLintFile.class);
    if (file == null) {
      return;
    }
    Optional<MarkerQuickFix> qfWithSameMessage = MarkerUtils.getIssueQuickFixes(marker).getQuickFixes().stream().filter(qf -> qf.getMessage().equals(fixMessage)).findFirst();
    if (!qfWithSameMessage.isPresent()) {
      return;
    }
    Display.getDefault().asyncExec(() -> qfWithSameMessage.get().getTextEdits().forEach(textEdit -> {
      try {
        IMarker editMarker = textEdit.getMarker();
        ITextEditor textEditor = LocationsUtils.findOpenEditorFor(editMarker);
        if (textEditor == null) {
          // TODO handle closed editors
          return;
        }
        Position markerPosition = LocationsUtils.getMarkerPosition(editMarker, textEditor);
        if (markerPosition != null) {
          IDocument doc = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
          doc.replace(markerPosition.getOffset(), markerPosition.getLength(), textEdit.getNewText());
        }
      } catch (Exception e) {
        SonarLintLogger.get().error("Quick fix location does not exist", e);
      }
      SonarLintCorePlugin.getTelemetry().addQuickFixAppliedForRule(MarkerUtils.getRuleKey(marker).toString());
    }));
  }

  @Override
  public Image getImage() {
    return SonarLintImages.BALLOON_IMG;
  }

  @Override
  public IMarker[] findOtherMarkers(IMarker[] markers) {
    return Arrays.stream(markers).filter(m -> {
      MarkerQuickFixes otherMarkerQuickFixes = MarkerUtils.getIssueQuickFixes(m);
      return otherMarkerQuickFixes.getQuickFixes().stream().anyMatch(qf -> qf.getMessage().equals(fixMessage));
    }).collect(Collectors.toList()).toArray(new IMarker[0]);
  }

}
