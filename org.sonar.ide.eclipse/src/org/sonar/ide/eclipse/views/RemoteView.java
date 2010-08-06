package org.sonar.ide.eclipse.views;

import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.ui.AbstractPackageExplorerListener;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;

/**
 * @author Evgeny Mandrikov
 */
public class RemoteView extends ViewPart {

  private Browser browser;

  @Override
  public void createPartControl(Composite parent) {
    browser = new Browser(parent, SWT.NONE);
  }

  @Override
  public void setFocus() {
    browser.setFocus();
  }

  @Override
  public void init(IViewSite site) throws PartInitException {
    selectionListener.init(site);
    super.init(site);
  }

  @Override
  public void dispose() {
    super.dispose();
    selectionListener.dispose(getViewSite());
  }

  private AbstractPackageExplorerListener selectionListener = new AbstractPackageExplorerListener(RemoteView.this) {
    @Override
    public void handleSlection(ISelection selection) {
      if (selection instanceof IStructuredSelection) {
        IStructuredSelection sel = (IStructuredSelection) selection;
        Object o = sel.getFirstElement();
        if (o == null) {
          // no selection
          return;
        }

        if (o instanceof ICompilationUnit) {
          ICompilationUnit cu = (ICompilationUnit) o;
          IResource resource = cu.getResource();
          ProjectProperties properties = ProjectProperties.getInstance(resource);
          String key = EclipseResourceUtils.getInstance().getFileKey(resource);
          browser.setUrl(properties.getUrl() + "/resource/index/" + key + "?metric=coverage");
        } else {
          return;
        }
      }
    }
  };

}
