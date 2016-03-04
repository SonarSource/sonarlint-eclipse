/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.ui.internal.link;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.bindings.keys.KeyLookupFactory;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public class LinkProjectsPage extends WizardPage {

  private final List<IProject> projects;
  private TableViewer viewer;
  private final Collection<IServer> sonarServers;

  public LinkProjectsPage(List<IProject> projects) {
    super("linkProjects", "Link with SonarQube", SonarLintImages.SONARWIZBAN_IMG);
    setDescription("Associate Eclipse projects with remote project/module on a SonarQube server");
    this.projects = projects;
    sonarServers = SonarLintCorePlugin.getDefault().getServers();
  }

  @Override
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);

    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    layout.marginWidth = 5;
    container.setLayout(layout);

    // List of projects
    viewer = new TableViewer(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.VIRTUAL);
    viewer.getTable().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 1, 3));

    viewer.getTable().setHeaderVisible(true);

    TableViewerColumn columnProject = new TableViewerColumn(viewer, SWT.LEFT);
    columnProject.getColumn().setText("Project");
    columnProject.getColumn().setWidth(200);

    TableViewerColumn columnSonarProject = new TableViewerColumn(viewer, SWT.LEFT);
    columnSonarProject.getColumn().setText("SonarQube Project");
    columnSonarProject.getColumn().setWidth(600);

    columnSonarProject.setEditingSupport(new ProjectAssociationModelEditingSupport(viewer));

    List<ProjectAssociationModel> list = new ArrayList<>();
    for (IProject project : projects) {
      ProjectAssociationModel sonarProject = new ProjectAssociationModel(project);
      list.add(sonarProject);
    }

    ColumnViewerEditorActivationStrategy activationSupport = createActivationSupport();

    /*
     * Without focus highlighter, keyboard events will not be delivered to
     * ColumnViewerEditorActivationStragety#isEditorActivationEvent(...) (see above)
     */
    FocusCellHighlighter focusCellHighlighter = new FocusCellOwnerDrawHighlighter(viewer);
    TableViewerFocusCellManager focusCellManager = new TableViewerFocusCellManager(viewer, focusCellHighlighter);

    TableViewerEditor.create(viewer, focusCellManager, activationSupport, ColumnViewerEditor.TABBING_VERTICAL
      | ColumnViewerEditor.KEYBOARD_ACTIVATION);

    ViewerSupport.bind(
      viewer,
      new WritableList(list, ProjectAssociationModel.class),
      new IValueProperty[] {BeanProperties.value(ProjectAssociationModel.class, ProjectAssociationModel.PROPERTY_PROJECT_ECLIPSE_NAME),
        BeanProperties.value(ProjectAssociationModel.class, ProjectAssociationModel.PROPERTY_PROJECT_SONAR_FULLNAME)});

    setControl(container);
  }

  private ColumnViewerEditorActivationStrategy createActivationSupport() {
    ColumnViewerEditorActivationStrategy activationSupport = new ColumnViewerEditorActivationStrategy(viewer) {
      @Override
      protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
        return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
          || event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION
          || event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC
          || event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == KeyLookupFactory
            .getDefault().formalKeyLookup(IKeyLookup.F2_NAME);
      }
    };
    activationSupport.setEnableEditorActivationWithKeyboard(true);
    return activationSupport;
  }

  private class ProjectAssociationModelEditingSupport extends EditingSupport {

    SonarSearchEngineProvider contentProposalProvider = new SonarSearchEngineProvider(sonarServers, LinkProjectsPage.this);

    public ProjectAssociationModelEditingSupport(TableViewer viewer) {
      super(viewer);
    }

    @Override
    protected boolean canEdit(Object element) {
      return element instanceof ProjectAssociationModel;
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
      return new TextCellEditorWithContentProposal(viewer.getTable(), contentProposalProvider, (ProjectAssociationModel) element);
    }

    @Override
    protected Object getValue(Object element) {
      return StringUtils.trimToEmpty(((ProjectAssociationModel) element).getSonarProjectName());
    }

    @Override
    protected void setValue(Object element, Object value) {
      // Don't set value as the model was already updated in the text adapter
    }

  }

  /**
   * Update all Eclipse projects when an association was provided:
   *   - enable Sonar nature
   *   - update sonar URL / key
   *   - refresh issues if necessary
   * @return
   */
  public boolean finish() {
    final ProjectAssociationModel[] projectAssociations = getProjects();
    for (ProjectAssociationModel projectAssociation : projectAssociations) {
      if (StringUtils.isNotBlank(projectAssociation.getKey())) {
        try {
          boolean changed = false;
          IProject project = projectAssociation.getProject();
          SonarLintProject sonarProject = SonarLintProject.getInstance(project);
          if (!projectAssociation.getServerId().equals(sonarProject.getServerId())) {
            sonarProject.setServerId(projectAssociation.getServerId());
            changed = true;
          }
          if (!projectAssociation.getKey().equals(sonarProject.getModuleKey())) {
            sonarProject.setModuleKey(projectAssociation.getKey());
            changed = true;
          }
          if (changed) {
            sonarProject.save();
          }
          if (changed) {
            sonarProject.sync();
          }
        } catch (Exception e) {
          SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
          return false;
        }
      }
    }
    return true;
  }

  private ProjectAssociationModel[] getProjects() {
    WritableList projectAssociations = (WritableList) viewer.getInput();
    return (ProjectAssociationModel[]) projectAssociations.toArray(new ProjectAssociationModel[projectAssociations.size()]);
  }

}
