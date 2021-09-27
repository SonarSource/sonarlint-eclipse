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
package org.sonarlint.eclipse.jdt.internal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.sonarlint.eclipse.ui.quickfixes.ISonarLintMarkerResolver;

/**
 * Inspired by MarkerResolutionProposal
 */
class MarkerResolverJdtAdapter implements ISonarLintMarkerResolver, IJavaCompletionProposal {

  private final ISonarLintMarkerResolver wrapped;
  private final IMarker marker;

  public MarkerResolverJdtAdapter(ISonarLintMarkerResolver resolution, IMarker marker) {
    this.wrapped = resolution;
    this.marker = marker;
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
  public void apply(IDocument document) {
    wrapped.run(marker);
  }

  @Nullable
  @Override
  public Point getSelection(IDocument document) {
    return null;
  }

  @Override
  public String getAdditionalProposalInfo() {
    return wrapped.getDescription();
  }

  @Override
  public String getDisplayString() {
    return getLabel();
  }

  @Nullable
  @Override
  public IContextInformation getContextInformation() {
    return null;
  }

  @Override
  public int getRelevance() {
    return wrapped.getRelevanceForResolution();
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
