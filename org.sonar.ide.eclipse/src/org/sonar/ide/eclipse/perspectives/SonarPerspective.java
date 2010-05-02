package org.sonar.ide.eclipse.perspectives;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.sonar.ide.eclipse.views.MetricsView;
import org.sonar.ide.eclipse.views.NavigatorView;

/**
 * @author Jérémie Lagarde
 */
public class SonarPerspective implements IPerspectiveFactory {

  private IPageLayout factory;

  public SonarPerspective() {
    super();
  }

  public void createInitialLayout(IPageLayout factory) {
    this.factory = factory;
    addViews();
    addActionSets();
    addNewWizardShortcuts();
    addPerspectiveShortcuts();
    addViewShortcuts();
  }

  private void addViews() {
    IFolderLayout left = factory.createFolder("left", IPageLayout.LEFT, (float) 0.25, factory.getEditorArea()); //$NON-NLS-1$
    left.addView(JavaUI.ID_PACKAGES);
    left.addView(JavaUI.ID_PROJECTS_VIEW);

    IFolderLayout bottom = factory.createFolder("bottomRight", IPageLayout.BOTTOM, 0.75f, factory.getEditorArea()); // NON-NLS-1$
    bottom.addView(IPageLayout.ID_PROBLEM_VIEW);
    bottom.addView(NavigatorView.ID);
    bottom.addView(MetricsView.ID);
    bottom.addView(IConsoleConstants.ID_CONSOLE_VIEW);

  }

  private void addActionSets() {
    factory.addActionSet("org.eclipse.debug.ui.launchActionSet"); // NON-NLS-1
    factory.addActionSet("org.eclipse.debug.ui.debugActionSet"); // NON-NLS-1
    factory.addActionSet("org.eclipse.debug.ui.profileActionSet"); // NON-NLS-1
    factory.addActionSet("org.eclipse.jdt.debug.ui.JDTDebugActionSet"); // NON-NLS-1
    factory.addActionSet("org.eclipse.jdt.junit.JUnitActionSet"); // NON-NLS-1
    factory.addActionSet("org.eclipse.team.ui.actionSet"); // NON-NLS-1
    factory.addActionSet(JavaUI.ID_ACTION_SET);
    factory.addActionSet(JavaUI.ID_ELEMENT_CREATION_ACTION_SET);
    factory.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET); // NON-NLS-1
  }

  private void addPerspectiveShortcuts() {
    factory.addPerspectiveShortcut("org.eclipse.ui.resourcePerspective"); // NON-NLS-1
  }

  private void addNewWizardShortcuts() {
    factory.addNewWizardShortcut("org.sonar.ide.eclipse.wizards.newserverlocationwizard");// NON-NLS-1
  }

  private void addViewShortcuts() {
    factory.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
    factory.addShowViewShortcut(JavaUI.ID_PACKAGES);
    factory.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
    factory.addShowViewShortcut(IPageLayout.ID_OUTLINE);
  }

}
