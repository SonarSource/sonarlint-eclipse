/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal.hotspots;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.flowlocations.SonarLintMarkerSelectionListener;
import org.sonarlint.eclipse.ui.internal.util.SonarLintWebView;
import org.sonarsource.sonarlint.core.client.api.common.TextRange;
import org.sonarsource.sonarlint.core.serverapi.hotspot.ServerHotspot;
import org.sonarsource.sonarlint.core.serverapi.hotspot.ServerHotspot.Resolution;
import org.sonarsource.sonarlint.core.serverapi.hotspot.ServerHotspot.Rule;
import org.sonarsource.sonarlint.core.serverapi.hotspot.ServerHotspot.Rule.Probability;
import org.sonarsource.sonarlint.core.serverapi.hotspot.ServerHotspot.Status;

public class HotspotsView extends ViewPart implements SonarLintMarkerSelectionListener {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.HotspotsView";
  private PageBook book;
  private Control hotspotsPage;
  private TableViewer hotspotViewer;
  private SashForm splitter;

  private Color highPriorityColor;
  private Color mediumPriorityColor;
  private Color lowPriorityColor;
  private SonarLintWebView riskDescriptionBrowser;
  private SonarLintWebView vulnerabilityDescriptionBrowser;
  private SonarLintWebView fixRecommendationsBrowser;

  @Override
  public void markerSelected(Optional<IMarker> marker) {
  }

  @Override
  public void createPartControl(Composite parent) {
    highPriorityColor = new Color(parent.getDisplay(), 212, 51, 63);
    mediumPriorityColor = new Color(parent.getDisplay(), 237, 125, 32);
    lowPriorityColor = new Color(parent.getDisplay(), 234, 190, 6);

    SonarLintUiPlugin.getSonarlintMarkerSelectionService().addMarkerSelectionListener(this);

    createToolbar();

    FormToolkit toolkit = new FormToolkit(parent.getDisplay());
    book = new PageBook(parent, SWT.NONE);

    Control noHotspotsMessage = createNoHotspotsMessage(toolkit);
    hotspotsPage = createHotspotsPage(toolkit);
    book.showPage(noHotspotsMessage);

  }

  private void createToolbar() {
    IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
    OpenFakeHotspotAction action = new OpenFakeHotspotAction();
    toolbarManager.add(action);
    toolbarManager.add(new Separator());
    toolbarManager.update(false);
  }

  private class OpenFakeHotspotAction extends Action {

    /**
     * Constructs a new action.
     */
    public OpenFakeHotspotAction() {
      super("Open fake hotspot");
      setDescription("Open fake hotspot");
      setToolTipText("Open fake hotspot");
      setImageDescriptor(SonarLintImages.DEBUG);
    }

    /**
     * Runs the action.
     */
    @Override
    public void run() {
      ServerHotspot hotspot = new ServerHotspot("Some message", "foo/bar/Foo.java", new TextRange(1, 2, 3, 4), "henryju", Status.TO_REVIEW, Resolution.FIXED,
        new Rule("java:S123", "Do not do this", "foo", Probability.LOW, "Risk", "Vulnerability", "Fix"));
      openHotspot(hotspot);
    }

  }

