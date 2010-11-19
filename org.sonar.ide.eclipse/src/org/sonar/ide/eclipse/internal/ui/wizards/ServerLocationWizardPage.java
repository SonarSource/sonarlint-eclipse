/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogPage;
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
import org.sonar.ide.eclipse.internal.core.ISonarConstants;
import org.sonar.ide.eclipse.internal.core.SonarLogger;
import org.sonar.ide.eclipse.internal.ui.Messages;
import org.sonar.ide.eclipse.internal.ui.SonarImages;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.wsclient.Host;

public class ServerLocationWizardPage extends WizardPage {

  private Text serverUrlText;
  private Text serverUsernameText;
  private Text serverPasswordText;
  private Button testConnectionButton;

  private Host host;

  public ServerLocationWizardPage() {
    this(new Host("http://localhost:9000"));
  }

  public ServerLocationWizardPage(Host host) {
    super("server_location_page", "Sonar Server Configuration", SonarImages.SONARWIZBAN_IMG);
    this.host = host;
  }

  /**
   * @see IDialogPage#createControl(Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    layout.numColumns = 2;
    layout.verticalSpacing = 9;
    Label label = new Label(container, SWT.NULL);
    label.setText(Messages.getString("pref.project.label.host")); //$NON-NLS-1$
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
    labelUsername.setText(Messages.getString("pref.project.label.username")); //$NON-NLS-1$
    serverUsernameText = new Text(container, SWT.BORDER | SWT.SINGLE);
    serverUsernameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // Sonar Server password
    Label labelPassword = new Label(container, SWT.NULL);
    labelPassword.setText(Messages.getString("pref.project.label.password")); //$NON-NLS-1$
    serverPasswordText = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
    serverPasswordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    // Sonar test connection button
    testConnectionButton = new Button(container, SWT.PUSH);
    testConnectionButton.setText(Messages.getString("action.testconnection.server")); //$NON-NLS-1$
    testConnectionButton.setToolTipText(Messages.getString("action.testconnection.server.desc")); //$NON-NLS-1$
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

            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
              monitor.beginTask("Testing", IProgressMonitor.UNKNOWN);
              try {
                if (SonarUiPlugin.getServerManager().testSonar(serverUrl, username, password)) {
                  status = new Status(Status.OK, ISonarConstants.PLUGIN_ID, Messages.getString("test.server.dialog.msg"));
                } else {
                  status = new Status(Status.ERROR, ISonarConstants.PLUGIN_ID, Messages.getString("test.server.dialog.error"));
                }
              } catch (CoreException e) {
                status = e.getStatus();
              } catch (OperationCanceledException e) {
                status = Status.CANCEL_STATUS;
                throw new InterruptedException();
              } catch (Exception e) {
                throw new InvocationTargetException(e);
              } finally {
                monitor.done();
              }
            }
          });
        } catch (InvocationTargetException e1) {
          SonarLogger.log(e1);
        } catch (InterruptedException e1) {
          // canceled
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

  private IStatus status;

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
