package org.sonar.ide.eclipse.wizards;

import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.preferences.PreferenceConstants;

/**
 * @author Jérémie Lagarde
 */
public class NewServerLocationWizardPage extends WizardPage {

  private Text serverUrlText;

  public NewServerLocationWizardPage(String pageName) {
    super(pageName);
  }

  public NewServerLocationWizardPage(String pageName, String title, ImageDescriptor titleImage) {
    super(pageName, title, titleImage);
  }


  /**
   * @see IDialogPage#createControl(Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    layout.numColumns = 3;
    layout.verticalSpacing = 9;
    Label label = new Label(container, SWT.NULL);
    label.setText("&Sonar server url:");

    serverUrlText = new Text(container, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    serverUrlText.setLayoutData(gd);
    serverUrlText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });
    initialize();
    dialogChanged();
    setControl(container);
  }

  /**
   * Tests if the current workbench selection is a suitable container to use.
   */

  private void initialize() {
    serverUrlText.setText(SonarPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SONAR_SERVER_URL));
  }

  /**
   * Uses the standard container selection dialog to choose the new value for
   * the container field.
   */

  private void dialogChanged() {
    if (getServerUrl().length() == 0) {
      updateStatus("Server url must be specified");
      return;
    }
    updateStatus(null);
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

  public String getServerUrl() {
    return serverUrlText.getText();
  }
}