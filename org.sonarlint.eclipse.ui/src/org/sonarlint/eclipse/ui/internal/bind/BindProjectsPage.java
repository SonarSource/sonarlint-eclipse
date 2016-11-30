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
package org.sonarlint.eclipse.ui.internal.bind;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.bindings.keys.IKeyLookup;
import org.eclipse.jface.bindings.keys.KeyLookupFactory;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.FocusCellHighlighter;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TableViewerEditor;
import org.eclipse.jface.viewers.TableViewerFocusCellManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.jobs.ProjectUpdateJob;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.IServerLifecycleListener;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintProjectDecorator;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;
import org.sonarlint.eclipse.ui.internal.server.wizard.NewServerLocationWizard;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteModule;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;

public class BindProjectsPage extends WizardPage {

  private final List<IProject> projects;
  private CheckboxTableViewer viewer;
  private Form noServersPage;
  private PageBook book;
  private IServerLifecycleListener serverListener;
  private IServer selectedServer;
  private Composite serverDropDownPage;
  private ComboViewer serverCombo;
  private Button autoBindBtn;
  private Button unassociateBtn;
  private Button checkAll;
  private Composite container;
  private Link updateServerLink;

  public BindProjectsPage(List<IProject> projects) {
    super("bindProjects", "Bind Eclipse projects to SonarQube projects", SonarLintImages.SONARWIZBAN_IMG);
    setDescription(
      "SonarQube is an Open Source platform to manage code quality. "
        + "Bind your Eclipse projects to some SonarQube projects in order to get the same issues in Eclipse and in SonarQube.");
    this.projects = projects;
    if (!projects.isEmpty()) {
      selectedServer = ServersManager.getInstance().getServer(SonarLintProject.getInstance(projects.get(0)).getServerId());
    }
  }

  @Override
  public void dispose() {
    if (serverListener != null) {
      ServersManager.getInstance().removeServerLifecycleListener(serverListener);
    }
  }

