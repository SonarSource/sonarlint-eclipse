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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.sonar.ide.api.IMeasure;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.SonarImages;
import org.sonar.ide.eclipse.core.FavoriteMetricsManager;
import org.sonar.ide.eclipse.core.ISonarConstants;
import org.sonar.ide.eclipse.core.ISonarMeasure;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.jobs.AbstractRemoteSonarJob;
import org.sonar.ide.eclipse.ui.AbstractSonarInfoView;
import org.sonar.ide.eclipse.ui.EnhancedFilteredTree;
import org.sonar.ide.eclipse.utils.SelectionUtils;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Metric;
import org.sonar.wsclient.services.MetricQuery;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * @author Evgeny Mandrikov
 */
public class MeasuresView extends AbstractSonarInfoView {

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.MeasuresView";

  private static final String FAVORITES_CATEGORY = "Favorites";

  private TreeViewer viewer;
  private IAction toggleFavoriteAction;
  private HashMap<String, Collection<ISonarMeasure>> measuresByDomain;

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
    viewer.setComparator(new ViewerComparator() {
      @Override
      public int category(Object element) {
        if (element instanceof Map.Entry) {
          String s = (String) ((Map.Entry) element).getKey();
          if (FAVORITES_CATEGORY.equals(s)) {
            return 0;
          } else {
            return 1;
          }
        }
        return super.category(element);
      }
    });
    Tree tree = viewer.getTree();
    tree.setHeaderVisible(true);
    tree.setLinesVisible(true);
    TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
    column1.setText("Name");
    column1.setWidth(200);
    TreeColumn column2 = new TreeColumn(tree, SWT.LEFT);
    column2.setText("Value");
    column2.setWidth(100);

    toggleFavoriteAction = new Action("Toggle favorite") {
      public void run() {
        ISonarMeasure measure = (ISonarMeasure) getSelectedElement();
        String metricKey = measure.getMetricKey();
        FavoriteMetricsManager.getInstance().toggle(metricKey);
        toggleFavorite(measure);
      }
    };

