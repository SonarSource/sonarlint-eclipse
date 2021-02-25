/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonarlint.eclipse.its.reddeer.views;

import java.util.List;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.condition.AbstractExtendedMarkersViewIsUpdating;
import org.eclipse.reddeer.eclipse.ui.views.markers.AbstractMarkersSupportView;
import org.eclipse.reddeer.swt.api.TreeItem;
import org.eclipse.reddeer.swt.impl.tree.DefaultTree;
import org.sonarlint.eclipse.ui.internal.views.issues.SonarLintReportView;

public class ReportView extends AbstractMarkersSupportView {

  public ReportView() {
    super("SonarLint Report");
  }

  public List<TreeItem> getItems() {
    activate();
    new WaitUntil(new ReportViewMarkerIsUpdating(), TimePeriod.SHORT, false);
    new WaitWhile(new ReportViewMarkerIsUpdating());
    return new DefaultTree(cTabItem).getItems();
  }

  private class ReportViewMarkerIsUpdating extends AbstractExtendedMarkersViewIsUpdating {

    public ReportViewMarkerIsUpdating() {
      super(ReportView.this, SonarLintReportView.class);
    }
  }
}
