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

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.markers.MarkerSupportView;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.ui.internal.markers.AbstractMarkerSelectionListener;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView;

public abstract class MarkerViewWithBottomPanel extends MarkerSupportView implements AbstractMarkerSelectionListener {

  protected MarkerViewWithBottomPanel(String contentGeneratorId) {
    super(contentGeneratorId);
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
    populateBottomPanel(bottom);
    startListeningForSelectionChanges(getSite().getPage());
  }

  protected abstract void populateBottomPanel(Composite bottom);

  @Override
  public void dispose() {
    stopListeningForSelectionChanges(getSite().getPage());
    super.dispose();
  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    SonarLintLogger.get().info("Selection");
    // Only respond to our own selections
    if (part != MarkerViewWithBottomPanel.this) {
      return;
    }
    AbstractMarkerSelectionListener.super.selectionChanged(part, selection);
  }

  @Override
  public void sonarlintIssueMarkerSelected(IMarker selectedMarker) {
    if (!MarkerUtils.getIssueFlow(selectedMarker).isEmpty()) {
      openIssueLocationsView(selectedMarker);
    }
  }

  private static void openIssueLocationsView(IMarker selectedMarker) {
    try {
      IssueLocationsView view = (IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IssueLocationsView.ID);
      if (view == null) {
        view = (IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IssueLocationsView.ID);
        view.setInput(selectedMarker);
      }
    } catch (PartInitException e) {
      SonarLintLogger.get().error("Unable to open Issue Locations View", e);
    }
  }

}
