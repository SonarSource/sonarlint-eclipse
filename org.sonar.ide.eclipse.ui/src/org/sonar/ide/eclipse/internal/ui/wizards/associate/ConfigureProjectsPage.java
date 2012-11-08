/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.ide.eclipse.internal.ui.wizards.associate;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.bindings.keys.KeyLookupFactory;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.core.resources.ProjectProperties;
import org.sonar.ide.eclipse.internal.ui.SonarImages;
import org.sonar.ide.eclipse.internal.ui.actions.ToggleNatureAction;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class ConfigureProjectsPage extends WizardPage {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigureProjectsPage.class);

  private final List<IProject> projects;
  private TableViewer viewer;
  private final List<Host> hosts;
  private boolean alreadyRun = false;

  public ConfigureProjectsPage(List<IProject> projects) {
    super("configureProjects", "Associate with Sonar", SonarImages.SONARWIZBAN_IMG);
    setDescription("Select projects to add Sonar capability.");
    this.projects = projects;
    hosts = SonarCorePlugin.getServersManager().getHosts();
  }

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
    columnSonarProject.getColumn().setText("Sonar project");
    columnSonarProject.getColumn().setWidth(600);

    columnSonarProject.setEditingSupport(new ProjectAssociationModelEditingSupport(viewer));

    List<ProjectAssociationModel> list = Lists.newArrayList();
    for (IProject project : projects) {
      ProjectAssociationModel sonarProject = new ProjectAssociationModel(project);
      list.add(sonarProject);
    }

    ColumnViewerEditorActivationStrategy activationSupport = new ColumnViewerEditorActivationStrategy(viewer) {
      protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
        return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
          || event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION
          || event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC
          || (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == KeyLookupFactory
              .getDefault().formalKeyLookup(IKeyLookup.F2_NAME));
      }
    };
    activationSupport.setEnableEditorActivationWithKeyboard(true);

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

    getShell().addShellListener(new ShellAdapter() {
      @Override
      public void shellActivated(ShellEvent shellevent) {
        if (!alreadyRun) {
          alreadyRun = true;
          try {
            if (hosts.isEmpty()) {
              setMessage("Please configure a sonar server first", IMessageProvider.ERROR);
            } else {
              getWizard().getContainer().run(true, false, new AssociateProjects(hosts, getProjects()));
            }
            setMessage("", IMessageProvider.NONE);
          } catch (InvocationTargetException ex) {
            LOG.error(ex.getMessage(), ex);
            if (ex.getTargetException() instanceof ConnectionException) {
              setMessage("Unable to connect to " + "FIXME" + ". Check your connection settings.", IMessageProvider.ERROR);
            } else {
              setMessage("Error: " + ex.getMessage(), IMessageProvider.ERROR);
            }
          } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            setMessage("Error: " + ex.getMessage(), IMessageProvider.ERROR);
          }
        }
      }
    });

    setControl(container);
  }

  private class ProjectAssociationModelEditingSupport extends EditingSupport {

    SonarSearchEngineProvider contentProposalProvider = new SonarSearchEngineProvider(hosts);

    public ProjectAssociationModelEditingSupport(TableViewer viewer) {
      super(viewer);
    }

    protected boolean canEdit(Object element) {
      return (element instanceof ProjectAssociationModel);
    }

    protected CellEditor getCellEditor(Object element) {
      return new TextCellEditorWithContentProposal(viewer.getTable(), contentProposalProvider, null, null, ((ProjectAssociationModel) element));
    }

    protected Object getValue(Object element) {
      return StringUtils.trimToEmpty(((ProjectAssociationModel) element).getSonarProjectName());
    }

    protected void setValue(Object element, Object value) {
      // Don't set value as the model was already updated in the text adapter
    }

  }

  String errorMessage;

  public boolean finish() {
    final ProjectAssociationModel[] projects = getProjects();
    for (ProjectAssociationModel projectAssociation : projects) {
      if (StringUtils.isNotBlank(projectAssociation.getKey())) {
        try {
          IProject project = projectAssociation.getProject();
          ProjectProperties properties = ProjectProperties.getInstance(project);
          properties.setUrl(projectAssociation.getUrl());
          properties.setKey(projectAssociation.getKey());
          properties.save();
          ToggleNatureAction.enableNature(project);
        } catch (CoreException e) {
          LOG.error(e.getMessage(), e);
          return false;
        }
      }
    }
    return true;
  }

  private ProjectAssociationModel[] getProjects() {
    WritableList projects = ((WritableList) viewer.getInput());
    return (ProjectAssociationModel[]) projects.toArray(new ProjectAssociationModel[projects.size()]);
  }

  public class AssociateProjects implements IRunnableWithProgress {

    private final List<Host> hosts;
    private final ProjectAssociationModel[] projects;

    public AssociateProjects(List<Host> hosts, ProjectAssociationModel[] projects) {
      Assert.isNotNull(hosts);
      Assert.isNotNull(projects);
      this.hosts = hosts;
      this.projects = projects;
    }

    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
      monitor.beginTask("Associating Sonar projects", IProgressMonitor.UNKNOWN);
      for (Host host : hosts) {
        String url = host.getHost();
        ResourceQuery query = new ResourceQuery().setScopes(Resource.SCOPE_SET).setQualifiers(Resource.QUALIFIER_PROJECT,
            Resource.QUALIFIER_MODULE);
        Sonar sonar = SonarCorePlugin.getServersManager().getSonar(url);
        List<Resource> resources = sonar.findAll(query);
        for (ProjectAssociationModel sonarProject : projects) {
          if (StringUtils.isBlank(sonarProject.getKey())) {// Not associated yet
            for (Resource resource : resources) {
              if (resource.getKey().contains(sonarProject.getEclipseName())) {
                sonarProject.associate(host.getHost(), resource.getName(), resource.getKey());
              }
            }
          }
        }
      }
      monitor.done();
    }

  }

}
