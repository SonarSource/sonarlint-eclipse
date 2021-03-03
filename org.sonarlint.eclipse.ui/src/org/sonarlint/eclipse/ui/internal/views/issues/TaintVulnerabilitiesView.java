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
package org.sonarlint.eclipse.ui.internal.views.issues;

import java.time.LocalDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class TaintVulnerabilitiesView extends MarkerViewWithBottomPanel {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.issues.TaintVulnerabilitiesView";
  private static LocalDateTime reportDate;
  private static String reportTitle;
  private static TaintVulnerabilitiesView instance;
  private Link label;
  private Composite bottom;

  public TaintVulnerabilitiesView() {
    super(SonarLintUiPlugin.PLUGIN_ID + ".views.issues.taintIssueMarkerGenerator");
    instance = this;
  }

  @Override
  public void dispose() {
    super.dispose();
    instance = null;
  }

  @Override
  protected void populateBottomPanel(Composite bottom) {
    this.bottom = bottom;
    RowLayout bottomLayout = new RowLayout();
    bottomLayout.center = true;
    bottom.setLayout(bottomLayout);
    GridData bottomLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
    bottom.setLayoutData(bottomLayoutData);

    label = new Link(bottom, SWT.NONE);
    label.setText("This view displays taint vulnerabilities found by SonarQube or SonarCloud during last analysis. For more informations see <a>TODO</a>");
  }

}
