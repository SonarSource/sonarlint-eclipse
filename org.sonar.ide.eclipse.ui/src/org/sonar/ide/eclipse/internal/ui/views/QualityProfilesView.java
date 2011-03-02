/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.internal.IWorkbenchThemeConstants;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.sonar.ide.api.IMeasure;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.internal.core.ISonarConstants;
import org.sonar.ide.eclipse.internal.ui.AbstractTableLabelProvider;
import org.sonar.ide.eclipse.internal.ui.EnhancedFilteredTree;
import org.sonar.ide.eclipse.internal.ui.SonarImages;
import org.sonar.ide.eclipse.internal.ui.jobs.AbstractRemoteSonarJob;
import org.sonar.ide.shared.profile.ProfileUtil;
import org.sonar.wsclient.services.Measure;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.services.Rule;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * @author Jérémie Lagarde
 * @since 1.1.0
 */
public class QualityProfilesView extends AbstractSonarInfoView {

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.QualityProfilesView";

  private TreeViewer viewer;
  private HashMap<String, Collection<Rule>> rulesByPlugin;

  private boolean showInactive;

  @Override
  protected void internalCreatePartControl(Composite parent) {
    IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
    toolbarManager.add(new ShowInactiveAction());
    
    PatternFilter filter = new PatternFilter() {
      public boolean isElementVisible(Viewer viewer, Object element){
        if (element instanceof Rule) {
          Rule rule = (Rule) element;
          if(!isShowInactive() && !rule.isActive())
            return false;
        }
        return super.isElementVisible(viewer,element);
      }
    };
    
    filter.setPattern("org.eclipse.ui.keys.optimization.false");
    FilteredTree filteredTree = new EnhancedFilteredTree(parent, SWT.BORDER | SWT.VERTICAL | /* SWT.H_SCROLL | */SWT.V_SCROLL, filter);
    viewer = filteredTree.getViewer();
    viewer.setContentProvider(new MapContentProvider());
    viewer.setLabelProvider(new RulesLabelProvider());
    viewer.setComparator(new ViewerComparator());
    viewer.setUseHashlookup(true);
    Tree tree = viewer.getTree();
    tree.setHeaderVisible(true);
    tree.setLinesVisible(true);
    
    TreeColumn column1 = new TreeColumn(tree, SWT.LEAD );
    column1.setText("Name");
    column1.setWidth(250);
    TreeColumn column2 = new TreeColumn(tree, SWT.LEFT);
    column2.setText("Description");
    column2.setWidth(800);
  }

  @Override
  protected Control getControl() {
    return viewer.getControl();
  }

  
  private class ShowInactiveAction extends Action {
    public ShowInactiveAction() {
      super("Show inactive rules", SWT.TOGGLE);
      setTitleToolTip("Show inactive rules");
      setImageDescriptor(SonarImages.SONARCLOSE_IMG);
      setChecked(isShowInactive());
    }
    @Override
    public void run() {
      setShowInactive( !isShowInactive());
    }
    private void setShowInactive(boolean enabled) {
      if(enabled != showInactive) {
        showInactive = enabled;
        update(getContentDescription(), rulesByPlugin);
      }
    }
  }
  
  protected boolean isShowInactive() {
    return showInactive;
  }
  
  class RulesLabelProvider extends AbstractTableLabelProvider implements ILabelProvider, IColorProvider, IFontProvider {

    @SuppressWarnings("rawtypes")
    public String getColumnText(Object element, int columnIndex) {
      if(element instanceof Map.Entry) {
        if(columnIndex > 0) {
          return "";
        }
        return ((Map.Entry) element).getKey().toString();
      }
      if(element instanceof Rule) {
        Rule rule = (Rule) element;
        if(columnIndex == 0) {
          return rule.getTitle();
        } else {
          return rule.getDescription();
        }
      }
      return "";
    }

    public Image getImage(Object element) {
      return null;
    }

    public String getText(Object element) {
      return getColumnText(element, 0);
    }

    public Font getFont(Object element) {
      if(element instanceof Rule) {
        if( !((Rule) element).isActive()) {
          return JFaceResources.getFontRegistry().getItalic(JFaceResources.DIALOG_FONT);
        }
      } else {
        return JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);
      }
      return null;
    }

    @SuppressWarnings("restriction")
    public Color getForeground(Object element) {

      if(element instanceof Rule) {
        if( !((Rule) element).isActive()) {
          return getControl().getDisplay().getSystemColor(SWT.COLOR_GRAY);
        }
      }
      return null;
    }

    public Color getBackground(Object element) {
      if(element instanceof Map.Entry) {
        return getControl().getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
      }
      return null;
    }
  }

  private void update(final String profileName, final HashMap<String, Collection<Rule>> rules) {
    getSite().getShell().getDisplay().asyncExec(new Runnable() {

      public void run() {
        setContentDescription(profileName);
        viewer.setInput(rules);
      }
    });
  }

  @Override
  protected void doSetInput(Object input) {
    final ISonarResource element = (ISonarResource) input;
    Job job = new AbstractRemoteSonarJob("Loading rules") {

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        monitor.beginTask("Loading rules for " + element.getKey(), IProgressMonitor.UNKNOWN);
        EclipseSonar index = EclipseSonar.getInstance(element.getProject());
        final SourceCode sourceCode = index.search(element);
        if(sourceCode != null && !monitor.isCanceled()) {
          Resource resource = index.getSonar().find(ResourceQuery.createForMetrics(sourceCode.getKey(), ProfileUtil.METRIC_KEY));
          final Measure measure = resource.getMeasure(ProfileUtil.METRIC_KEY);
          if( !monitor.isCanceled() && measure != null) {
            List<Rule> rules = sourceCode.getRules();

            // Group by key and plugin
            final Multimap<String, Rule> rulesByPlugin = ArrayListMultimap.create();
            for(Rule rule : rules) {
                String plugin = rule.getRepository();
                rulesByPlugin.put(plugin, rule);
            }

            QualityProfilesView.this.rulesByPlugin = Maps.newHashMap(rulesByPlugin.asMap());
            update(measure.getData(), QualityProfilesView.this.rulesByPlugin);
          }
        }
        monitor.done();
        return Status.OK_STATUS;
      }
    };
    IWorkbenchSiteProgressService siteService = (IWorkbenchSiteProgressService) getSite().getAdapter(IWorkbenchSiteProgressService.class);
    siteService.schedule(job);
  }

}
