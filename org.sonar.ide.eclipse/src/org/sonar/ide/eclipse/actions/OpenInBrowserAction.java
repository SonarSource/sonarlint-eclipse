package org.sonar.ide.eclipse.actions;

import java.net.URL;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;

/**
 * Open the internal web browser to show the page of the sonar server corresponding to the selection.
 * 
 * @author Jérémie Lagarde
 * 
 */
public class OpenInBrowserAction implements IObjectActionDelegate {

  private IStructuredSelection selection;

  public OpenInBrowserAction() {
    super();
  }

  public void setActivePart(IAction action, IWorkbenchPart targetPart) {
  }

  public void run(IAction action) {
    try {
      Object element = selection.getFirstElement();
      if (element instanceof IResource) {
        openBrowser((IResource)element);
      }  
    } catch (Exception e) {
      SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
    }
  }

  protected void openBrowser(IResource resource) {
    String fileKey = EclipseResourceUtils.getInstance().getFileKey(resource);
    IWorkbenchBrowserSupport browserSupport = SonarPlugin.getDefault().getWorkbench().getBrowserSupport();
    ProjectProperties properties = ProjectProperties.getInstance(resource);
    try {
      URL consoleURL = new URL(properties.getUrl() + "/resource/index/" + fileKey);
      if (browserSupport.isInternalWebBrowserAvailable()) {
        browserSupport.createBrowser("id" + properties.getUrl().hashCode()).openURL(consoleURL);
      } else {
        browserSupport.getExternalBrowser().openURL(consoleURL);
      }
    }catch(Exception e) {
      SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
    }
  }
  
  public void selectionChanged(IAction action, ISelection selection) {
    if (selection instanceof IStructuredSelection) {
      this.selection = (IStructuredSelection) selection;
    }
  }
}
