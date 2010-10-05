package org.sonar.ide.eclipse.wizards;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.core.databinding.property.value.IValueProperty;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.databinding.viewers.ViewerSupport;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.sonar.ide.eclipse.SonarImages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.actions.ToggleNatureAction;
import org.sonar.ide.eclipse.core.SonarLogger;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.ui.AbstractModelObject;
import org.sonar.ide.eclipse.ui.InlineEditingSupport;
import org.sonar.ide.eclipse.utils.SelectionUtils;
import org.sonar.wsclient.Host;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Inspired by org.eclipse.pde.internal.ui.wizards.tools.ConvertedProjectWizard
 */
public class ConfigureProjectsWizard extends Wizard {

  private ConfigureProjectsPage mainPage;
  private List<IProject> projects;
  private List<IProject> selected;

  public ConfigureProjectsWizard(List<IProject> projects, List<IProject> initialSelection) {
    setNeedsProgressMonitor(true);
    setWindowTitle("Associate with Sonar");
    this.projects = projects;
    this.selected = initialSelection;
  }

  @Override
  public void addPages() {
    mainPage = new ConfigureProjectsPage(projects, selected);
    addPage(mainPage);
  }

  @Override
  public boolean performFinish() {
    return mainPage.finish();
  }

  // TODO move to top level
  public class ConfigureProjectsPage extends WizardPage {
    private List<IProject> projects;
    private List<IProject> selected;
    private CheckboxTableViewer viewer;
    private ComboViewer comboViewer;

    public ConfigureProjectsPage(List<IProject> projects, List<IProject> selected) {
      super("configureProjects", "Associate with Sonar", SonarImages.getImageDescriptor(SonarImages.IMG_SONARWIZBAN));
      setDescription("Select projects to add Sonar capability.");
      this.projects = projects;
      this.selected = selected;
    }

    public void createControl(Composite parent) {
      Composite container = new Composite(parent, SWT.NONE);

      GridLayout layout = new GridLayout();
      layout.numColumns = 1;
      layout.marginHeight = 0;
      layout.marginWidth = 5;
      container.setLayout(layout);

      GridData gridData;

      // List of Sonar servers
      comboViewer = new ComboViewer(container);
      gridData = new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
      comboViewer.getCombo().setLayoutData(gridData);
      comboViewer.setContentProvider(ArrayContentProvider.getInstance());
      comboViewer.setLabelProvider(new LabelProvider() {
        @Override
        public String getText(Object element) {
          return ((Host) element).getHost();
        }
      });
      comboViewer.setInput(SonarPlugin.getServerManager().getServers());
      comboViewer.getCombo().select(0);

      // List of projects
      viewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
      gridData = new GridData(GridData.FILL, GridData.FILL, true, true, 1, 1);
      viewer.getTable().setLayoutData(gridData);

      DataBindingContext dbc = new DataBindingContext();

      viewer.getTable().setHeaderVisible(true);

      TableViewerColumn columnProject = new TableViewerColumn(viewer, SWT.LEFT);
      columnProject.getColumn().setText("Project");
      columnProject.getColumn().setWidth(200);

      TableViewerColumn columnGroupId = new TableViewerColumn(viewer, SWT.LEFT);
      columnGroupId.getColumn().setText("GroupId");
      columnGroupId.getColumn().setWidth(200);

      TableViewerColumn columnArtifactId = new TableViewerColumn(viewer, SWT.LEFT);
      columnArtifactId.getColumn().setText("ArtifactId");
      columnArtifactId.getColumn().setWidth(200);

      TableViewerColumn columnBranch = new TableViewerColumn(viewer, SWT.LEFT);
      columnBranch.getColumn().setText("Branch");
      columnBranch.getColumn().setWidth(200);

      columnGroupId.setEditingSupport(new InlineEditingSupport(viewer, dbc, SonarProject.PROPERTY_GROUP_ID));
      columnArtifactId.setEditingSupport(new InlineEditingSupport(viewer, dbc, SonarProject.PROPERTY_ARTIFACT_ID));
      columnBranch.setEditingSupport(new InlineEditingSupport(viewer, dbc, SonarProject.PROPERTY_BRANCH));

      List<SonarProject> list = Lists.newArrayList();
      List<SonarProject> selectedList = Lists.newArrayList();
      for (IProject project : projects) {
        SonarProject sonarProject = new SonarProject(project);
        list.add(sonarProject);
        if (selected.contains(project)) {
          selectedList.add(sonarProject);
        }
      }

      // TODO we can improve UI of table by adding image to first element :
      // PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
      ViewerSupport.bind(viewer,
        new WritableList(list, SonarProject.class),
        new IValueProperty[] {
        BeanProperties.value(SonarProject.class, SonarProject.PROPERTY_PROJECT_NAME),
        BeanProperties.value(SonarProject.class, SonarProject.PROPERTY_GROUP_ID),
        BeanProperties.value(SonarProject.class, SonarProject.PROPERTY_ARTIFACT_ID),
        BeanProperties.value(SonarProject.class, SonarProject.PROPERTY_BRANCH)
       });
      viewer.setCheckedElements(selectedList.toArray(new SonarProject[selectedList.size()]));

      setControl(container);
    }

    public boolean finish() {
      Object[] checked = viewer.getCheckedElements();
      for (Object obj : checked) {
        SonarProject sonarProject = (SonarProject) obj;
        try {
          IProject project = sonarProject.getProject();
          ProjectProperties properties = ProjectProperties.getInstance(project);
          Host host = (Host) SelectionUtils.getSingleElement(comboViewer.getSelection());
          properties.setUrl(host.getHost());
          properties.setArtifactId(sonarProject.getArtifactId());
          properties.setGroupId(sonarProject.getGroupId());
          properties.setBranch(sonarProject.getBranch());
          properties.save();
          ToggleNatureAction.enableNature(project);
        } catch (CoreException e) {
          SonarLogger.log(e);
          return false;
        }
      }
      return true;
    }

    public class SonarProject extends AbstractModelObject {
      public static final String PROPERTY_PROJECT_NAME = "name";
      public static final String PROPERTY_GROUP_ID = "groupId";
      public static final String PROPERTY_ARTIFACT_ID = "artifactId";
      public static final String PROPERTY_BRANCH = "branch";

      private IProject project;
      private String groupId;
      private String artifactId;
      private String branch;

      public SonarProject(IProject project) {
        this.project = project;
        this.groupId = "";
        this.artifactId = "";
        this.branch = "";
      }

      public IProject getProject() {
        return project;
      }

      public String getGroupId() {
        return groupId;
      }

      public void setGroupId(String groupId) {
        firePropertyChange("groupId", this.groupId, this.groupId = groupId);
      }

      public String getArtifactId() {
        return artifactId;
      }

      public void setArtifactId(String artifactId) {
        firePropertyChange("artifactId", this.artifactId, this.artifactId = artifactId);
      }

      public String getName() {
        return project.getName();
      }

      public void setBranch(String branch) {
        firePropertyChange("branch", this.branch, this.branch = branch);
      }

      public String getBranch() {
        return branch;
      }
    }

  }

}
