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

package org.sonar.ide.eclipse.views;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.sonar.ide.api.SonarIdeException;
import org.sonar.ide.eclipse.core.FavoriteMetricsManager;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.core.ISonarMeasure;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.jobs.AbstractRemoteSonarJob;
import org.sonar.ide.eclipse.ui.AbstractSonarInfoView;
import org.sonar.ide.eclipse.ui.AbstractTableLabelProvider;
import org.sonar.ide.eclipse.utils.PlatformUtils;
import org.sonar.ide.eclipse.utils.SelectionUtils;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import com.google.common.collect.Lists;

public class HotspotsView extends AbstractSonarInfoView {

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.HotspotsView";

  private static final int LIMIT = 20;

  private TableViewer viewer;
  private ComboViewer comboViewer;
  private Combo combo;
  private String metricKey;
  private Label resourceLabel;
  private TableViewerColumn column2;

  @Override
  protected void internalCreatePartControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout(3, false);
    container.setLayout(layout);

    Label hotspotsLabel = new Label(container, SWT.NONE);
    hotspotsLabel.setText("Hotspots by");

    comboViewer = new ComboViewer(container, SWT.READ_ONLY | SWT.TOP);

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
    comboViewer.setInput(FavoriteMetricsManager.getInstance().get());

    combo.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        metricKey = (String) SelectionUtils.getSingleElement(comboViewer.getSelection());
        if (getInput() != null) {
          doSetInput(getInput());
        }
      }
    });
    combo.select(0);
    metricKey = (String) SelectionUtils.getSingleElement(comboViewer.getSelection());
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
    return metricKey;
  }

  private void update(final Object content) {
    getSite().getShell().getDisplay().asyncExec(new Runnable() {
      public void run() {
        resourceLabel.setText("for project " + getInput().getName());
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
            ISonarResource sonarResource = PlatformUtils.adapt(file, ISonarResource.class);
            measures.add(SonarCorePlugin.createSonarMeasure(sonarResource, measure));
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

}
