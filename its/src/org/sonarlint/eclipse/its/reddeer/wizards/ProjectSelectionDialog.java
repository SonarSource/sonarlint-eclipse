package org.sonarlint.eclipse.its.reddeer.wizards;

import org.eclipse.reddeer.jface.dialogs.TitleAreaDialog;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.swt.impl.text.DefaultText;

public class ProjectSelectionDialog extends TitleAreaDialog {
  public ProjectSelectionDialog() {
    super(new DefaultShell());
  }

  public void setProjectName(String projectName) {
    new DefaultText(this).setText(projectName);
  }
  
  public void ok() {
    new PushButton(this, "OK").click();
  }
}
