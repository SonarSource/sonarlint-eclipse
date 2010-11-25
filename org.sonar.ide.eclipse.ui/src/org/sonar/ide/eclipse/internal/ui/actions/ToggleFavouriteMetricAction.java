/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.internal.ui.actions;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.sonar.ide.eclipse.core.ISonarMetric;
import org.sonar.ide.eclipse.internal.ui.SonarImages;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.util.PlatformUtils;
import org.sonar.ide.eclipse.ui.util.SelectionUtils;

public class ToggleFavouriteMetricAction extends BaseSelectionListenerAction {
  public ToggleFavouriteMetricAction() {
    super("");
  }

  @Override
  protected boolean updateSelection(IStructuredSelection selection) {
    ISonarMetric metric = getSelectedMetric(selection);
    if (metric == null) {
      return false;
    }
    if (SonarUiPlugin.getFavouriteMetricsManager().isFavorite(metric)) {
      setText("Remove from favourites");
      setImageDescriptor(SonarImages.STAR_OFF);
    } else {
      setText("Add to favourites");
      setImageDescriptor(SonarImages.STAR);
    }
    return true;
  };

  @Override
  public void run() {
    ISonarMetric metric = getSelectedMetric(getStructuredSelection());
    SonarUiPlugin.getFavouriteMetricsManager().toggle(metric);
    selectionChanged(getStructuredSelection());
  };

  private ISonarMetric getSelectedMetric(IStructuredSelection selection) {
    Object obj = SelectionUtils.getSingleElement(selection);
    if (obj == null) {
      return null;
    }
    return PlatformUtils.adapt(obj, ISonarMetric.class);
  }
}
