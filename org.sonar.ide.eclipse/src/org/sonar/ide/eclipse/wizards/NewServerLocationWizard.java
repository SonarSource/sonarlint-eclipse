package org.sonar.ide.eclipse.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Jérémie Lagarde
 */
public class NewServerLocationWizard extends Wizard implements INewWizard {

  private NewServerLocationWizardPage page;

  public NewServerLocationWizard() {
    super();
    setNeedsProgressMonitor(true);
  }

  public void addPages() {
    super.addPages();
    page = new NewServerLocationWizardPage("server_location_page", Messages.getString("new.sonar.server"), SonarPlugin.getImageDescriptor(SonarPlugin.IMG_SONARWIZBAN));
    addPage(page);
  }


  public void init(IWorkbench workbench, IStructuredSelection selection) {
  }

  @Override
  public boolean performFinish() {
    final String serverUrl = page.getServerUrl();
    IRunnableWithProgress op = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        try {
          doFinish(serverUrl, monitor);
        } catch (CoreException e) {
          throw new InvocationTargetException(e);
        } finally {
          monitor.done();
        }
      }
    };
    try {
      getContainer().run(true, false, op);
    } catch (InterruptedException e) {
      return false;
    } catch (InvocationTargetException e) {
      Throwable realException = e.getTargetException();
      MessageDialog.openError(getShell(), "Error", realException.getMessage());
      return false;
    }
    return true;
  }

  private void doFinish(
      String serverUrl,
      IProgressMonitor monitor)
      throws CoreException {
    monitor.beginTask("Creating " + serverUrl, 2);
    try {
      SonarPlugin.getServerManager().addServer(serverUrl);
      // TODO : add auto load
    } catch (Exception e) {
      SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
    }
    monitor.worked(1);
  }
}
