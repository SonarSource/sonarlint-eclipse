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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.sonar.ide.api.IMeasure;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.core.SonarLogger;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.ui.AbstractSonarInfoView;
import org.sonar.ide.eclipse.ui.EnhancedFilteredTree;

import com.google.common.base.Function;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Evgeny Mandrikov
 */
public class MeasuresView extends AbstractSonarInfoView {

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.MeasuresView";

  private TreeViewer viewer;

  @Override
  protected void internalCreatePartControl(Composite parent) {
    PatternFilter filter = new PatternFilter() {
      /**
       * This is a workaround to show measures, which belongs to specified category.
       */
      @SuppressWarnings("unchecked")
      @Override
      protected boolean isParentMatch(Viewer viewer, Object element) {
        Map<String, List<IMeasure>> map = (Map<String, List<IMeasure>>) viewer.getInput();
        if (element instanceof IMeasure) {
          IMeasure measure = (IMeasure) element;
          String domain = measure.getMetricDef().getDomain();
          for (Map.Entry<String, List<IMeasure>> e : map.entrySet()) {
            if (domain.equals(e.getKey())) {
              return isLeafMatch(viewer, e);
            }
          }
        }
        return super.isParentMatch(viewer, element);
      }
    };
    FilteredTree filteredTree = new EnhancedFilteredTree(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL, filter);
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

  @Override
  protected Control getControl() {
    return viewer.getControl();
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
      if (element instanceof IMeasure) {
        switch (columnIndex) {
          case 0:
            return ((IMeasure) element).getMetricDef().getName();
          case 1:
            return ((IMeasure) element).getValue();
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

  private void clear() {
    update("Select Java project, package or class in Package Explorer to see measures.", null);
  }

  private void update(final String description, final Object content) {
    // TODO Godin: use syncExec to avoid "Unhandled event loop exception" ?
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        setContentDescription(description);
        viewer.setInput(content);
        viewer.expandAll();
      }
    });
  }

  @Override
  protected void doSetInput(Object input) {
    final ISonarResource element = (ISonarResource) input;
    Job job = new Job("Loading measures") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Loading measures for " + element.getKey(), IProgressMonitor.UNKNOWN);
        try {
          update("Loading...", null);
          final SourceCode sourceCode = EclipseSonar.getInstance(element.getProject()).search(element);
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
        } catch (Exception e) {
          SonarLogger.log(e);
        }
        monitor.done();
        return Status.OK_STATUS;
      }
    };
    IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) getSite().getAdapter(IWorkbenchSiteProgressService.class);
    siteService.schedule(job);
  }

}
