/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.wizards;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.wsclient.Host;

/**
 * @author Jérémie Lagarde
 */
public class ServerLocationWizardPage extends WizardPage {

  private Text serverUrlText;
  private Text serverUsernameText;
  private Text serverPasswordText;
  private String defaultServerUrl;
  private Button testConnectionButton;

  public ServerLocationWizardPage(String pageName) {
    super(pageName);
  }

  public ServerLocationWizardPage(String pageName, String title, ImageDescriptor titleImage, String defaultServerUrl) {
    super(pageName, title, titleImage);
    this.defaultServerUrl = defaultServerUrl;
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
                if (SonarPlugin.getServerManager().testSonar(serverUrl, username, password)) {
                  status = new Status(Status.OK, SonarPlugin.PLUGIN_ID, Messages.getString("test.server.dialog.msg"));
                } else {
                  status = new Status(Status.ERROR, SonarPlugin.PLUGIN_ID, Messages.getString("test.server.dialog.error"));
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
          // TODO Auto-generated catch block
          e1.printStackTrace();
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

  private IStatus status;

  private void initialize() {
    Host host = SonarPlugin.getServerManager().findServer(defaultServerUrl);
    if (host != null) {
      serverUrlText.setText(host.getHost());
      if (StringUtils.isNotBlank(host.getUsername()))
        serverUsernameText.setText(host.getUsername());
      if (StringUtils.isNotBlank(host.getPassword()))
        serverPasswordText.setText(host.getPassword());
    } else {
      if (defaultServerUrl != null)
        serverUrlText.setText(defaultServerUrl);
    }
  }

  /**
   * Uses the standard container selection dialog to choose the new value for the container field.
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
    return StringUtils.removeEnd(serverUrlText.getText(), "/");
  }

  public String getUsername() {
    return serverUsernameText.getText();
  }

  public String getPassword() {
    return serverPasswordText.getText();
  }
}
