/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteOrganization;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;

public class OrganizationWizardPage extends WizardPage {

  private final ServerConnectionModel model;

  public OrganizationWizardPage(ServerConnectionModel model) {
    super("server_organization_page", "SonarQube Server Organization", SonarLintImages.IMG_WIZBAN_NEW_SERVER);
    this.model = model;
  }

  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    container.setLayout(layout);

    GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    container.setLayoutData(layoutData);

    createOrganizationField(container);
    setControl(container);
  }

  private void createOrganizationField(final Composite container) {
    Label labelOrganization = new Label(container, SWT.NULL);
    labelOrganization.setText("Organization:");
    Text organizationText = new Text(container, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    organizationText.setLayoutData(gd);

    DataBindingContext dbc = new DataBindingContext();
    dbc.bindValue(
      WidgetProperties.text(SWT.Modify).observe(organizationText),
      BeanProperties.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_ORGANIZATION)
        .observe(model));

    WizardPageSupport.create(this, dbc);

    ContentProposalAdapter contentProposalAdapter = new ContentAssistCommandAdapter(
      organizationText,
      new TextContentAdapter(),
      new OrganizationProvider(model, this),
      ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
      null,
      true);
    contentProposalAdapter.setAutoActivationCharacters(null);
    contentProposalAdapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
    contentProposalAdapter.setFilterStyle(ContentProposalAdapter.FILTER_NONE);
    contentProposalAdapter.setAutoActivationDelay(100);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      tryLoadOrganizations();
      TextSearchIndex<RemoteOrganization> organizationsIndex = model.getOrganizationsIndex();
      if (organizationsIndex != null && organizationsIndex.size() <= 1) {
        CustomWizardDialog customWizardDialog = (CustomWizardDialog) getContainer();
        if (!customWizardDialog.isMovingBackward()) {
          // Skip organization selection
          customWizardDialog.showPage(getNextPage());
          // Ensure that when pressing back on next page we don't return on organization page
          getNextPage().setPreviousPage(getPreviousPage());
        }
      }
    }
  }

  private void tryLoadOrganizations() {
    try {
      getContainer().run(true, true, new IRunnableWithProgress() {

        @Override
        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
          try {
            model.setOrganizationsIndex(Server.getOrganizationsIndex(model.getServerUrl(), model.getUsername(), model.getPassword(), monitor));
            setMessage(null);
          } finally {
            monitor.done();
          }
        }
      });
    } catch (InvocationTargetException e) {
      SonarLintLogger.get().debug("Unable to download organizations", e.getCause());
      setMessage(e.getCause().getMessage(), IMessageProvider.ERROR);
      model.setOrganizationsIndex(null);
    } catch (InterruptedException e) {
      model.setOrganizationsIndex(null);
    }
  }

}
