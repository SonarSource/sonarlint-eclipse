package org.sonar.ide.eclipse.actions;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.sonar.ide.eclipse.SonarPlugin;

public class SendFeedbackAction implements IWorkbenchWindowActionDelegate {

  public void dispose() {
  }

  public void init(IWorkbenchWindow window) {
  }

  public void run(IAction action) {
    IWorkbenchBrowserSupport browserSupport = SonarPlugin.getDefault().getWorkbench().getBrowserSupport();
    try {
      URL url = new URL("http://jira.codehaus.org/browse/SONARIDE");
      if (browserSupport.isInternalWebBrowserAvailable()) {
        browserSupport.createBrowser(null).openURL(url);
      } else {
        browserSupport.getExternalBrowser().openURL(url);
      }
    } catch (PartInitException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (MalformedURLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void selectionChanged(IAction action, ISelection selection) {
  }

}
