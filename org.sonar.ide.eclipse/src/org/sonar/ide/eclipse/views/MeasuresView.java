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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.sonar.ide.eclipse.EclipseSonar;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.properties.ProjectProperties;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.ide.shared.measures.MeasureData;
import org.sonar.wsclient.Sonar;

/**
 * @author Evgeny Mandrikov
 */
public class MeasuresView extends ViewPart {

  public static final String ID = "org.sonar.ide.eclipse.views.MeasuresView";

  private TableViewer viewer;

  private void createColumns(final TableViewer viewer) {
    final int[] bounds = { 100, 100, 100 };
    for (int i = 0; i < MeasuresLabelProvider.COLUMNS.length; i++) {
      final TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
      column.getColumn().setText(MeasuresLabelProvider.COLUMNS[i]);
      column.getColumn().setWidth(bounds[i]);
      column.getColumn().setResizable(true);
      column.getColumn().setMoveable(true);
    }
    final Table table = viewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
  }

  @Override
  public void createPartControl(Composite parent) {
    viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
    createColumns(viewer);
    viewer.setContentProvider(new MeasuresContentProvider());
    viewer.setLabelProvider(new MeasuresLabelProvider());
  }

  @Override
  public void init(IViewSite site) throws PartInitException {
    site.getPage().addSelectionListener(JavaUI.ID_PACKAGES, selectionListener);
    super.init(site);
  }

  @Override
  public void dispose() {
    super.dispose();
    getSite().getPage().removeSelectionListener(JavaUI.ID_PACKAGES, selectionListener);
  }

  private ISelection currentSelection;

  ISelectionListener selectionListener = new ISelectionListener() {
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
      // TODO don't handle selections, if this view inactive, eg. when another perspective selected
      // TODO comment me
      if (selection == null || selection.equals(currentSelection)) {
        return;
      }
      // TODO comment me
      if (part == null) {
        return;
      }

      currentSelection = selection;
      if (currentSelection instanceof IStructuredSelection) {
        IStructuredSelection sel = (IStructuredSelection) currentSelection;
        Object o = sel.getFirstElement();
        if (o == null) {
          // no selection
          return;
        }
        if (o instanceof IPackageFragment) {
          IPackageFragment packageFragment = (IPackageFragment) o;
          IProject project = packageFragment.getResource().getProject();
          updateMeasures(project, getResourceKey(project, packageFragment.getElementName()));
        } else if (o instanceof ICompilationUnit) {
          ICompilationUnit cu = (ICompilationUnit) o;
          try {
            IProject project = cu.getResource().getProject();
            final String packageName;
            if (cu.getPackageDeclarations().length == 0) {
              packageName = "[default]";
            } else {
              packageName = cu.getPackageDeclarations()[0].getElementName();
            }
            String className = StringUtils.removeEnd(cu.getElementName(), ".java");
            updateMeasures(project, getResourceKey(project, packageName + "." + className));
          } catch (JavaModelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    }
  };

  private String getResourceKey(IProject project, String s) {
    return EclipseResourceUtils.getInstance().getProjectKey(project) + ":" + s;
  }

  /**
   * Passing the focus request to the viewer's control.
   */
  @Override
  public void setFocus() {
    viewer.getControl().setFocus();
  }

  private void updateMeasures(final IProject project, final String resourceKey) {
    // TODO Godin: should be refactored
    new Job("My new job") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Some nice progress message here ...", 100);
        // execute the task ...
        ProjectProperties properties = ProjectProperties.getInstance(project);
        Sonar sonar = SonarPlugin.getServerManager().getSonar(properties.getUrl());
        final List<MeasureData> measures = new EclipseSonar(sonar).search(resourceKey).getMeasures();
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            viewer.setInput(measures);
          }
        });
        monitor.done();
        return Status.OK_STATUS;
      }
    }.schedule();
  }

}
