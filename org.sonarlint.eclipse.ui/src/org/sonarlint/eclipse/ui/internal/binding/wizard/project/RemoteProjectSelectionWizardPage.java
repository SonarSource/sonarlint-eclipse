/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding.wizard.project;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.MandatoryStringValidator;
import org.sonarlint.eclipse.ui.internal.util.wizard.BeanPropertiesCompat;
import org.sonarlint.eclipse.ui.internal.util.wizard.WidgetPropertiesCompat;

public class RemoteProjectSelectionWizardPage extends AbstractProjectBindingWizardPage {

  private Binding projectTextBinding;

  public RemoteProjectSelectionWizardPage(ProjectBindingModel model) {
    super("remote_project_page", "Choose the SonarQube/SonarCloud project", model, 1);
  }

  @Override
  protected void doCreateControl(Composite container) {
    var projectKeyText = new Text(container, SWT.BORDER | SWT.SINGLE);
    projectKeyText.setMessage("Start typing to search for your project by name or enter the project key");
    var gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalIndent = 10;
    projectKeyText.setLayoutData(gd);

    var dataBindingContext = new DataBindingContext();
    projectTextBinding = dataBindingContext.bindValue(
      WidgetPropertiesCompat.text(SWT.Modify).observe(projectKeyText),
      BeanPropertiesCompat.value(ProjectBindingModel.class, ProjectBindingModel.PROPERTY_REMOTE_PROJECT_KEY)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(new MandatoryStringValidator("You must select a project key")),
      null);
    ControlDecorationSupport.create(projectTextBinding, SWT.LEFT | SWT.TOP);

    WizardPageSupport.create(this, dataBindingContext);

    var contentProposalAdapter = new ContentAssistCommandAdapter(
      projectKeyText,
      new TextContentAdapter(),
      new RemoteProjectProvider(model, this),
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
      projectTextBinding.validateTargetToModel();
    }
  }

}
