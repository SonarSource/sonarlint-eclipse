package org.sonar.ide.eclipse.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * @author Evgeny Mandrikov
 */
public class MeasuresView extends ViewPart {

  public static final String ID = "org.sonar.ide.eclipse.views.MeasuresView";

  private TreeViewer viewer;
  private Action linkToEditorAction;
  private boolean linking;

  @Override
  public void createPartControl(Composite parent) {
    viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    viewer.setContentProvider(new MeasuresContentProvider());

    // Create actions
    linkToEditorAction = new Action(Messages.getString("action.link"), IAction.AS_CHECK_BOX) {

      @Override
      public void run() {
        toggleLinking(isChecked());
      }
    };
    linkToEditorAction.setToolTipText(Messages.getString("action.link.desc")); //$NON-NLS-1$
    linkToEditorAction.setImageDescriptor(SonarPlugin.getImageDescriptor(SonarPlugin.IMG_SONARSYNCHRO));

    // Create toolbar
    IActionBars bars = getViewSite().getActionBars();
    bars.getToolBarManager().add(linkToEditorAction);

    // TODO comment me
    getSite().getPage().addPartListener(partListener2);
  }

  @Override
  public void setFocus() {
    viewer.getControl().setFocus();
  }

  @Override
  public void dispose() {
    getSite().getPage().removePartListener(partListener2);
  }

  protected void toggleLinking(boolean checked) {
    this.linking = checked;
    if (this.linking) {
      editorActivated(getSite().getPage().getActiveEditor());
    }
  }

  protected void editorActivated(IEditorPart editor) {
    if (editor == null) {
      return;
    }
    // TODO
    System.out.println(editor);
  }

  private final IPartListener2 partListener2 = new IPartListener2() {

    public void partActivated(IWorkbenchPartReference ref) {
      if (ref.getPart(true) instanceof IEditorPart) {
        editorActivated(getViewSite().getPage().getActiveEditor());
      }
    }

    public void partBroughtToTop(IWorkbenchPartReference ref) {
      if (ref.getPart(true) == MeasuresView.this) {
        editorActivated(getViewSite().getPage().getActiveEditor());
      }
    }

    public void partClosed(IWorkbenchPartReference ref) {
    }

    public void partDeactivated(IWorkbenchPartReference ref) {
    }

    public void partHidden(IWorkbenchPartReference ref) {
    }

    public void partInputChanged(IWorkbenchPartReference ref) {
    }

    public void partOpened(IWorkbenchPartReference ref) {
      if (ref.getPart(true) == MeasuresView.this) {
        editorActivated(getViewSite().getPage().getActiveEditor());
      }
    }

    public void partVisible(IWorkbenchPartReference ref) {
      if (ref.getPart(true) == MeasuresView.this) {
        editorActivated(getViewSite().getPage().getActiveEditor());
      }
    }
  };

}
