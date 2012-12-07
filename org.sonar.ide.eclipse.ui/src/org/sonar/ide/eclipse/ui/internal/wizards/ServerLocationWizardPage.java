/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.ui.internal.ISonarConstants;
import org.sonar.ide.eclipse.ui.internal.Messages;
import org.sonar.ide.eclipse.ui.internal.SonarImages;
import org.sonar.ide.eclipse.wsclient.SonarConnectionTester.ConnectionTestResult;
import org.sonar.wsclient.Host;

import java.lang.reflect.InvocationTargetException;

public class ServerLocationWizardPage extends WizardPage {
  private final Host host;

  private Text serverUrlText;
  private Text serverUsernameText;
  private Text serverPasswordText;
  private Button testConnectionButton;
  private IStatus status;

  public ServerLocationWizardPage() {
    this(new Host("http://localhost:9000"));
  }

  public ServerLocationWizardPage(Host host) {
    super("server_location_page", "Sonar Server Configuration", SonarImages.SONARWIZBAN_IMG);
    this.host = host;
  }

  /**
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    layout.numColumns = 2;
    layout.verticalSpacing = 9;
    Label label = new Label(container, SWT.NULL);
    label.setText(Messages.ServerLocationWizardPage_label_host);
    serverUrlText = new Text(container, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    serverUrlText.setLayoutData(gd);
    serverUrlText.addModifyListener(new ModifyListener() {
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
    testConnectionButton = new Button(container, SWT.PUSH);
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
          getWizard().getContainer().run(true, true, new IRunnableWithProgress() {

            public void run(IProgressMonitor monitor) {
              monitor.beginTask("Testing", IProgressMonitor.UNKNOWN);
              try {
                ConnectionTestResult result = SonarCorePlugin.getServerConnectionTester().testSonar(serverUrl, username, password);
                switch (result) {
                  case OK:
                    status = new Status(IStatus.OK, ISonarConstants.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_connected);
                    break;
                  case AUTHENTICATION_ERROR:
                    status = new Status(IStatus.ERROR, ISonarConstants.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_authentication_error);
                    break;
                  case CONNECT_ERROR:
                    status = new Status(IStatus.ERROR, ISonarConstants.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_connection_error);
                    break;
                }
              } finally {
                monitor.done();
              }
            }
          });
        } catch (InvocationTargetException e1) {
          LoggerFactory.getLogger(getClass()).error(e1.getMessage(), e1);
          status = new Status(IStatus.ERROR, ISonarConstants.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_error);
        } catch (InterruptedException e1) {
          LoggerFactory.getLogger(getClass()).error(e1.getMessage(), e1);
          status = new Status(IStatus.ERROR, ISonarConstants.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_error);
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

    initialize();
    dialogChanged();
    setControl(container);
  }

  private void initialize() {
    serverUrlText.setText(StringUtils.defaultString(host.getHost()));
    serverUsernameText.setText(StringUtils.defaultString(host.getUsername()));
    serverPasswordText.setText(StringUtils.defaultString(host.getPassword()));
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
