/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.ui.internal.resolver;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Image;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.ui.views.markers.WorkbenchMarkerResolution;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public class NoSonarResolution extends WorkbenchMarkerResolution {

  private static final String NOSONAR_TAG = "NOSONAR"; //$NON-NLS-1$
  private static final String NOSONAR_COMMENT = "// " + NOSONAR_TAG; //$NON-NLS-1$
  private final IMarker marker;

  public NoSonarResolution(IMarker marker) {
    this.marker = marker;
  }

  @Override
  public String getDescription() {
    return "Add a NOSONAR comment that will mute all SonarLint issues on this line";
  }

  @Override
  public String getLabel() {
    return "Add NOSONAR";
  }

  @Override
  public void run(IMarker marker) {
    final int line = marker.getAttribute(IMarker.LINE_NUMBER, -1);
    final IFile iFile = marker.getResource().getAdapter(IFile.class);
    if ((line == -1) || (iFile == null)) {
      return;
    }
    ITextFileBufferManager iTextFileBufferManager = FileBuffers.getTextFileBufferManager();
    try {
      iTextFileBufferManager.connect(iFile.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
      ITextFileBuffer iTextFileBuffer = iTextFileBufferManager.getTextFileBuffer(iFile.getFullPath(), LocationKind.IFILE);
      IDocument iDoc = iTextFileBuffer.getDocument();
      final IRegion region = iDoc.getLineInformation(line - 1);
      final int endOfLine = region.getOffset() + region.getLength();
      final String lineSource = iDoc.get(region.getOffset(), region.getLength());
      if (lineSource.contains(NOSONAR_TAG)) {
        return;
      }
      final MultiTextEdit edit = new MultiTextEdit();
      edit.addChild(new InsertEdit(endOfLine, " " + NOSONAR_COMMENT));
      edit.apply(iDoc);
      marker.delete();
    } catch (Exception e) {
      SonarLintCorePlugin.getDefault().error("Unable to apply NOSONAR resolution: " + e.getMessage(), e);
    } finally {
      try {
        iTextFileBufferManager.disconnect(iFile.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
      } catch (CoreException e) {
        // Ignore
      }
    }
  }

  @Override
  public Image getImage() {
    return SonarLintImages.SONAR16_IMG;
  }

  @Override
  public IMarker[] findOtherMarkers(IMarker[] markers) {
    List<IMarker> result = new ArrayList<>();
    for (IMarker iMarker : markers) {
      if (iMarker != this.marker) {
        result.add(iMarker);
      }
    }
    return result.toArray(new IMarker[result.size()]);
  }
}
