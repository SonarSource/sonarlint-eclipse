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
package org.sonarlint.eclipse.ui.internal.bind.wizard;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.viewers.ViewersObservables;
import org.eclipse.jface.databinding.wizard.WizardPageSupport;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.ide.IDE.SharedImages;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class ProjectsSelectionWizardPage extends AbstractProjectBindingWizardPage {

  private static final class SonarLintProjectLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
      ISonarLintProject current = (ISonarLintProject) element;
      return current.getName();
    }

    @Override
    public Image getImage(Object element) {
      return PlatformUI.getWorkbench().getSharedImages().getImage(SharedImages.IMG_OBJ_PROJECT);
    }
  }

  private TableViewer projectsViewer;
  private Button btnRemove;

  private Binding projectsBinding;
  private IObservableValue observableInput;

  public ProjectsSelectionWizardPage(ProjectBindingModel model) {
    super("projects_page", "Select projects to bind", model, 2);
  }

  @Override
  protected void doCreateControl(Composite container) {
    projectsViewer = new TableViewer(container, SWT.MULTI | SWT.BORDER);
    projectsViewer.addSelectionChangedListener(event -> updateButtonsState());
    projectsViewer.setContentProvider(new ArrayContentProvider());
    projectsViewer.setLabelProvider(new SonarLintProjectLabelProvider());
    projectsViewer.setInput(new ArrayList<>(model.getEclipseProjects()));
    projectsViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));

    DataBindingContext dbc = new DataBindingContext();
    observableInput = ViewersObservables.observeInput(projectsViewer);
    projectsBinding = dbc.bindValue(
      observableInput,
      BeanProperties.value(ProjectBindingModel.class, ProjectBindingModel.PROPERTY_PROJECTS)
        .observe(model),
      new UpdateValueStrategy().setBeforeSetValidator(new MandatoryProjectsValidator("You must select at least one project")), null);
    ControlDecorationSupport.create(projectsBinding, SWT.LEFT | SWT.TOP);

    WizardPageSupport.create(this, dbc);

    Button btnAddProject = new Button(container, SWT.NONE);
    btnAddProject.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        addSonarLintProjectsAction();
      }

    });
    btnAddProject.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
    btnAddProject.setText("Add...");

    btnRemove = new Button(container, SWT.NONE);
    btnRemove.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        List<ISonarLintProject> newInput = new ArrayList<>((List) projectsViewer.getInput());
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
    List<ISonarLintProject> projects = ProjectsProviderUtils.allProjects()
      .stream()
      .filter(p -> !((List) projectsViewer.getInput()).contains(p))
      .sorted(comparing(ISonarLintProject::getName))
      .collect(toList());
    ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new SonarLintProjectLabelProvider());
    dialog.setElements(projects.toArray());
    dialog.setMessage("Select projects to add:");
    dialog.setTitle("Project selection");
    dialog.setHelpAvailable(false);
    dialog.setMultipleSelection(true);
    if (dialog.open() == Window.OK) {
      List<ISonarLintProject> newInput = new ArrayList<>((List) projectsViewer.getInput());
      asList(dialog.getResult()).forEach(o -> newInput.add((ISonarLintProject) o));
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
