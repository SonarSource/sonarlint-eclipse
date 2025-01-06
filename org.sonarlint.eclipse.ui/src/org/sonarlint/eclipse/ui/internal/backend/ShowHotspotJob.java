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
package org.sonarlint.eclipse.ui.internal.backend;

import java.util.Objects;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.jobs.AbstractSonarProjectJob;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.hotspots.HotspotsView;
import org.sonarlint.eclipse.ui.internal.util.DisplayUtils;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarsource.sonarlint.core.rpc.protocol.client.hotspot.HotspotDetailsDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.TextRangeDto;

public class ShowHotspotJob extends AbstractSonarProjectJob {

  private final HotspotDetailsDto hotspotDetails;

  public ShowHotspotJob(ISonarLintProject project, HotspotDetailsDto hotspotDetails) {
    super("Opening security hotspot '" + hotspotDetails.getKey() + "'...", project);
    this.hotspotDetails = hotspotDetails;
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor) throws CoreException {
    try {
      var hotspotFilePath = hotspotDetails.getIdeFilePath();
      var hotspotFile = getProject().find(hotspotFilePath.toString());
      if (hotspotFile.isEmpty()) {
        return Status.error("Unable to find file '" + hotspotFilePath + "' in '" + getProject().getName() + "'");
      }
      show(hotspotFile.get(), hotspotDetails);
    } catch (Exception e) {
      return Status.error(e.getMessage(), e);
    }

    return Status.OK_STATUS;
  }

  private static void show(ISonarLintFile file, HotspotDetailsDto hotspot) {
    Display.getDefault().asyncExec(() -> {
      DisplayUtils.bringToFront();
      var doc = PlatformUtils.getDocumentFromEditorOrFile(file);
      var marker = createMarker(file, hotspot, doc);
      try {
        var view = (HotspotsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(HotspotsView.ID);
        view.openHotspot(hotspot, marker);
      } catch (Exception e) {
        SonarLintLogger.get().error("Unable to open Hotspots View", e);
      }
    });
  }

  @Nullable
  private static IMarker createMarker(ISonarLintFile file, HotspotDetailsDto hotspot, IDocument doc) {
    IMarker marker = null;
    try {
      marker = file.getResource().createMarker(SonarLintCorePlugin.MARKER_HOTSPOT_ID);
      marker.setAttribute(IMarker.MESSAGE, hotspot.getMessage());
      marker.setAttribute(IMarker.LINE_NUMBER, hotspot.getTextRange().getStartLine());
      var position = MarkerUtils.getPosition(doc,
        new TextRangeDto(hotspot.getTextRange().getStartLine(), hotspot.getTextRange().getStartLineOffset(), hotspot.getTextRange().getEndLine(),
          hotspot.getTextRange().getEndLineOffset()));
      if (position != null && Objects.equals(hotspot.getCodeSnippet(), doc.get(position.getOffset(), position.getLength()))) {
        marker.setAttribute(IMarker.CHAR_START, position.getOffset());
        marker.setAttribute(IMarker.CHAR_END, position.getOffset() + position.getLength());
      }
    } catch (Exception e) {
      SonarLintLogger.get().debug("Unable to create hotspot marker", e);
    }
    return marker;
  }

}
