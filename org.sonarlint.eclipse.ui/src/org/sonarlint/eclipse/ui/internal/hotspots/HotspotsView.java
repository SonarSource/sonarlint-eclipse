/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.hotspots;

import java.util.Optional;
import org.eclipse.core.resources.IMarker;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.flowlocations.SonarLintMarkerSelectionListener;

public class HotspotsView extends ViewPart implements SonarLintMarkerSelectionListener {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.HotspotsView";

  @Override
  public void markerSelected(Optional<IMarker> marker) {
  }

  @Override
  public void createPartControl(Composite parent) {
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().addMarkerSelectionListener(this);
  }

  @Override
  public void setFocus() {
  }

  @Override
  public void dispose() {
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().removeMarkerSelectionListener(this);
    super.dispose();
  }

}
