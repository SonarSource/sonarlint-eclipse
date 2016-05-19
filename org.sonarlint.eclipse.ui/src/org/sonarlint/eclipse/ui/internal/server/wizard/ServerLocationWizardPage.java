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
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

public class ServerLocationWizardPage extends WizardPage {
  private final IServer server;

  private Text serverIdText;
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
    final FormToolkit toolkit = new FormToolkit(parent.getDisplay());
    parent.addDisposeListener(new DisposeListener() {
      @Override
      public void widgetDisposed(DisposeEvent e) {
        toolkit.dispose();
      }
    });
    final ScrolledForm form = toolkit.createScrolledForm(parent);
    form.setBackground(parent.getBackground());

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.verticalSpacing = 9;
    form.getBody().setLayout(layout);

    GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    form.setLayoutData(layoutData);

    // SonarQube Server URL
    Label labelUrl = new Label(form.getBody(), SWT.NULL);
    labelUrl.setText(Messages.ServerLocationWizardPage_label_host);
    serverUrlText = new Text(form.getBody(), SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    serverUrlText.setLayoutData(gd);
    if (edit) {
      serverUrlText.setText(StringUtils.defaultString(server.getHost()));
    } else {
      serverUrlText.setText("https://");
    }
    serverUrlText.setFocus();
    serverUrlText.setSelection(serverUrlText.getText().length());
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

    createServerIdField(form);

    // Sonar Server Username
    Label labelUsername = new Label(form.getBody(), SWT.NULL);
    labelUsername.setText(Messages.ServerLocationWizardPage_label_username);
    serverUsernameText = new Text(form.getBody(), SWT.BORDER | SWT.SINGLE);
    serverUsernameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    if (edit) {
      serverUsernameText.setText(StringUtils.defaultString(ServersManager.getUsername(server)));
    }

    // Sonar Server password
    Label labelPassword = new Label(form.getBody(), SWT.NULL);
    labelPassword.setText(Messages.ServerLocationWizardPage_label_password);
    serverPasswordText = new Text(form.getBody(), SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
    serverPasswordText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    if (edit) {
      serverPasswordText.setText(StringUtils.defaultString(ServersManager.getPassword(server)));
    }

    // Test connection button
    createTestConnectionButton(form.getBody());

    if (edit) {
      dialogChanged();
    }
    Dialog.applyDialogFont(parent);
    setControl(form.getBody());
  }

  private void createServerIdField(final ScrolledForm form) {
    boolean isEditable = !edit;
    Label labelId = new Label(form.getBody(), SWT.NULL);
    labelId.setText(Messages.ServerLocationWizardPage_label_id);
    serverIdText = new Text(form.getBody(), isEditable ? (SWT.BORDER | SWT.SINGLE) : (SWT.BORDER | SWT.READ_ONLY));
    GridData gdId = new GridData(GridData.FILL_HORIZONTAL);
    serverIdText.setLayoutData(gdId);
    serverIdText.setEnabled(isEditable);
    serverIdText.setEditable(isEditable);
    if (edit) {
      serverIdText.setText(StringUtils.defaultString(server.getId()));
    } else {
      if (defaultServerId != null) {
        serverIdText.setText(defaultServerId);
        serverIdManuallyChanged = true;
      } else {
        serverIdManuallyChanged = false;
      }
    }
    idModifyListener = new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        serverIdManuallyChanged = true;
        dialogChanged();
      }
    };
    serverIdText.addModifyListener(idModifyListener);
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
          ServerConnectionTestJob testJob = new ServerConnectionTestJob(transcientServer(), getUsername(), getPassword());
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

  private void dialogChanged() {
    updateStatus(ServersManager.getInstance().validate(getServerId(), getServerUrl(), edit));
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

  private IServer transcientServer() {
    return ServersManager.getInstance().create(getServerId(), getServerUrl(), getUsername(), getPassword());
  }

}
