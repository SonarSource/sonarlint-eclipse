/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.ProjectToBindSelectionDialog;
import org.sonarlint.eclipse.ui.internal.binding.SonarLintProjectLabelProvider;
import org.sonarlint.eclipse.ui.internal.util.wizard.BeanPropertiesCompat;
import org.sonarlint.eclipse.ui.internal.util.wizard.ViewersObservablesCompat;

public class ProjectsSelectionWizardPage extends AbstractProjectBindingWizardPage {

  private TableViewer projectsViewer;
  private Button btnRemove;

  private Binding projectsBinding;
  private IObservableValue observableInput;

  public ProjectsSelectionWizardPage(ProjectBindingModel model) {
    super("projects_page", "Select projects to bind", model, 2);
  }

  @Override
  protected void doCreateControl(Composite container) {
    var bindingContainer = new Composite(container, SWT.NONE);
    var layout = new GridLayout();
    layout.numColumns = 1;
    bindingContainer.setLayout(layout);
    var gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = 500;

    var bindingLabel = new Label(bindingContainer, SWT.WRAP);
    bindingLabel.setText("Complete your Connected Mode setup by binding your local project to your SonarQube (Server, "
      + "Cloud) project to benefit from the same rules and settings that are used to inspect the project on the "
      + "server.");
    bindingLabel.setLayoutData(gd);

    var tableContainer = new Composite(bindingContainer, SWT.NONE);
    var tableLayout = new GridLayout();
    tableLayout.numColumns = 2;
    tableContainer.setLayout(tableLayout);
    projectsViewer = new TableViewer(tableContainer, SWT.MULTI | SWT.BORDER);
    projectsViewer.addSelectionChangedListener(event -> updateButtonsState());
    projectsViewer.setContentProvider(new ArrayContentProvider());
    projectsViewer.setLabelProvider(new SonarLintProjectLabelProvider());
    projectsViewer.setInput(new ArrayList<>(model.getEclipseProjects()));
    var gdTable = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3);
    gdTable.widthHint = 500;
    gdTable.heightHint = 200;
    projectsViewer.getTable().setLayoutData(gdTable);

    var dataBindingContext = new DataBindingContext();
    observableInput = ViewersObservablesCompat.observeInput(projectsViewer);
    projectsBinding = dataBindingContext.bindValue(
      observableInput,
      BeanPropertiesCompat.value(ProjectBindingModel.class, ProjectBindingModel.PROPERTY_PROJECTS)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(new MandatoryProjectsValidator("You must select at least one project")), null);
    ControlDecorationSupport.create(projectsBinding, SWT.LEFT | SWT.TOP);

    WizardPageSupport.create(this, dataBindingContext);

    var btnAddProject = new Button(tableContainer, SWT.NONE);
    btnAddProject.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        addSonarLintProjectsAction();
      }

    });
    btnAddProject.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnAddProject.setText("Add...");

    btnRemove = new Button(tableContainer, SWT.NONE);
    btnRemove.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        var newInput = new ArrayList<ISonarLintProject>((List) projectsViewer.getInput());
        newInput.removeAll(getSelectedElements());
        observableInput.setValue(newInput);
      }
    });
    btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnRemove.setText("Remove");

    updateButtonsState();
  }

  private void updateButtonsState() {
    btnRemove.setEnabled(!getSelectedElements().isEmpty());
  }

  private List<ISonarLintProject> getSelectedElements() {
    return ((IStructuredSelection) projectsViewer.getSelection()).toList();
  }

  protected void addSonarLintProjectsAction() {
    var selected = ProjectToBindSelectionDialog.selectProjectsToAdd(getShell(), (List) projectsViewer.getInput());
    if (!selected.isEmpty()) {
      var newInput = new ArrayList<ISonarLintProject>((List) projectsViewer.getInput());
      newInput.addAll(selected);
      observableInput.setValue(newInput);
    }
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (visible) {
      projectsBinding.validateTargetToModel();
    }
  }

}
