/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.views;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.part.ViewPart;
import org.sonar.ide.client.SonarClient;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.shared.MetricsLoader;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Metric;

/**
 * @author Jérémie Lagarde
 * @deprecated since 0.2.0 use {@link MeasuresView} instead of it
 */
@Deprecated
public class MetricsView extends ViewPart {

  public static final String ID = "org.sonar.ide.eclipse.views.MetricsView";

  private TableViewer viewer;
  private Combo serversCombo;

  @Override
  public void createPartControl(final Composite parent) {
    parent.setLayout(new GridLayout(2, false));
    createServersCombo(parent);
    createViewer(parent);
    if (serversCombo.getItemCount() > 0) {
      final Sonar sonar = SonarPlugin.getServerManager().getSonar(serversCombo.getText());
      final Collection<Metric> metrics = MetricsLoader.getMetrics(sonar, null);
      viewer.setInput(metrics);
    }
  }

  private void createServersCombo(final Composite parent) {
    final Label label = new Label(parent, SWT.NONE);
    label.setText(Messages.getString("pref.project.label.host")); //$NON-NLS-1$
    // Create select list of servers.
    final GridData gridData = new GridData();
    gridData.horizontalSpan = 1;
    serversCombo = new Combo(parent, SWT.READ_ONLY);
    serversCombo.setLayoutData(gridData);
    final List<Host> servers = SonarPlugin.getServerManager().getServers();
    try {
      final String defaultServer = SonarPlugin.getServerManager().getDefaultServer().getHost();
      int index = -1;
      for (int i = 0; i < servers.size(); i++) {
        final Host server = servers.get(i);
        if (StringUtils.equals(defaultServer, server.getHost())) {
          index = i;
        }
        serversCombo.add(server.getHost());
      }
      if (index == -1) {
        serversCombo.add(defaultServer);
        index = servers.size();
      }
      serversCombo.select(index);
    } catch (final Exception e) {
      serversCombo.select(0);
    }

    serversCombo.addSelectionListener(new SelectionAdapter() {

      @Override
      public void widgetSelected(final SelectionEvent e) {
        final SonarClient sonar = (SonarClient) SonarPlugin.getServerManager().getSonar(serversCombo.getText());
        if (sonar != null && sonar.isAvailable()) {
          final Collection<Metric> metrics = MetricsLoader.getMetrics(sonar, null);
          viewer.setInput(metrics);
        } else {
          viewer.setInput(null);
        }
      }
    });

  }

  private void createViewer(final Composite parent) {
    final GridData gridData = new GridData();
    gridData.horizontalSpan = 2;
    gridData.horizontalAlignment = SWT.FILL;
    gridData.grabExcessHorizontalSpace = true;
    gridData.verticalAlignment = SWT.FILL;
    gridData.grabExcessVerticalSpace = true;
    viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    viewer.getControl().setLayoutData(gridData);
    createColumns(viewer);
    viewer.setContentProvider(new MetricsContentProvider());
    viewer.setLabelProvider(new MetricsLabelProvider());
  }

  private void createColumns(final TableViewer viewer) {

    final String[] titles = { "Name", "Domaine", "key", "Description" };
    final int[] bounds = { 100, 100, 100, 0 };

    for (int i = 0; i < titles.length; i++) {
      final TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
      column.getColumn().setText(titles[i]);
      column.getColumn().setWidth(bounds[i]);
      column.getColumn().setResizable(true);
      column.getColumn().setMoveable(true);
    }
    final Table table = viewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
  }

  /**
   * Passing the focus request to the viewer's control.
   */
  @Override
  public void setFocus() {
    viewer.getControl().setFocus();
  }

}