    hookContextMenu();
  }

  private void hookContextMenu() {
    // Create menu manager
    MenuManager menuMgr = new MenuManager("#PopupMenu");
    menuMgr.setRemoveAllWhenShown(true);
    menuMgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager mgr) {
        fillContextMenu(mgr);
      }
    });
    // Create menu
    Menu menu = menuMgr.createContextMenu(viewer.getControl());
    viewer.getControl().setMenu(menu);
    // Register menu for extension
    getSite().registerContextMenu(menuMgr, viewer);
  }

  private void fillContextMenu(IMenuManager mgr) {
    // populate menu
    Object selectedElement = getSelectedElement();
    if (selectedElement instanceof ISonarMeasure) {
      ISonarMeasure measure = (ISonarMeasure) selectedElement;
      if (FavoriteMetricsManager.getInstance().isFavorite(measure.getMetricKey())) {
        toggleFavoriteAction.setText("Remove from favorites");
        toggleFavoriteAction.setImageDescriptor(SonarImages.STAR_OFF);
      } else {
        toggleFavoriteAction.setText("Add to favorites");
        toggleFavoriteAction.setImageDescriptor(SonarImages.STAR);
      }
      mgr.add(toggleFavoriteAction);
    }
    // required, for extensions
    mgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
  }

  private Object getSelectedElement() {
    return SelectionUtils.getSingleElement(viewer.getSelection());
  }

  @Override
  protected Control getControl() {
    return viewer.getControl();
  }

  class MeasuresLabelProvider implements ITableLabelProvider, ILabelProvider {

    public Image getColumnImage(Object element, int columnIndex) {
      if ((columnIndex == 0) && (element instanceof ISonarMeasure)) {
        ISonarMeasure measure = (ISonarMeasure) element;
        ImageDescriptor imageDescriptor = SonarImages.forTendency(measure);
        if (imageDescriptor != null) {
          return imageDescriptor.createImage();
        }
      }
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
      if (element instanceof ISonarMeasure) {
        ISonarMeasure measure = (ISonarMeasure) element;
        switch (columnIndex) {
          case 0:
            return measure.getMetricName();
          case 1:
            return measure.getValue();
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

  private void update(final String description, final Object content) {
    Display.getDefault().asyncExec(new Runnable() {
      public void run() {
        setContentDescription(description);
        viewer.setInput(content);
        viewer.expandAll();
      }
    });
  }

  private void toggleFavorite(ISonarMeasure measure) {
    Collection<ISonarMeasure> favorites = measuresByDomain.get(FAVORITES_CATEGORY);
    if (favorites == null) {
      favorites = Lists.newArrayList();
      measuresByDomain.put(FAVORITES_CATEGORY, favorites);
    }
    if (favorites.contains(measure)) {
      favorites.remove(measure);
    } else {
      favorites.add(measure);
    }
    if (favorites.isEmpty()) {
      measuresByDomain.remove(FAVORITES_CATEGORY);
    }
    viewer.refresh();
  }

  @Override
  protected void doSetInput(Object input) {
    final ISonarResource element = (ISonarResource) input;
    Job job = new AbstractRemoteSonarJob("Loading measures") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Loading measures for " + element.getKey(), IProgressMonitor.UNKNOWN);
        update("Loading...", null);
        EclipseSonar index = EclipseSonar.getInstance(element.getProject());
        final SourceCode sourceCode = index.search(element);
        if (sourceCode == null) {
          update("Not found.", null);
        } else {
          Collection<ISonarMeasure> measures = getMeasures(index, element);
          final List<ISonarMeasure> favorites = Lists.newArrayList();

          // Group by domain
          final Multimap<String, ISonarMeasure> measuresByDomain = ArrayListMultimap.create();
          for (ISonarMeasure measure : measures) {
            if (FavoriteMetricsManager.getInstance().isFavorite(measure.getMetricKey())) {
              favorites.add(measure);
            }
            String domain = measure.getMetricDomain();
            measuresByDomain.put(domain, measure);
          }

          MeasuresView.this.measuresByDomain = Maps.newHashMap(measuresByDomain.asMap());
          if ( !favorites.isEmpty()) {
            MeasuresView.this.measuresByDomain.put(FAVORITES_CATEGORY, favorites);
          }
          update(element.getName(), MeasuresView.this.measuresByDomain);
        }
        monitor.done();
        return Status.OK_STATUS;
      }
    };
    IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) getSite().getAdapter(IWorkbenchSiteProgressService.class);
    siteService.schedule(job);
  }

  public List<ISonarMeasure> getMeasures(EclipseSonar index, ISonarResource sonarResource) {
    Map<String, Metric> metricsByKey = getMetrics(index);
    Set<String> keys = metricsByKey.keySet();
    String[] metricKeys = keys.toArray(new String[keys.size()]);
    ResourceQuery query = ResourceQuery.createForMetrics(sonarResource.getKey(), metricKeys).setIncludeTrends(true);
    Resource resource = index.getSonar().find(query);
    List<ISonarMeasure> result = Lists.newArrayList();
    for (Measure measure : resource.getMeasures()) {
      final Metric metric = metricsByKey.get(measure.getMetricKey());
      // Hacks around SONAR-1620
      if ( !metric.getHidden() && !"DATA".equals(metric.getType()) && StringUtils.isNotBlank(measure.getFormattedValue())) {
        result.add(SonarCorePlugin.createSonarMeasure(sonarResource, metric, measure));
      }
    }
    return result;
  }

  public Map<String, Metric> getMetrics(EclipseSonar index) {
    // TODO Godin: This is not optimal. Would be better to load metrics only once.
    List<Metric> metrics = index.getSonar().findAll(MetricQuery.all());
    return Maps.uniqueIndex(metrics, new Function<Metric, String>() {
      public String apply(Metric metric) {
        return metric.getKey();
      }
    });
  }
}