  @Override
  public void createControl(Composite parent) {
    container = new Composite(parent, SWT.NONE);

    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    layout.marginHeight = 0;
    layout.marginWidth = 5;
    container.setLayout(layout);

    book = new PageBook(container, SWT.NONE);

    createNoServerForm(book);
    createServerDropDown(book);

    toggleServerPage();

    createCheckUncheckAllCb();

    viewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    viewer.getTable().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 1, 3));

    viewer.getTable().setHeaderVisible(true);

    TableViewerColumn columnProject = new TableViewerColumn(viewer, SWT.LEFT);
    columnProject.getColumn().setText("Eclipse Project");
    columnProject.getColumn().setWidth(200);

    TableViewerColumn columnSonarProject = new TableViewerColumn(viewer, SWT.LEFT);
    columnSonarProject.getColumn().setText("SonarQube Project");
    columnSonarProject.getColumn().setWidth(600);

    columnSonarProject.setEditingSupport(new ProjectAssociationModelEditingSupport(viewer));

    List<ProjectBindModel> list = new ArrayList<>();
    for (IProject project : projects) {
      ProjectBindModel sonarProject = new ProjectBindModel(project);
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
      new WritableList(list, ProjectBindModel.class),
      new IValueProperty[] {BeanProperties.value(ProjectBindModel.class, ProjectBindModel.PROPERTY_PROJECT_ECLIPSE_NAME),
        BeanProperties.value(ProjectBindModel.class, ProjectBindModel.PROPERTY_PROJECT_SONAR_FULLNAME)});

    Composite btnContainer = new Composite(container, SWT.NONE);

    FillLayout btnLayout = new FillLayout();
    btnContainer.setLayout(btnLayout);

    viewer.addSelectionChangedListener(event -> updateState());

    createUnassociateBtn(btnContainer);

    createAutoBindBtn(btnContainer);

    viewer.setAllChecked(true);

    updateState();
    setControl(container);
  }

  private void createAutoBindBtn(Composite btnContainer) {
    autoBindBtn = new Button(btnContainer, SWT.PUSH);
    autoBindBtn.setText("Auto bind selected projects");
    autoBindBtn.addListener(SWT.Selection, event -> {
      TextSearchIndex<RemoteModule> moduleIndex = selectedServer.getModuleIndex();
      for (Object object : viewer.getCheckedElements()) {
        ProjectBindModel bind = (ProjectBindModel) object;
        List<RemoteModule> results = moduleIndex.search(bind.getEclipseName());
        if (!results.isEmpty()) {
          bind.associate(selectedServer.getId(), results.get(0).getKey());
        } else {
          bind.setAutoBindFailed(true);
        }
      }
    });
  }

  private void createUnassociateBtn(Composite btnContainer) {
    unassociateBtn = new Button(btnContainer, SWT.PUSH);
    unassociateBtn.setText("Unbind selected projects");
    unassociateBtn.setEnabled(viewer.getCheckedElements().length > 0);
    unassociateBtn.addListener(SWT.Selection, event -> {
      for (Object object : viewer.getCheckedElements()) {
        ProjectBindModel bind = (ProjectBindModel) object;
        bind.unassociate();
      }
    });
  }

  private void createCheckUncheckAllCb() {
    checkAll = new Button(container, SWT.CHECK);
    checkAll.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(SelectionEvent e) {
        viewer.setAllChecked(checkAll.getSelection());
        updateState();
      }

    });
  }

  private void createServerDropDown(Composite parent) {
    serverDropDownPage = new Composite(parent, SWT.NONE);
    GridData layoutData = new GridData();
    layoutData.horizontalSpan = 2;
    serverDropDownPage.setLayoutData(layoutData);

    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    layout.marginHeight = 0;
    layout.marginWidth = 5;
    serverDropDownPage.setLayout(layout);

    Label labelField = new Label(serverDropDownPage, SWT.NONE);
    labelField.setText("Select a SonarQube server: ");
    serverCombo = new ComboViewer(serverDropDownPage, SWT.READ_ONLY);

    serverCombo.setContentProvider(ArrayContentProvider.getInstance());

    serverCombo.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        IServer current = (IServer) element;
        return current.getId();
      }
    });

    updateServerLink = new Link(serverDropDownPage, SWT.NONE);
    updateServerLink.setText("<a>Refresh project list</a>");
    updateServerLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        updateSelectedServer();
      }
    });

    serverListener = new ServerChangeListener();
    ServersManager.getInstance().addServerLifecycleListener(serverListener);

    /* within the selection event, tell the object it was selected */
    serverCombo.addSelectionChangedListener(event -> {
      IStructuredSelection selection = (IStructuredSelection) event.getSelection();
      selectedServer = (IServer) selection.getFirstElement();
      serverCombo.refresh();
      updateState();
    });
  }

  private void updateSelectedServer() {
    updateServerLink.setEnabled(false);
    try {
      final IServer server = (IServer) ((IStructuredSelection) serverCombo.getSelection()).getFirstElement();
      getContainer().run(true, true, monitor -> {
        try {
          server.updateStorage(monitor);
        } finally {
          Display.getDefault().asyncExec(this::updateState);
        }
      });
    } catch (InvocationTargetException ex) {
      throw new IllegalStateException(ex);
    } catch (InterruptedException e1) {
      // Job cancelled, ignore
    }
  }

  private void updateState() {
    if (viewer == null) {
      return;
    }
    updateServerLink.setVisible(selectedServer != null);
    updateServerLink.setEnabled(selectedServer != null && !selectedServer.isUpdating());
    boolean hasSelected = viewer.getCheckedElements().length > 0;
    checkAll.setSelection(hasSelected);
    checkAll.setGrayed(viewer.getCheckedElements().length < projects.size());
    checkAll.setText(hasSelected ? "Unselect all" : "Select all");
    unassociateBtn.setEnabled(hasSelected);
    if (selectedServer != null && !selectedServer.isStorageUpdated()) {
      setMessage("No data for the selected server", IMessageProvider.WARNING);
    } else {
      setMessage(null);
    }
    if (autoBindBtn != null) {
      autoBindBtn.setEnabled(hasSelected && selectedServer != null && selectedServer.isStorageUpdated());
    }
    container.layout(true, true);
  }

  private void createNoServerForm(Composite parent) {
    FormToolkit toolkit = new FormToolkit(parent.getDisplay());
    noServersPage = toolkit.createForm(book);
    GridData layoutData = new GridData();
    layoutData.horizontalSpan = 2;
    noServersPage.setLayoutData(layoutData);

    Composite body = noServersPage.getBody();
    GridLayout layout = new GridLayout(2, false);
    body.setLayout(layout);

    Link hlink = new Link(body, SWT.NONE);
    hlink.setText(Messages.ServersView_noServers);
    hlink.setBackground(book.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    GridData gd = new GridData(SWT.LEFT, SWT.FILL, true, false);
    hlink.setLayoutData(gd);
    hlink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        NewServerLocationWizard wizard = new NewServerLocationWizard();
        WizardDialog wd = new WizardDialog(book.getShell(), wizard);
        if (wd.open() == Window.OK) {
          toggleServerPage();
        }
      }
    });
  }

  private void toggleServerPage() {
    List<IServer> servers = ServersManager.getInstance().getServers();
    if (servers.isEmpty()) {
      book.showPage(noServersPage);
      selectedServer = null;
    } else {
      book.showPage(serverDropDownPage);
      serverCombo.setInput(servers.toArray());
      if (!servers.contains(selectedServer)) {
        selectedServer = servers.get(0);
      }
      serverCombo.setSelection(new StructuredSelection(selectedServer));
    }
  }

  private ColumnViewerEditorActivationStrategy createActivationSupport() {
    ColumnViewerEditorActivationStrategy activationSupport = new ColumnViewerEditorActivationStrategy(viewer) {
      @Override
      protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
        return event.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
          || event.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION
          || event.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC
          || (event.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED && event.keyCode == KeyLookupFactory
            .getDefault().formalKeyLookup(IKeyLookup.F2_NAME));
      }
    };
    activationSupport.setEnableEditorActivationWithKeyboard(true);
    return activationSupport;
  }

  private final class ServerChangeListener implements IServerLifecycleListener {

    @Override
    public void serverRemoved(IServer server) {
      updateServerPage();
    }

    @Override
    public void serverChanged(IServer server) {
      updateServerPage();
    }

    @Override
    public void serverAdded(IServer server) {
      updateServerPage();
    }

    private void updateServerPage() {
      getContainer().getShell().getDisplay().asyncExec(() -> {
        toggleServerPage();
        getContainer().getShell().layout(true, true);
      });
    }
  }

  private class ProjectAssociationModelEditingSupport extends EditingSupport {

    public ProjectAssociationModelEditingSupport(TableViewer viewer) {
      super(viewer);
    }

    @Override
    protected boolean canEdit(Object element) {
      return selectedServer != null && element instanceof ProjectBindModel;
    }

    @Override
    protected CellEditor getCellEditor(Object element) {
      return new TextCellEditorWithContentProposal(viewer.getTable(), new SearchEngineProvider(selectedServer, BindProjectsPage.this), (ProjectBindModel) element);
    }

    @Override
    protected Object getValue(Object element) {
      return StringUtils.trimToEmpty(((ProjectBindModel) element).getSonarFullName());
    }

    @Override
    protected void setValue(Object element, Object value) {
      // Don't set value as the model was already updated in the text adapter
    }

  }

  /**
   * Update all Eclipse projects when a binding was provided:
   */
  public boolean finish() {
    final ProjectBindModel[] projectBindings = getProjects();
    for (ProjectBindModel projectBinding : projectBindings) {
      updateProjectBinding(projectBinding);
    }
    return true;
  }

  private static void updateProjectBinding(ProjectBindModel projectBinding) {
    boolean changed = false;
    IProject project = projectBinding.getProject();
    SonarLintProject sonarProject = SonarLintProject.getInstance(project);
    String oldServerId = sonarProject.getServerId();
    if (!Objects.equals(projectBinding.getServerId(), oldServerId)) {
      sonarProject.setServerId(projectBinding.getServerId());
      changed = true;
    }
    if (!Objects.equals(projectBinding.getModuleKey(), sonarProject.getModuleKey())) {
      sonarProject.setModuleKey(projectBinding.getModuleKey());
      changed = true;
    }
    if (changed) {
      updateProjectBinding(projectBinding, project, sonarProject, oldServerId);
    }
  }

  private static void updateProjectBinding(ProjectBindModel projectBinding, IProject project, SonarLintProject sonarProject, String oldServerId) {
    sonarProject.save();
    MarkerUtils.deleteIssuesMarkers(project);
    MarkerUtils.deleteChangeSetIssuesMarkers(project);
    SonarLintCorePlugin.clearIssueTracker(project);
    JobUtils.scheduleAnalysisOfOpenFiles(project);
    if (sonarProject.isBound()) {
      new ProjectUpdateJob(sonarProject).schedule();
    }
    if (oldServerId != null && !Objects.equals(projectBinding.getServerId(), oldServerId)) {
      IServer oldServer = ServersManager.getInstance().getServer(oldServerId);
      if (oldServer != null) {
        oldServer.notifyAllListeners();
      }
    }
    if (projectBinding.getServerId() != null) {
      IServer server = ServersManager.getInstance().getServer(projectBinding.getServerId());
      if (server != null) {
        server.notifyAllListeners();
      }
    }
    IBaseLabelProvider labelProvider = PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(SonarLintProjectDecorator.ID);
    if (labelProvider != null) {
      ((SonarLintProjectDecorator) labelProvider).fireChange(new IProject[] {sonarProject.getProject()});
    }
  }

  private ProjectBindModel[] getProjects() {
    WritableList projectAssociations = (WritableList) viewer.getInput();
    return (ProjectBindModel[]) projectAssociations.toArray(new ProjectBindModel[projectAssociations.size()]);
  }
}
