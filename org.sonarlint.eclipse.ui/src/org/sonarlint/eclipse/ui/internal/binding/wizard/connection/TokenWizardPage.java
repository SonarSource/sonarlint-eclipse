/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding.wizard.connection;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel.ConnectionType;
import org.sonarlint.eclipse.ui.internal.util.DisplayUtils;
import org.sonarlint.eclipse.ui.internal.util.wizard.BeanPropertiesCompat;
import org.sonarlint.eclipse.ui.internal.util.wizard.WidgetPropertiesCompat;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.auth.HelpGenerateUserTokenParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.auth.HelpGenerateUserTokenResponse;

public class TokenWizardPage extends AbstractServerConnectionWizardPage {

  private Text connectionTokenText;

  private Binding tokenTextBinding;

  public TokenWizardPage(ServerConnectionModel model) {
    super("connection_token_page", null, model, 3);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doCreateControl(Composite container) {
    // When using the Connection suggestion we only use this page but for that we have to display the information
    // regarding the SonarQube URL / SonarCloud organization and project key
    if (model.isFromConnectionSuggestion()) {
      var connectionLabel = new Label(container, SWT.WRAP);

      var organization = model.getOrganization();
      if (organization == null) {
        connectionLabel.setText("The token is used for setting up the connection to the SonarQube server URL '"
          + model.getServerUrl() + "'.");
      } else {
        connectionLabel.setText("The token is used for setting up the connection with SonarCloud to the organization '"
          + organization + "'.");
      }

      var gd = new GridData(GridData.FILL_HORIZONTAL);
      gd.horizontalSpan = 3;
      connectionLabel.setLayoutData(gd);
    }

    createTokenField(container);
    createOpenSecurityPageButton(container);

    var dataBindingContext = new DataBindingContext();
    tokenTextBinding = dataBindingContext.bindValue(
      WidgetPropertiesCompat.text(SWT.Modify).observe(connectionTokenText),
      BeanPropertiesCompat.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_USERNAME)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(
        new MandatoryStringValidator("You must provide an authentication token")),
      null);
    ControlDecorationSupport.create(tokenTextBinding, SWT.LEFT | SWT.TOP);

    WizardPageSupport.create(this, dataBindingContext);
  }

  private void createTokenField(final Composite container) {
    var labelUsername = new Label(container, SWT.NULL);
    labelUsername.setText("Token:");
    connectionTokenText = new Text(container, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
    var gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalIndent = 10;
    connectionTokenText.setLayoutData(gd);
  }

  private void createOpenSecurityPageButton(Composite container) {
    var button = new Button(container, SWT.PUSH);
    button.setImage(SonarLintImages.IMG_OPEN_EXTERNAL);
    button.setText(Messages.ServerLocationWizardPage_action_token);
    button.setToolTipText(Messages.ServerLocationWizardPage_action_token_tooltip);
    button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

    button.addListener(SWT.Selection, e -> openTokenCreationPage());
  }

  private void openTokenCreationPage() {
    try {
      var job = new GenerateTokenJob(model.getServerUrl(), model.getConnectionType() == ConnectionType.SONARCLOUD);
      getContainer().run(true, true, job);
      var response = job.getResponse();
      var token = response.getToken();
      if (token != null) {
        handleReceivedToken(token);
      }
    } catch (InterruptedException canceled) {
      // Cancelled
    } catch (InvocationTargetException e) {
      SonarLintLogger.get().error("Unable to generate token", e);
    }
  }

  private void handleReceivedToken(String token) {
    DisplayUtils.asyncExec(() -> {
      connectionTokenText.setText(token);
      DisplayUtils.bringToFront();
    });
  }

  static final class GenerateTokenJob implements IRunnableWithProgress {

    private final String serverUrl;
    private final boolean isSonarCloud;
    private HelpGenerateUserTokenResponse response;

    public GenerateTokenJob(String serverUrl, boolean isSonarCloud) {
      this.serverUrl = serverUrl;
      this.isSonarCloud = isSonarCloud;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
      monitor.beginTask("Token generation", IProgressMonitor.UNKNOWN);
      try {
        var params = new HelpGenerateUserTokenParams(serverUrl, isSonarCloud);
        var future = SonarLintBackendService.get().getBackend().getConnectionService().helpGenerateUserToken(params);
        this.response = JobUtils.waitForFutureInIRunnableWithProgress(monitor, future);
      } finally {
        monitor.done();
      }
    }

    public HelpGenerateUserTokenResponse getResponse() {
      return response;
    }

  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      setTitle(model.getConnectionType() == ConnectionType.SONARCLOUD ? "SonarCloud User Authentication Token" : "SonarQube User Authentication Token");
      tokenTextBinding.validateTargetToModel();
    }
  }

}
