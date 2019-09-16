/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

public class OrganizationWizardPage extends AbstractServerConnectionWizardPage {

  private Binding orgaTextBinding;

  public OrganizationWizardPage(ServerConnectionModel model) {
    super("server_organization_page", "SonarCloud Organization", model, 2);
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void doCreateControl(Composite container) {
    Label labelOrganization = new Label(container, SWT.NULL);
    labelOrganization.setText("Organization:");
    Text organizationText = new Text(container, SWT.BORDER | SWT.SINGLE);
    organizationText.setMessage("Start typing to search for your organization");
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalIndent = 10;
    organizationText.setLayoutData(gd);

    DataBindingContext dbc = new DataBindingContext();
    orgaTextBinding = dbc.bindValue(
      WidgetProperties.text(SWT.Modify).observe(organizationText),
      BeanProperties.value(ServerConnectionModel.class, ServerConnectionModel.PROPERTY_ORGANIZATION)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(new MandatoryStringValidator("You must select an organization")),
      null);

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
      orgaTextBinding.validateTargetToModel();
    }
  }

}
