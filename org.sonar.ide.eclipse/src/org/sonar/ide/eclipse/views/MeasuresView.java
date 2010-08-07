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
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.sonar.ide.api.IMeasure;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.ui.AbstractPackageExplorerListener;
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
    PatternFilter filter = new PatternFilter() {
      /**
       * This is a workaround to show measures, which belongs to specified category.
       */
      @SuppressWarnings("unchecked")
      @Override
      protected boolean isParentMatch(Viewer viewer, Object element) {
        Map<String, List<MeasureData>> map = (Map<String, List<MeasureData>>) viewer.getInput();
        if (element instanceof MeasureData) {
          MeasureData measure = (MeasureData) element;
          String domain = measure.getDomain();
          for (Map.Entry<String, List<MeasureData>> e : map.entrySet()) {
            if (domain.equals(e.getKey())) {
              return isLeafMatch(viewer, e);
            }
          }
        }
        return super.isParentMatch(viewer, element);
      }
    };
    // TODO incompatible with Eclipse 3.4
    FilteredTree filteredTree = new FilteredTree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL, filter, true);
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

    clear();
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
      return null;
    }

    public String getText(Object element) {
      return getColumnText(element, 0);
    }
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
        // TODO SONARIDE-101
        if (o instanceof IJavaProject) {
          IJavaProject javaProject = (IJavaProject) o;
          IProject project = javaProject.getProject();
          updateMeasures(project, javaProject.getResource());
        } else if (o instanceof IPackageFragment) {
          IPackageFragment packageFragment = (IPackageFragment) o;
          IProject project = packageFragment.getResource().getProject();
          updateMeasures(project, packageFragment.getResource());
        } else if (o instanceof ICompilationUnit) {
          ICompilationUnit cu = (ICompilationUnit) o;
          IProject project = cu.getResource().getProject();
          updateMeasures(project, cu.getResource());
        } else {
          clear();
        }
      }
    }
  };

  private void clear() {
    update("Select Java project, package or class in Package Explorer to see measures.", null);
  }

  private void update(final String description, final Object content) {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        setContentDescription(description);
        viewer.setInput(content);
        viewer.expandAll();
      }
    });
  }

  /**
   * Passing the focus request to the viewer's control.
   */
  @Override
  public void setFocus() {
    viewer.getControl().setFocus();
  }

  private void updateMeasures(final IProject project, final IResource resource) {
    Job job = new Job("Loading measures") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Loading measures for " + resource.toString(), IProgressMonitor.UNKNOWN);
        update("Loading...", null);
        final SourceCode sourceCode = EclipseSonar.getInstance(project).search(resource);
        if (sourceCode == null) {
          update("Not found.", null);
        } else {
          Collection<IMeasure> measures = sourceCode.getMeasures();
          // Group by domain
          final Multimap<String, IMeasure> measuresByDomain = Multimaps.index(measures, new Function<IMeasure, String>() {
            public String apply(IMeasure measure) {
              return measure.getMetricDef().getDomain();
            }
          });
          update(sourceCode.getKey(), measuresByDomain.asMap());
        }
        monitor.done();
        return Status.OK_STATUS;
      }
    };
    IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) getSite().getAdapter(IWorkbenchSiteProgressService.class);
    siteService.schedule(job);
  }

}
