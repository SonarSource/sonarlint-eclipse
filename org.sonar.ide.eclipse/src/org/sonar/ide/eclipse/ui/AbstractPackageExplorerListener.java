/**
 * 
 */
package org.sonar.ide.eclipse.ui;

import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * @author Evgeny Mandrikov
 */
public abstract class AbstractPackageExplorerListener implements ISelectionListener, IPartListener2 {

  private IWorkbenchPart part;
  private boolean visible;
  private ISelection currentSelection;

  public AbstractPackageExplorerListener(IWorkbenchPart part) {
    this.part = part;
  }

  public void init(IViewSite site) {
    site.getPage().addSelectionListener(JavaUI.ID_PACKAGES, this);
    site.getPage().addPartListener(this);
  }

  public void dispose(IViewSite site) {
    site.getPage().removeSelectionListener(JavaUI.ID_PACKAGES, this);
    site.getPage().removePartListener(this);
  }

  protected abstract void handleSlection(ISelection selection);

  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    // Don't handle selections, if this view inactive, eg. when another perspective selected
    if ( !visible) {
      return;
    }
    // TODO comment me
    if (selection == null || selection.equals(currentSelection)) {
      return;
    }
    // TODO comment me
    if (part == null) {
      return;
    }
    currentSelection = selection;
    handleSlection(selection);
  }

  public void partActivated(IWorkbenchPartReference partRef) {
  }

  public void partBroughtToTop(IWorkbenchPartReference partRef) {
  }

  public void partClosed(IWorkbenchPartReference partRef) {
  }

  public void partDeactivated(IWorkbenchPartReference partRef) {
  }

  public void partHidden(IWorkbenchPartReference partRef) {
    setVisible(partRef, false);
  }

  public void partInputChanged(IWorkbenchPartReference partRef) {
  }

  public void partOpened(IWorkbenchPartReference partRef) {
    setVisible(partRef, true);
  }

  public void partVisible(IWorkbenchPartReference partRef) {
    setVisible(partRef, true);
  }

  private void setVisible(IWorkbenchPartReference partRef, boolean visible) {
    if (partRef.getPart(true) == part) {
      this.visible = visible;
    }
  }
}
