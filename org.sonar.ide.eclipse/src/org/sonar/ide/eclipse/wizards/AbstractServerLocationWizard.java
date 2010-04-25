package org.sonar.ide.eclipse.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * @author Jérémie Lagarde
 */
public abstract class AbstractServerLocationWizard extends Wizard implements INewWizard {

  protected ServerLocationWizardPage page;

  public AbstractServerLocationWizard() {
    super();
    setNeedsProgressMonitor(true);
  }

  protected abstract String getTitle();
  protected abstract String getDefaultUrl();

  public void addPages() {
    super.addPages();
    page = new ServerLocationWizardPage("server_location_page", getTitle(), SonarPlugin.getImageDescriptor(SonarPlugin.IMG_SONARWIZBAN),getDefaultUrl());
    addPage(page);
  }

  public void init(IWorkbench workbench, IStructuredSelection selection) {
  }

  @Override
  public boolean performFinish() {
    final String serverUrl = page.getServerUrl();
    final String username = page.getUsername();
    final String password = page.getPassword();
    IRunnableWithProgress op = new IRunnableWithProgress() {
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        monitor.beginTask("Saving " + serverUrl, 1);
        try {
          doFinish(serverUrl, username, password, monitor);
        } catch (Exception e) {
          SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
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

  protected void doFinish(String serverUrl, String username, String password, IProgressMonitor monitor) throws Exception {
    SonarPlugin.getServerManager().addServer(serverUrl, username, password);
  }
}
