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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

public class MarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

  @Override
  public boolean hasResolutions(final IMarker marker) {
    return isOnFile(marker) && isNotGlobalIssue(marker);
  }

  private static boolean isNotGlobalIssue(final IMarker marker) {
    return marker.getAttribute(IMarker.LINE_NUMBER, 0) > 1
      || marker.getAttribute(IMarker.CHAR_END, 0) > 0;
  }

  private boolean isOnFile(final IMarker marker) {
    return marker.getResource().getAdapter(IFile.class) != null;
  }

  @Override
  public IMarkerResolution[] getResolutions(final IMarker marker) {
    return hasResolutions(marker) ? (new IMarkerResolution[] {new NoSonarResolution(marker)}) : new IMarkerResolution[0];
  }

}