  private Control createNoHotspotsMessage(FormToolkit kit) {
    Form form = kit.createForm(book);
    Composite body = form.getBody();
    GridLayout layout = new GridLayout();
    body.setLayout(layout);

    Link emptyMsg = new Link(body, SWT.CENTER | SWT.WRAP);
    emptyMsg.setText("You can open a Security Hotspot from SonarQube. <a>More infos</a>");
    GridData gd = new GridData(SWT.LEFT, SWT.FILL, true, false);
    emptyMsg.setLayoutData(gd);
    emptyMsg.setBackground(emptyMsg.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    emptyMsg.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        try {
          PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://github.com/SonarSource/sonarlint-eclipse/wiki/Security-Hotspots"));
        } catch (PartInitException | MalformedURLException e1) {
          // ignore
        }
      }
    });
    return form;
  }

  private Control createHotspotsPage(FormToolkit kit) {
    Form form = kit.createForm(book);
    Composite body = form.getBody();
    GridLayout layout = new GridLayout();
    body.setLayout(layout);

    splitter = new SashForm(body, SWT.NONE);
    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    splitter.setLayoutData(gd);
    splitter.setOrientation(SWT.HORIZONTAL);

    createHotspotTable();

    final TabFolder tabFolder = new TabFolder(splitter, SWT.NONE);

    TabItem riskDescriptionTab = new TabItem(tabFolder, SWT.NONE);
    riskDescriptionTab.setText("What's the risk?");
    riskDescriptionTab.setToolTipText("Risk decription");
    riskDescriptionTab.setControl(createRiskDescriptionControl(tabFolder));

    TabItem vulnerabilityDescriptionTab = new TabItem(tabFolder, SWT.NONE);
    vulnerabilityDescriptionTab.setText("Are you at risk?");
    vulnerabilityDescriptionTab.setToolTipText("Vulnerability decription");
    vulnerabilityDescriptionTab.setControl(createVulnerabilityDescriptionControl(tabFolder));

    TabItem fixRecommendationsTab = new TabItem(tabFolder, SWT.NONE);
    fixRecommendationsTab.setText("How can you fix it?");
    fixRecommendationsTab.setToolTipText("Recommendations");
    fixRecommendationsTab.setControl(createFixRecommendationsControl(tabFolder));

    return form;
  }

  private Control createRiskDescriptionControl(Composite parent) {
    riskDescriptionBrowser = new SonarLintWebView(parent, true) {

      @Override
      protected String body() {
        ServerHotspot hotspot = (ServerHotspot) hotspotViewer.getStructuredSelection().getFirstElement();
        if (hotspot == null) {
          return "<em>No security hotspots selected<em>";
        }
        return hotspot.rule.riskDescription;
      }
    };
    return riskDescriptionBrowser;
  }

  private Control createVulnerabilityDescriptionControl(Composite parent) {
    vulnerabilityDescriptionBrowser = new SonarLintWebView(parent, true) {

      @Override
      protected String body() {
        ServerHotspot hotspot = (ServerHotspot) hotspotViewer.getStructuredSelection().getFirstElement();
        if (hotspot == null) {
          return "<em>No security hotspots selected<em>";
        }
        return hotspot.rule.vulnerabilityDescription;
      }
    };
    return vulnerabilityDescriptionBrowser;
  }

  private Control createFixRecommendationsControl(Composite parent) {
    fixRecommendationsBrowser = new SonarLintWebView(parent, true) {

      @Override
      protected String body() {
        ServerHotspot hotspot = (ServerHotspot) hotspotViewer.getStructuredSelection().getFirstElement();
        if (hotspot == null) {
          return "<em>No security hotspots selected<em>";
        }
        return hotspot.rule.fixRecommendations;
      }
    };
    return fixRecommendationsBrowser;
  }

  private void createHotspotTable() {
    hotspotViewer = new TableViewer(splitter, SWT.H_SCROLL | SWT.V_SCROLL | SWT.HIDE_SELECTION | SWT.FULL_SELECTION | SWT.SINGLE | SWT.READ_ONLY);

    final Table table = hotspotViewer.getTable();
    table.setHeaderVisible(false);
    table.setLinesVisible(false);

    // Deselect line when clicking on a blank space in the table
    table.addListener(SWT.MouseDown, event -> {

      TableItem item = table.getItem(new Point(event.x, event.y));

      if (item == null) {
        // No table item at the click location?
        hotspotViewer.setSelection(StructuredSelection.EMPTY);
      }
    });

    hotspotViewer.addPostSelectionChangedListener(new ISelectionChangedListener() {

      @Override
      public void selectionChanged(SelectionChangedEvent event) {
        riskDescriptionBrowser.refresh();
        vulnerabilityDescriptionBrowser.refresh();
        fixRecommendationsBrowser.refresh();
      }
    });

    // The first column is always left aligned at least on Linux... so add an empty one
    TableViewerColumn colEmpty = new TableViewerColumn(hotspotViewer, SWT.NONE);
    colEmpty.getColumn().setWidth(0);
    colEmpty.setLabelProvider(new ColumnLabelProvider() {
      @Nullable
      @Override
      public String getText(Object element) {
        return null;
      }
    });

    TableViewerColumn colPriority = new TableViewerColumn(hotspotViewer, SWT.NONE);
    colPriority.getColumn().setText("Review priority");
    colPriority.getColumn().setAlignment(SWT.CENTER);
    colPriority.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return ((ServerHotspot) element).rule.vulnerabilityProbability.name();
      }

      @Override
      public Color getBackground(Object element) {
        switch (((ServerHotspot) element).rule.vulnerabilityProbability) {
          case HIGH:
            return highPriorityColor;
          case MEDIUM:
            return mediumPriorityColor;
          case LOW:
            return lowPriorityColor;
          default:
            throw new IllegalStateException("Unexpected probablility");
        }
      }

      @Override
      public Color getForeground(Object element) {
        return table.getDisplay().getSystemColor(SWT.COLOR_WHITE);
      }
    });

    TableViewerColumn colMessage = new TableViewerColumn(hotspotViewer, SWT.NONE);
    colMessage.getColumn().setText("Description");
    colMessage.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        return ((ServerHotspot) element).message;
      }
    });

    hotspotViewer.setContentProvider(ArrayContentProvider.getInstance());
  }

  public void openHotspot(ServerHotspot hotspot) {
    book.showPage(hotspotsPage);
    hotspotViewer.setInput(new ServerHotspot[] {hotspot});
    hotspotViewer.refresh();
    for (TableColumn c : hotspotViewer.getTable().getColumns()) {
      c.pack();
    }
    splitter.layout();
  }

  @Override
  public void setFocus() {
    hotspotViewer.getTable().forceFocus();
  }

  @Override
  public void dispose() {
    highPriorityColor.dispose();
    mediumPriorityColor.dispose();
    lowPriorityColor.dispose();
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().removeMarkerSelectionListener(this);
    super.dispose();
  }

}
