/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal.server.wizard;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
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
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class ServerLocationWizardPage extends WizardPage {
  private final IServer server;

  private Text serverIdText;
  private Text serverNameText;
  private Text serverUrlText;
  private Text serverUsernameText;
  private Text serverPasswordText;
  private IStatus status;
  private final boolean edit;
  private boolean serverIdManuallyChanged;

  private ModifyListener idModifyListener;

  private final String defaultServerId;

  public ServerLocationWizardPage() {
    this((IServer) null);
  }

  public ServerLocationWizardPage(String defaultServerId) {
    this((IServer) null, defaultServerId);
  }

  public ServerLocationWizardPage(IServer sonarServer) {
    this(sonarServer, null);
  }

  public ServerLocationWizardPage(IServer sonarServer, String defaultServerId) {
    super("server_location_page", "SonarQube Server Configuration", SonarLintImages.IMG_WIZBAN_NEW_SERVER);
    this.edit = sonarServer != null;
    this.server = sonarServer;
    this.defaultServerId = defaultServerId;
  }

  /**
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(Composite)
   */
  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    container.setLayout(layout);
    layout.numColumns = 2;
    layout.verticalSpacing = 9;

    // SonarQube Server ID
    boolean isEditable = !edit;
    Label labelId = new Label(container, SWT.NULL);
    labelId.setText(Messages.ServerLocationWizardPage_label_id);
    serverIdText = new Text(container, isEditable ? (SWT.BORDER | SWT.SINGLE) : (SWT.BORDER | SWT.READ_ONLY));
    GridData gdId = new GridData(GridData.FILL_HORIZONTAL);
    serverIdText.setLayoutData(gdId);
    serverIdText.setEnabled(isEditable);
    serverIdText.setEditable(isEditable);
    idModifyListener = new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        serverIdManuallyChanged = true;
        dialogChanged();
      }
    };
    serverIdText.addModifyListener(idModifyListener);

    // SonarQube Server URL
    Label labelUrl = new Label(container, SWT.NULL);
    labelUrl.setText(Messages.ServerLocationWizardPage_label_host);
    serverUrlText = new Text(container, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    serverUrlText.setLayoutData(gd);
    serverUrlText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        if (!edit && !serverIdManuallyChanged) {
          try {
            URL url = new URL(serverUrlText.getText());
            serverIdText.removeModifyListener(idModifyListener);
            serverIdText.setText(StringUtils.substringBefore(url.getHost(), "."));
            serverIdText.addModifyListener(idModifyListener);
          } catch (MalformedURLException e1) {
            // Ignore
          }
        }
        dialogChanged();
      }
    });

    // Sonar Server Name
    Label labelName = new Label(container, SWT.NULL);
    labelName.setText(Messages.ServerLocationWizardPage_label_name);
    serverNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
    serverNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

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

    // Test connection button
    createTestConnectionButton(container);

    initialize();
    dialogChanged();
    Dialog.applyDialogFont(container);
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
        try {
          ServerConnectionTestJob testJob = new ServerConnectionTestJob(getServer(), getUsername(), getPassword());
          getWizard().getContainer().run(true, true, testJob);
          status = testJob.getStatus();
        } catch (OperationCanceledException e1) {
          status = Status.CANCEL_STATUS;
        } catch (Exception e1) {
          status = new Status(IStatus.ERROR, SonarLintUiPlugin.PLUGIN_ID, Messages.ServerLocationWizardPage_msg_error + " " + e1.getMessage(), e1);
        }
        getWizard().getContainer().updateButtons();

        String message = status.getMessage();
        if (status.getSeverity() == IStatus.OK) {
          setMessage(message, IMessageProvider.INFORMATION);
        } else {
          setMessage(message, IMessageProvider.ERROR);
        }
      }
    });
  }

  private void initialize() {
    if (edit) {
      serverIdText.setText(StringUtils.defaultString(server.getId()));
      serverUrlText.setText(StringUtils.defaultString(server.getHost()));
      serverNameText.setText(StringUtils.defaultString(server.getName()));
      serverUsernameText.setText(StringUtils.defaultString(ServersManager.getUsername(server)));
      serverPasswordText.setText(StringUtils.defaultString(ServersManager.getPassword(server)));
    } else {
      if (defaultServerId != null) {
        serverIdText.setText(defaultServerId);
        serverIdManuallyChanged = true;
      } else {
        serverIdManuallyChanged = false;
      }
      serverUrlText.setText("https://");
    }
    serverUrlText.setFocus();
    serverUrlText.setSelection(serverUrlText.getText().length());
  }

  private void dialogChanged() {
    if (StringUtils.isBlank(getServerUrl())) {
      updateStatus("Server url must be specified");
      return;
    }
    if (StringUtils.isBlank(getServerId())) {
      updateStatus("Server id must be specified");
      return;
    }
    if (!edit && ServersManager.getInstance().getServer(getServerId()) != null) {
      updateStatus("Server id already exists");
      return;
    }
    try {
      getServer();
    } catch (Exception e) {
      updateStatus(e.getMessage());
      return;
    }
    updateStatus(null);
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

  public String getServerId() {
    return serverIdText.getText();
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

  public String getServerName() {
    return StringUtils.defaultIfBlank(serverNameText.getText(), getServerId());
  }

  public IServer getServer() {
    return new Server(getServerId(), getServerName(), getServerUrl(), StringUtils.isNotBlank(getUsername()) || StringUtils.isNotBlank(getPassword()));
  }

}
