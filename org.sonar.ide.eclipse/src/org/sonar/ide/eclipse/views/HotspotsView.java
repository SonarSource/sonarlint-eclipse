package org.sonar.ide.eclipse.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.ui.AbstractPackageExplorerListener;
import org.sonar.ide.eclipse.ui.AbstractTableLabelProvider;
import org.sonar.ide.eclipse.utils.PlatformUtils;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

public class HotspotsView extends ViewPart {

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.HotspotsView";

  private TableViewer viewer;
  private Combo combo;
  private Object selection;
  private String metricKey;
  private Label resourceLabel;
  private TableViewerColumn column2;

  @Override
  public void createPartControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout(3, false);
    container.setLayout(layout);

    Label hotspotsLabel = new Label(container, SWT.NONE);
    hotspotsLabel.setText("Hotspots by");

    combo = new Combo(container, SWT.READ_ONLY | SWT.TOP);

    resourceLabel = new Label(container, SWT.NONE);
    GridData gridData = new GridData();
    gridData.horizontalAlignment = SWT.FILL;
    gridData.grabExcessHorizontalSpace = true;
    resourceLabel.setLayoutData(gridData);

    viewer = new TableViewer(container);
    gridData = new GridData();
    gridData.horizontalSpan = 3;
    gridData.horizontalAlignment = SWT.FILL;
    gridData.grabExcessHorizontalSpace = true;
    gridData.grabExcessVerticalSpace = true;
    gridData.verticalAlignment = SWT.FILL;
    viewer.getTable().setLayoutData(gridData);
    viewer.getTable().setHeaderVisible(true);

    TableViewerColumn column1 = new TableViewerColumn(viewer, SWT.LEFT);
    column1.getColumn().setText("File");
    column1.getColumn().setWidth(200);
    column2 = new TableViewerColumn(viewer, SWT.LEFT);
    column2.getColumn().setWidth(200);

    viewer.setContentProvider(new ArrayContentProvider());
    viewer.setLabelProvider(new HotspotsLabelProvider());
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        Object object = ((IStructuredSelection) viewer.getSelection()).getFirstElement();
        IFile file = PlatformUtils.adapt(object, IFile.class);
        if (file != null) {
          PlatformUtils.openEditor(file);
        }
      }
    });

    combo.add("complexity");
    combo.add("uncovered_lines");
    combo.add("function_complexity");
    combo.add("public_undocumented_api");
    combo.add("weighted_violations");
    combo.add("duplicated_lines");
    combo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        metricKey = combo.getText();
        updateHotspots();
      }
    });
    combo.select(0);
    metricKey = combo.getText();
  }

  private class HotspotsLabelProvider extends AbstractTableLabelProvider {
    @Override
    public String getColumnText(Object element, int columnIndex) {
      Resource resource = (Resource) element;
      switch (columnIndex) {
        case 0:
          return resource.getName();
        case 1:
          return resource.getMeasureFormattedValue(getMetricKey(), "");
        default:
          throw new RuntimeException("Should not happen");
      }
    }
  }

  @Override
  public void setFocus() {
    viewer.getControl().setFocus();
  }

  @Override
  public void init(IViewSite site) throws PartInitException {
    selectionListener.init(site);
    super.init(site);
  }

  @Override
  public void dispose() {
    super.dispose();
    selectionListener.dispose(getViewSite());
  }

  private AbstractPackageExplorerListener selectionListener = new AbstractPackageExplorerListener(this) {
    @Override
    protected void handleSlection(ISelection selection) {
      if (selection instanceof IStructuredSelection) {
        IStructuredSelection sel = (IStructuredSelection) selection;
        Object o = sel.getFirstElement();
        if (o == null) {
          // no selection
          return;
        }
        HotspotsView.this.selection = o;
        updateHotspots();
      }
    }
  };

  private String getMetricKey() {
    return metricKey;
  }

  private void update(final Object content) {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        resourceLabel.setText("for project " + selection);
        column2.getColumn().setText(metricKey);
        viewer.setInput(content);
      }
    });
  }

  private void updateHotspots() {
    final Resource resource = PlatformUtils.adapt(selection, Resource.class);
    if (resource == null) {
      return;
    }
    Job job = new Job("Loading hotspots") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Loading hotspots for " + resource.getKey(), IProgressMonitor.UNKNOWN);
        IProject project = PlatformUtils.adapt(selection, IResource.class).getProject();
        EclipseSonar index = EclipseSonar.getInstance(project);
        List<Resource> resources = index.getSonar().findAll(getResourceQuery(resource));
        update(resources);
        monitor.done();
        return Status.OK_STATUS;
      }
    };
    IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) getSite().getAdapter(IWorkbenchSiteProgressService.class);
    siteService.schedule(job);
  }

  private ResourceQuery getResourceQuery(Resource resource) {
    return ResourceQuery.createForMetrics(resource.getKey(), getMetricKey())
        .setScopes(Resource.SCOPE_ENTITY)
        .setDepth(ResourceQuery.DEPTH_UNLIMITED)
        .setLimit(5);
  }
}
