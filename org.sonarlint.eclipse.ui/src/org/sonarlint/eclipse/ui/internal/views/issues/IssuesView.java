/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.views.markers.MarkerSupportView;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class IssuesView extends MarkerSupportView {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.issues.IssuesView";

  public IssuesView() {
    super(SonarLintUiPlugin.PLUGIN_ID + ".views.issues.issueMarkerGenerator");
  }

  @Override
  public void createPartControl(Composite parent) {
    GridLayout layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    parent.setLayout(layout);
    Composite issuesTable = new Composite(parent, SWT.NONE);
    GridData issuesLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    issuesTable.setLayoutData(issuesLayoutData);
    super.createPartControl(issuesTable);
    Composite bottom = new Composite(parent, SWT.NONE);
    RowLayout bottomLayout = new RowLayout();
    bottomLayout.center = true;
    bottom.setLayout(bottomLayout);
    GridData bottomLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
    bottom.setLayoutData(bottomLayoutData);
    Label label = new Label(bottom, SWT.NONE);
    label.setText("Issues reported \"on the fly\" by SonarLint on files you have recently opened/edited");
  }

}
