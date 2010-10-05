/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

package org.sonar.ide.eclipse.perspectives;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.sonar.ide.eclipse.views.HotspotsView;
import org.sonar.ide.eclipse.views.MeasuresView;
import org.sonar.ide.eclipse.views.RemoteView;
import org.sonar.ide.eclipse.views.ViolationsView;

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
    left.addView("org.eclipse.jdt.ui.PackageExplorer");

    IFolderLayout right = layout.createFolder("right", IPageLayout.RIGHT, 0.75f, layout.getEditorArea()); //$NON-NLS-1$
    right.addView(MeasuresView.ID);

    IFolderLayout bottom = layout.createFolder("bottomRight", IPageLayout.BOTTOM, 0.75f, layout.getEditorArea()); // NON-NLS-1$
    bottom.addView(RemoteView.ID);
    bottom.addView(HotspotsView.ID);
    bottom.addView(ViolationsView.ID);
    bottom.addView(IConsoleConstants.ID_CONSOLE_VIEW);
  }

  private void addActionSets() {
    layout.addActionSet("org.eclipse.debug.ui.launchActionSet"); // NON-NLS-1
    layout.addActionSet("org.eclipse.debug.ui.debugActionSet"); // NON-NLS-1
    layout.addActionSet("org.eclipse.debug.ui.profileActionSet"); // NON-NLS-1
    layout.addActionSet("org.eclipse.jdt.debug.ui.JDTDebugActionSet"); // NON-NLS-1
    layout.addActionSet("org.eclipse.jdt.junit.JUnitActionSet"); // NON-NLS-1
    layout.addActionSet("org.eclipse.team.ui.actionSet"); // NON-NLS-1
    layout.addActionSet("org.eclipse.jdt.ui.JavaActionSet");
    layout.addActionSet("org.eclipse.jdt.ui.JavaElementCreationActionSet");
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
    layout.addShowViewShortcut("org.eclipse.jdt.ui.PackageExplorer");
    layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
    layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
  }

}
