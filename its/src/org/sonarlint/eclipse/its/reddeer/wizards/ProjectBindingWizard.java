package org.sonarlint.eclipse.its.reddeer.wizards;

import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.reference.ReferencedComposite;
import org.eclipse.reddeer.jface.wizard.WizardDialog;
import org.eclipse.reddeer.jface.wizard.WizardPage;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.DefaultText;

public class ProjectBindingWizard extends WizardDialog {
  public ProjectBindingWizard() {
    super(new DefaultShell());
  }

  public static class BoundProjectsPage extends WizardPage {

    public BoundProjectsPage(ReferencedComposite referencedComposite) {
      super(referencedComposite);
    }

    public void clickAdd() {
      new PushButton("Add...").click();
    }
  }

  public static class ServerProjectSelectionPage extends WizardPage {

    public ServerProjectSelectionPage(ReferencedComposite referencedComposite) {
      super(referencedComposite);
    }

    public void waitForProjectsToBeFetched() {
      new WaitUntil(new ControlIsEnabled(new DefaultText(this)));
    }

    public void setProjectKey(String projectKey) {
      new DefaultText(this).setText(projectKey);
    }
  }

}
