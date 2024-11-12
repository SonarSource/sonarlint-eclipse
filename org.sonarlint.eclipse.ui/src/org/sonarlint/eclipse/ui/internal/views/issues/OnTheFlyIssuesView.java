/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.views.issues;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;

public class OnTheFlyIssuesView extends MarkerViewWithBottomPanel {
  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.issues.IssuesView";

  public OnTheFlyIssuesView() {
    super(SonarLintUiPlugin.PLUGIN_ID + ".views.issues.onTheFlyIssueMarkerGenerator");
  }

  @Override
  protected void populateBottomPanel(Composite bottom) {
    var bottomLayout = new RowLayout();
    bottomLayout.center = true;
    bottom.setLayout(bottomLayout);
    var bottomLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
    bottom.setLayoutData(bottomLayoutData);

    bottomLabel = new Link(bottom, SWT.NONE);
    bottomLabel.addListener(SWT.Selection,
      e -> BrowserUtils.openExternalBrowser(SonarLintDocumentation.ON_THE_FLY_VIEW_LINK, e.display));
  }

  @Override
  public void resetDefaultText() {
    bottomLabel.setText(
      "Issues reported \"on the fly\" on files you have recently opened/edited. <a>Learn more</a>");
    bottomLabel.getParent().layout();
  }
}
