/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.ide.eclipse.internal.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.progress.IProgressConstants;
import org.sonar.ide.eclipse.internal.core.ISonarConstants;
import org.sonar.ide.eclipse.internal.ui.views.HotspotsView;
import org.sonar.ide.eclipse.internal.ui.views.MeasuresView;
import org.sonar.ide.eclipse.internal.ui.views.QualityProfilesView;
import org.sonar.ide.eclipse.internal.ui.views.ViolationsView;
import org.sonar.ide.eclipse.internal.ui.views.WebView;

public class SonarPerspectiveFactory implements IPerspectiveFactory {

  public SonarPerspectiveFactory() {
  }

  public void createInitialLayout(IPageLayout layout) {
    String editorArea = layout.getEditorArea();

    IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.25f, editorArea); // $NON-NLS-1$
    left.addView("org.eclipse.jdt.ui.PackageExplorer");
    left.addPlaceholder(IPageLayout.ID_PROJECT_EXPLORER);

    IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.75f, editorArea); // $NON-NLS-1$
    bottom.addView(WebView.ID);
    bottom.addView(HotspotsView.ID);
    bottom.addView(ViolationsView.ID);
    bottom.addView(QualityProfilesView.ID);
    bottom.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);
    bottom.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);

    IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.75f, editorArea); //$NON-NLS-1$
    right.addView(MeasuresView.ID);

    // action sets
    layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

    // views - Sonar
    layout.addShowViewShortcut(WebView.ID);
    layout.addShowViewShortcut(HotspotsView.ID);
    layout.addShowViewShortcut(MeasuresView.ID);
    layout.addShowViewShortcut(ViolationsView.ID);
    layout.addShowViewShortcut(QualityProfilesView.ID);

    // views - java
    layout.addShowViewShortcut("org.eclipse.jdt.ui.PackageExplorer"); // $NON-NLS-1$

    // views - debugging
    layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
    layout.addShowViewShortcut("org.eclipse.pde.runtime.LogView"); //$NON-NLS-1$

    // views - standard workbench
    layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
    layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
    layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);
    layout.addShowViewShortcut(IProgressConstants.PROGRESS_VIEW_ID);
    layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);

    // new actions
    layout.addNewWizardShortcut(ISonarConstants.PLUGIN_ID + ".wizards.newserverlocationwizard"); //$NON-NLS-1$
  }

}
