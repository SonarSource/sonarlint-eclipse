/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.internal.ui.views;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.sonar.ide.api.SonarIdeException;
import org.sonar.ide.eclipse.core.*;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.internal.ui.AbstractSonarInfoView;
import org.sonar.ide.eclipse.internal.ui.AbstractTableLabelProvider;
import org.sonar.ide.eclipse.jobs.AbstractRemoteSonarJob;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.ide.eclipse.ui.util.PlatformUtils;
import org.sonar.ide.eclipse.ui.util.SelectionUtils;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import com.google.common.collect.Lists;

import java.util.List;

public class HotspotsView extends AbstractSonarInfoView {

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.HotspotsView";

  private static final int LIMIT = 20;

  private TableViewer viewer;
  private ComboViewer comboViewer;
  private Combo combo;
  private ISonarMetric metric;
  private TableViewerColumn column2;

  private FavouriteMetricsManager.Listener favouriteMetricsListener = new FavouriteMetricsManager.Listener() {
    public void updated() {
      updateFavouriteMetrics();
    }
  };

  @Override
  protected void internalCreatePartControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout(2, false);
    container.setLayout(layout);

    Label hotspotsLabel = new Label(container, SWT.NONE);
    hotspotsLabel.setText("Hotspots by");

    comboViewer = new ComboViewer(container, SWT.READ_ONLY | SWT.TOP);
    GridData gridData = new GridData();
    gridData.horizontalAlignment = SWT.FILL;
    gridData.grabExcessHorizontalSpace = true;
    comboViewer.getCombo().setLayoutData(gridData);

    viewer = new TableViewer(container);
    gridData = new GridData();
    gridData.horizontalSpan = 2;
    gridData.horizontalAlignment = SWT.FILL;
    gridData.grabExcessHorizontalSpace = true;
    gridData.grabExcessVerticalSpace = true;
    gridData.verticalAlignment = SWT.FILL;
    viewer.getTable().setLayoutData(gridData);
    viewer.getTable().setHeaderVisible(true);
    viewer.getTable().setLinesVisible(true);

    TableViewerColumn column1 = new TableViewerColumn(viewer, SWT.LEFT);
    column1.getColumn().setText("Resource");
    column1.getColumn().setWidth(200);
    column2 = new TableViewerColumn(viewer, SWT.LEFT);
    column2.getColumn().setWidth(200);

    viewer.setContentProvider(ArrayContentProvider.getInstance());
    viewer.setLabelProvider(new HotspotsLabelProvider());
    viewer.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        ISonarMeasure measure = (ISonarMeasure) SelectionUtils.getSingleElement(viewer.getSelection());
        IResource resource = measure.getSonarResource().getResource();
        if (resource instanceof IFile) {
          PlatformUtils.openEditor((IFile) resource);
        }
      }
    });

    combo = comboViewer.getCombo();
    comboViewer.setContentProvider(ArrayContentProvider.getInstance());
    comboViewer.setLabelProvider(new MetricNameLabelProvider());
    updateFavouriteMetrics();
    SonarUiPlugin.getFavouriteMetricsManager().addListener(favouriteMetricsListener);

    combo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        metric = getSelectedMetric();
        if (getInput() != null) {
          doSetInput(getInput());
        }
      }
    });
    combo.select(0);
    metric = getSelectedMetric();

    getSite().setSelectionProvider(viewer);
  }

  /**
   * TODO Godin: extract to upper level
   */
  public static class MetricNameLabelProvider extends BaseLabelProvider implements ILabelProvider {
    public String getText(Object element) {
      return ((ISonarMetric) element).getName();
    }

    public Image getImage(Object element) {
      return null;
    }
  }

  private ISonarMetric getSelectedMetric() {
    return ((ISonarMetric) SelectionUtils.getSingleElement(comboViewer.getSelection()));
  }

  private class HotspotsLabelProvider extends AbstractTableLabelProvider {
    @Override
    public String getColumnText(Object element, int columnIndex) {
      ISonarMeasure measure = (ISonarMeasure) element;
      switch (columnIndex) {
        case 0:
          return measure.getSonarResource().getName();
        case 1:
          return measure.getValue();
        default:
          throw new SonarIdeException("Should never happen");
      }
    }
  }

  @Override
  protected Control getControl() {
    return viewer.getControl();
  }

  private String getMetricKey() {
    if (metric == null) {
      return null;
    }
    return metric.getKey();
  }

  private void updateFavouriteMetrics() {
    comboViewer.setInput(SonarUiPlugin.getFavouriteMetricsManager().get());
    if (getMetricKey() != null) {
      comboViewer.setSelection(new StructuredSelection(metric));
    }
  }

  private void update(final Object content) {
    getSite().getShell().getDisplay().asyncExec(new Runnable() {
      public void run() {
        setContentDescription(getInput().getName());
        updateFavouriteMetrics();
        column2.getColumn().setText(combo.getText());
        viewer.setInput(content);
      }
    });
  }

  @Override
  protected ISonarResource findSonarResource(Object element) {
    ISonarResource sonarResource = super.findSonarResource(element);
    if (sonarResource == null) {
      return null;
    }
    if ( !(sonarResource.getResource() instanceof IProject)) {
      sonarResource = super.findSonarResource(sonarResource.getProject());
    }
    return sonarResource;
  }

  /**
   * @param input ISonarResource to be shown in the view (can't be null)
   */
  @Override
  protected void doSetInput(Object input) {
    final ISonarResource sonarResource = (ISonarResource) input;
    Job job = new AbstractRemoteSonarJob("Loading hotspots") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Loading hotspots for " + sonarResource.getKey(), IProgressMonitor.UNKNOWN);
        EclipseSonar index = EclipseSonar.getInstance(sonarResource.getProject());
        List<Resource> resources = index.getSonar().findAll(getResourceQuery(sonarResource));
        List<ISonarMeasure> measures = Lists.newArrayList();
        for (Resource resource : resources) {
          for (Measure measure : resource.getMeasures()) {
            IFile file = PlatformUtils.adapt(resource, IFile.class);
            if (file != null) { // Sonar resource doesn't exist in working copy
              ISonarResource sonarResource = PlatformUtils.adapt(file, ISonarResource.class);
              measures.add(SonarCorePlugin.createSonarMeasure(sonarResource, measure));
            }
          }
        }
        update(measures);
        monitor.done();
        return Status.OK_STATUS;
      }
    };
    IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) getSite().getAdapter(IWorkbenchSiteProgressService.class);
    siteService.schedule(job);
  }

  private ResourceQuery getResourceQuery(ISonarResource resource) {
    return ResourceQuery.createForMetrics(resource.getKey(), getMetricKey()).setScopes(Resource.SCOPE_ENTITY)
        .setDepth(ResourceQuery.DEPTH_UNLIMITED).setLimit(LIMIT);
  }

  @Override
  public void dispose() {
    SonarUiPlugin.getFavouriteMetricsManager().removeListener(favouriteMetricsListener);
    super.dispose();
  }

}
