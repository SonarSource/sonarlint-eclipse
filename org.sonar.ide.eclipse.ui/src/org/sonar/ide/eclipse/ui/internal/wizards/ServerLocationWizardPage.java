/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.ui.internal.Messages;
import org.sonar.ide.eclipse.ui.internal.SonarImages;
import org.sonar.ide.eclipse.ui.internal.SonarUiPlugin;

public class ServerLocationWizardPage extends WizardPage {
  private final ISonarServer sonarServer;

  private Text serverUrlText;
  private Text serverUsernameText;
  private Text serverPasswordText;
  private IStatus status;

  public ServerLocationWizardPage() {
    this(SonarCorePlugin.getServersManager().getDefault());
  }

  public ServerLocationWizardPage(ISonarServer sonarServer) {
    super("server_location_page", "SonarQube Server Configuration", SonarImages.SONARWIZBAN_IMG);
    this.sonarServer = sonarServer;
  }

  /**
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(Composite)
   */
  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    layout.numColumns = 2;
    layout.verticalSpacing = 9;

    // Sonar Server URL
    Label label = new Label(container, SWT.NULL);
    label.setText(Messages.ServerLocationWizardPage_label_host);
    serverUrlText = new Text(container, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    serverUrlText.setLayoutData(gd);
    serverUrlText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });

    // Sonar Server Username
    Label labelUsername = new Label(container, SWT.NULL);
    labelUsername.setText(Messages.ServerLocationWizardPage_label_username);
    serverUsernameText = new Text(container, SWT.BORDER | SWT.SINGLE);
    serverUsernameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // Sonar Server password
    Label labelPassword = new Label(container, SWT.NULL);
    labelPassword.setText(Messages.ServerLocationWizardPage_label_password);
    serverPasswordText = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
    serverPasswordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // Sonar test connection button
    createTestConnectionButton(container);

    initialize();
    dialogChanged();
    setControl(container);
  }

  private void createTestConnectionButton(Composite container) {
    Button testConnectionButton = new Button(container, SWT.PUSH);
    testConnectionButton.setText(Messages.ServerLocationWizardPage_action_test);
    testConnectionButton.setToolTipText(Messages.ServerLocationWizardPage_action_test_tooltip);
    testConnectionButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

    testConnectionButton.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        // We need those variables - in other case we would get an IllegalAccessException
        final String serverUrl = getServerUrl();
        final String username = getUsername();
        final String password = getPassword();
        try {
          ServerConnectionTestJob testJob = new ServerConnectionTestJob(username, password, serverUrl);
          getWizard().getContainer().run(true, true, testJob);
          status = testJob.getStatus();
        } catch (InvocationTargetException | InterruptedException e1) {
          SonarCorePlugin.getDefault().error(e1.getMessage(), e1);
          status = new Status(IStatus.ERROR, SonarUiPlugin.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_error);
        } catch (OperationCanceledException e1) {
          status = Status.CANCEL_STATUS;
        }
        getWizard().getContainer().updateButtons();

        String message = status.getMessage();
        switch (status.getSeverity()) {
          case IStatus.OK:
            setMessage(message, IMessageProvider.INFORMATION);
            break;
          default:
            setMessage(message, IMessageProvider.ERROR);
            break;
        }
      }
    });
  }

  private void initialize() {
    serverUrlText.setText(StringUtils.defaultString(sonarServer.getUrl()));
    serverUsernameText.setText(StringUtils.defaultString(sonarServer.getUsername()));
    serverPasswordText.setText(StringUtils.defaultString(sonarServer.getPassword()));
  }

  private void dialogChanged() {
    if (StringUtils.isBlank(getServerUrl())) {
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
    return StringUtils.removeEnd(serverUrlText.getText(), "/");
  }

  public String getUsername() {
    return serverUsernameText.getText();
  }

  public String getPassword() {
    return serverPasswordText.getText();
  }

}
