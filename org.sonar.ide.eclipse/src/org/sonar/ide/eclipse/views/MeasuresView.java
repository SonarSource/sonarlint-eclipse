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
import java.util.Map;

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
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ViewPart;
import org.sonar.ide.api.IMeasure;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.utils.EclipseResourceUtils;
import org.sonar.ide.shared.measures.MeasureData;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * @author Evgeny Mandrikov
 */
public class MeasuresView extends ViewPart {

  public static final String ID = "org.sonar.ide.eclipse.views.MeasuresView";

  private TreeViewer viewer;

  @Override
  public void createPartControl(Composite parent) {
    PatternFilter filter = new PatternFilter();
    FilteredTree filteredTree = new FilteredTree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL, filter);
    viewer = filteredTree.getViewer();
    viewer.setContentProvider(new MapContentProvider());
    viewer.setLabelProvider(new MeasuresLabelProvider());
    Tree tree = viewer.getTree();
    tree.setHeaderVisible(true);
    tree.setLinesVisible(true);
    TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
    column1.setText("Name");
    column1.setWidth(200);
    TreeColumn column2 = new TreeColumn(tree, SWT.LEFT);
    column2.setText("Value");
    column2.setWidth(100);
  }

  class MeasuresLabelProvider implements ITableLabelProvider, ILabelProvider {

    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    @SuppressWarnings("unchecked")
    public String getColumnText(Object element, int columnIndex) {
      if (element instanceof Map.Entry) {
        if (columnIndex > 0) {
          return "";
        }
        return ((Map.Entry) element).getKey().toString();
      }
      if (element instanceof MeasureData) {
        switch (columnIndex) {
          case 0:
            return ((MeasureData) element).getName();
          case 1:
            return ((MeasureData) element).getValue();
          default:
            return "";
        }
      }
      return "";
    }

    public void addListener(ILabelProviderListener listener) {
    }

    public void dispose() {
    }

    public boolean isLabelProperty(Object element, String property) {
      return false;
    }

    public void removeListener(ILabelProviderListener listener) {
    }

    public Image getImage(Object element) {
      // TODO Auto-generated method stub
      return null;
    }

    public String getText(Object element) {
      return getColumnText(element, 0);
    }

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
        // TODO show measures for project, when project selected
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
        monitor.beginTask("Some nice progress message here ...", IProgressMonitor.UNKNOWN);
        Collection<IMeasure> measures = EclipseSonar.getInstance(project).search(resourceKey).getMeasures();
        // Group by domain
        final Multimap<String, IMeasure> measuresByDomain = Multimaps.index(measures, new Function<IMeasure, String>() {
          public String apply(IMeasure measure) {
            return measure.getMetricDef().getDomain();
          }
        });
        Display.getDefault().asyncExec(new Runnable() {
          public void run() {
            setContentDescription(resourceKey);
            viewer.setInput(measuresByDomain.asMap());
            viewer.expandAll();
          }
        });
        monitor.done();
        return Status.OK_STATUS;
      }
    }.schedule();
  }
}
