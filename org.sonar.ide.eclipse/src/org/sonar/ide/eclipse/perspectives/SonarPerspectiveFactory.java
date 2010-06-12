/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.perspectives;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.sonar.ide.eclipse.views.MeasuresView;
import org.sonar.ide.eclipse.views.MetricsView;
import org.sonar.ide.eclipse.views.NavigatorView;

/**
 * @author Jérémie Lagarde
 */
public class SonarPerspectiveFactory implements IPerspectiveFactory {

  private IPageLayout layout;

  public SonarPerspectiveFactory() {
    super();
  }

  public void createInitialLayout(IPageLayout layout) {
    this.layout = layout;
    addViews();
    addActionSets();
    addNewWizardShortcuts();
    addPerspectiveShortcuts();
    addViewShortcuts();
  }

  private void addViews() {
    IFolderLayout left = layout.createFolder("left", IPageLayout.LEFT, 0.25f, layout.getEditorArea()); //$NON-NLS-1$
    left.addView(JavaUI.ID_PACKAGES);

    IFolderLayout leftbottom = layout.createFolder("leftbottom", IPageLayout.BOTTOM, (float) 0.5, "left"); //$NON-NLS-1$
    leftbottom.addView(IPageLayout.ID_PROP_SHEET);
    
    IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.75f, layout.getEditorArea()); //$NON-NLS-1$
    right.addView(MeasuresView.ID);

    IFolderLayout bottom = layout.createFolder("bottomRight", IPageLayout.BOTTOM, 0.75f, layout.getEditorArea()); // NON-NLS-1$
    bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
    bottom.addView(NavigatorView.ID);
    bottom.addView(MetricsView.ID);
    bottom.addView(IConsoleConstants.ID_CONSOLE_VIEW);
  }

  private void addActionSets() {
    layout.addActionSet("org.eclipse.debug.ui.launchActionSet"); // NON-NLS-1
    layout.addActionSet("org.eclipse.debug.ui.debugActionSet"); // NON-NLS-1
    layout.addActionSet("org.eclipse.debug.ui.profileActionSet"); // NON-NLS-1
    layout.addActionSet("org.eclipse.jdt.debug.ui.JDTDebugActionSet"); // NON-NLS-1
    layout.addActionSet("org.eclipse.jdt.junit.JUnitActionSet"); // NON-NLS-1
    layout.addActionSet("org.eclipse.team.ui.actionSet"); // NON-NLS-1
    layout.addActionSet(JavaUI.ID_ACTION_SET);
    layout.addActionSet(JavaUI.ID_ELEMENT_CREATION_ACTION_SET);
    layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET); // NON-NLS-1
  }

  private void addPerspectiveShortcuts() {
    layout.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); // NON-NLS-1
  }

  private void addNewWizardShortcuts() {
    layout.addNewWizardShortcut("org.sonar.ide.eclipse.wizards.newserverlocationwizard");// NON-NLS-1
  }

  private void addViewShortcuts() {
    layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
    layout.addShowViewShortcut(JavaUI.ID_PACKAGES);
    layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
    layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
  }

}
