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
package org.sonarlint.eclipse.ui.internal.views.issues;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class SonarLintReportView extends MarkerViewWithBottomPanel {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.issues.ChangeSetIssuesView";
  private static LocalDateTime reportDate;
  private static String reportTitle;
  private static SonarLintReportView instance;
  private Label label;
  private Composite bottom;

  public SonarLintReportView() {
    super(SonarLintUiPlugin.PLUGIN_ID + ".views.issues.reportIssueMarkerGenerator");
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

    label = new Label(bottom, SWT.NONE);
    refreshText();
  }

  private void refreshText() {
    if (reportTitle != null) {
      label.setText(reportTitle + " (at " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(reportDate) + ")");
    } else {
      label.setText("Run the analysis from the SonarLint context menu to find issues in the SCM change set or in all your project files");
    }
  }

  public static void setReportTitle(@Nullable String title) {
    SonarLintReportView.reportDate = title != null ? LocalDateTime.now() : null;
    SonarLintReportView.reportTitle = title;
    if (SonarLintReportView.instance != null) {
      instance.refreshText();
      instance.requestLayout();
    }
  }

  private void requestLayout() {
    // TODO replace by requestLayout() when supporting only Eclipse 4.6+
    bottom.getShell().layout(new Control[] {instance.bottom}, SWT.DEFER);
  }

}
