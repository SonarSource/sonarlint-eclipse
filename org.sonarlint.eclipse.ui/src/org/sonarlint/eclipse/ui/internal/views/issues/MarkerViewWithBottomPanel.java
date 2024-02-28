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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.views.markers.MarkerSupportView;
import org.sonarlint.eclipse.ui.internal.SonarLintRpcClientSupportService;

public abstract class MarkerViewWithBottomPanel extends MarkerSupportView {
  protected static final String UNAVAILABLE_MESSAGE = "The analysis is not available as the SonarLint backend is not ready";

  @Nullable
  protected static MarkerViewWithBottomPanel instance;

  @Nullable
  protected Link bottomLabel;

  protected MarkerViewWithBottomPanel(String contentGeneratorId) {
    super(contentGeneratorId);
    instance = this;
  }

  @Override
  public void dispose() {
    super.dispose();
    instance = null;
  }

  @Override
  public void createPartControl(Composite parent) {
    var layout = new GridLayout(1, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 0;
    layout.verticalSpacing = 0;
    parent.setLayout(layout);
    var issuesTable = new Composite(parent, SWT.NONE);
    var issuesLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    issuesTable.setLayoutData(issuesLayoutData);
    super.createPartControl(issuesTable);
    var bottom = new Composite(parent, SWT.NONE);
    populateBottomPanel(bottom);
    if (SonarLintRpcClientSupportService.getSloopAvailability()) {
      resetDefaultText();
    } else {
      warnAboutSloopUnavailable();
    }
  }

  protected abstract void populateBottomPanel(Composite bottom);

  protected abstract void resetDefaultText();

  protected void warnAboutSloopUnavailable() {
    bottomLabel.setText(UNAVAILABLE_MESSAGE);
    bottomLabel.getParent().layout();
  }

  public static void tryWarnAboutSloopUnavailable() {
    if (MarkerViewWithBottomPanel.instance != null) {
      MarkerViewWithBottomPanel.instance.warnAboutSloopUnavailable();
    }
  }

  public static void tryResetDefaultText() {
    if (MarkerViewWithBottomPanel.instance != null) {
      MarkerViewWithBottomPanel.instance.resetDefaultText();
    }
  }
}
