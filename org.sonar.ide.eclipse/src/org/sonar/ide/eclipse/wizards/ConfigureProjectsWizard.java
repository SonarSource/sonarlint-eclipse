package org.sonar.ide.eclipse.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.sonar.ide.api.SonarIdeException;
import org.sonar.ide.eclipse.SonarImages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.actions.ToggleNatureAction;
import org.sonar.ide.eclipse.core.SonarLogger;
import org.sonar.wsclient.Host;

import java.util.List;

/**
 * Inspired by org.eclipse.pde.internal.ui.wizards.tools.ConvertedProjectWizard
 */
public class ConfigureProjectsWizard extends Wizard {

  private static final boolean DEVELOP = false;

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
    private IProject[] projects;
    private IProject[] selected;
    private CheckboxTableViewer viewer;

    public ConfigureProjectsPage(List<IProject> projects, List<IProject> selected) {
      super("configureProjects", "Associate with Sonar", SonarImages.getImageDescriptor(SonarImages.IMG_SONARWIZBAN));
      setDescription("Select projects to add Sonar capability.");
      this.projects = projects.toArray(new IProject[projects.size()]);
      this.selected = selected.toArray(new IProject[selected.size()]);
    }

    public void createControl(Composite parent) {
      Composite container = new Composite(parent, SWT.NONE);

      GridLayout layout = new GridLayout();
      layout.numColumns = 1;
      layout.marginHeight = 0;
      layout.marginWidth = 5;
      container.setLayout(layout);

      GridData gridData;

      if (DEVELOP) {
        // List of Sonar servers
        ComboViewer comboViewer = new ComboViewer(container);
        gridData = new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
        comboViewer.getCombo().setLayoutData(gridData);
        comboViewer.setContentProvider(new ArrayContentProvider());
        comboViewer.setLabelProvider(new LabelProvider() {
          @Override
          public String getText(Object element) {
            return ((Host) element).getHost();
          }
        });
        comboViewer.setInput(SonarPlugin.getServerManager().getServers());
        comboViewer.getCombo().select(0);
      }

      // List of projects
      viewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
      gridData = new GridData(GridData.FILL, GridData.FILL, true, true, 1, 1);
      viewer.getTable().setLayoutData(gridData);

      if (DEVELOP) {
        viewer.getTable().setHeaderVisible(true);

        TableViewerColumn column1 = new TableViewerColumn(viewer, SWT.LEFT);
        column1.getColumn().setText("Project");
        column1.getColumn().setWidth(200);

        TableViewerColumn column2 = new TableViewerColumn(viewer, SWT.LEFT);
        column2.getColumn().setText("GroupId");
        column2.getColumn().setWidth(100);

        TableViewerColumn column3 = new TableViewerColumn(viewer, SWT.LEFT);
        column3.getColumn().setText("ArtifactId");
        column3.getColumn().setWidth(100);

        TableViewerColumn column4 = new TableViewerColumn(viewer, SWT.LEFT);
        column4.getColumn().setText("Branch");
        column4.getColumn().setWidth(100);
        column4.setEditingSupport(new EditingSupport(viewer) {
          @Override
          protected CellEditor getCellEditor(Object element) {
            return new TextCellEditor(viewer.getTable());
          }

          @Override
          protected boolean canEdit(Object element) {
            return true;
          }

          @Override
          protected Object getValue(Object element) {
            // TODO
            return "";
          }

          @Override
          protected void setValue(Object element, Object value) {
            // TODO Auto-generated method stub
          }
        });
      }

      viewer.setContentProvider(new ProjectContentProvider());
      viewer.setLabelProvider(new ProjectLabelProvider());
      viewer.setInput(projects);
      viewer.setCheckedElements(selected);
      setControl(container);
    }

    public boolean finish() {
      Object[] checked = viewer.getCheckedElements();
      for (Object obj : checked) {
        IProject project = (IProject) obj;
        try {
          // TODO not only enable nature, but also set server, groupId, artifactId, ...
          ToggleNatureAction.enableNature(project);
        } catch (CoreException e) {
          SonarLogger.log(e);
          return false;
        }
      }
      return true;
    }

    public class ProjectContentProvider implements IStructuredContentProvider {
      public Object[] getElements(Object parent) {
        if (projects != null) {
          return projects;
        }
        return new Object[0];
      }

      public void dispose() {
      }

      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }
    }

    public class ProjectLabelProvider extends LabelProvider implements ITableLabelProvider {
      public Image getColumnImage(Object element, int index) {
        if (index == 0) {
          return PlatformUI.getWorkbench().getSharedImages().getImage(IDE.SharedImages.IMG_OBJ_PROJECT);
        }
        return null;
      }

      public String getColumnText(Object obj, int index) {
        switch (index) {
          case 0:
            return ((IProject) obj).getName();
          case 1:
            return "todo groupId";
          case 2:
            return "todo artifacId";
          case 3:
            return "todo branch";
          default:
            throw new SonarIdeException("Should never happen");
        }
      }
    }
  }
}
