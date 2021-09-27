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
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolutionRelevance;
import org.sonarlint.eclipse.ui.quickfixes.ISonarLintMarkerResolver;

public class MarkerResolutionRelevanceAdapter implements ISonarLintMarkerResolver, IMarkerResolutionRelevance {

  private final ISonarLintMarkerResolver wrapped;

  public MarkerResolutionRelevanceAdapter(ISonarLintMarkerResolver wrapped) {
    this.wrapped = wrapped;
  }

  @Override
  public String getLabel() {
    return wrapped.getLabel();
  }

  @Override
  public void run(IMarker marker) {
    wrapped.run(marker);
  }

  @Override
  public String getDescription() {
    return wrapped.getDescription();
  }

  @Override
  public Image getImage() {
    return wrapped.getImage();
  }

  @Override
  public int getRelevanceForResolution() {
    return wrapped.getRelevanceForResolution();
  }

}
