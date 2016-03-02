package org.sonarlint.eclipse.ui.internal.server.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.server.wizard.NewServerLocationWizard;

/**
 * An action to invoke the new server and server configuration wizard.
 */
public class NewServerWizardAction extends Action {

  /**
   * New server action.
   */
  public NewServerWizardAction() {
    super();

    setImageDescriptor(SonarLintImages.WIZ_NEW_SERVER);
    setText(Messages.actionSetNewServer);
  }

  @Override
  public void run() {
    IWorkbench workbench = PlatformUI.getWorkbench();
    IWorkbenchWindow workbenchWindow = workbench.getActiveWorkbenchWindow();
    ISelection selection = workbenchWindow.getSelectionService().getSelection();

    IStructuredSelection selectionToPass;
    if (selection instanceof IStructuredSelection)
      selectionToPass = (IStructuredSelection) selection;
    else
      selectionToPass = StructuredSelection.EMPTY;

    IWorkbenchWizard wizard = new NewServerLocationWizard();
    wizard.init(workbench, selectionToPass);
    WizardDialog dialog = new WizardDialog(workbench.getActiveWorkbenchWindow().getShell(), wizard);
    dialog.open();
  }

}
