/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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

import java.util.stream.Stream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.rule.RuleDescriptionPanel;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.LocationsUtils;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.RuleMonolithicDescriptionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.hotspot.HotspotDetailsDto;

public class HotspotsView extends ViewPart {
  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.HotspotsView";

  private PageBook book;
  private Control hotspotsPage;
  private TableViewer hotspotViewer;
  private SashForm splitter;

  private ScrolledComposite riskDescriptionScrolledComposite;
  private ScrolledComposite vulnerabilityDescriptionScrolledComposite;
  private ScrolledComposite fixRecommendationsScrolledComposite;

  private Composite riskDescriptionScrolledContent;
  private Composite vulnerabilityDescriptionScrolledContent;
  private Composite fixRecommendationsScrolledContent;

  private Control riskDescriptionContent;
  private Control vulnerabilityDescriptionContent;
  private Control fixRecommendationsContent;

  @Override
  public void createPartControl(Composite parent) {
    var toolkit = new FormToolkit(parent.getDisplay());
    book = new PageBook(parent, SWT.NONE);

    var noHotspotsMessage = createNoHotspotsMessage(toolkit);
    hotspotsPage = createHotspotsPage(toolkit);
    book.showPage(noHotspotsMessage);
  }

  private Control createNoHotspotsMessage(FormToolkit kit) {
    var form = kit.createForm(book);
    var body = form.getBody();
    var layout = new GridLayout();
    body.setLayout(layout);

    var emptyMsg = new Link(body, SWT.CENTER | SWT.WRAP);
    emptyMsg.setText("You can open a Security Hotspot from SonarQube Server. <a>Learn more</a>");
    var gd = new GridData(SWT.LEFT, SWT.FILL, true, false);
    emptyMsg.setLayoutData(gd);
    emptyMsg.setBackground(emptyMsg.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    emptyMsg.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        BrowserUtils.openExternalBrowser(SonarLintDocumentation.SECURITY_HOTSPOTS_LINK, e.display);
      }
    });
    return form;
  }

  private Control createHotspotsPage(FormToolkit kit) {
    var form = kit.createForm(book);
    var body = form.getBody();
    var layout = new GridLayout();
    body.setLayout(layout);

    splitter = new SashForm(body, SWT.NONE);
    var gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    splitter.setLayoutData(gd);
    splitter.setOrientation(SWT.HORIZONTAL);

    createHotspotTable();

    var tabFolder = new TabFolder(splitter, SWT.NONE);

    var riskDescriptionTab = new TabItem(tabFolder, SWT.NONE);
    riskDescriptionTab.setText("What's the risk?");
    riskDescriptionTab.setToolTipText("Risk description");
    riskDescriptionScrolledComposite = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
    riskDescriptionScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    riskDescriptionScrolledComposite.setExpandHorizontal(true);
    riskDescriptionScrolledComposite.setExpandVertical(true);
    riskDescriptionScrolledContent = new Composite(riskDescriptionScrolledComposite, SWT.NONE);
    riskDescriptionScrolledContent.setLayout(new GridLayout(1, false));
    riskDescriptionScrolledComposite.setContent(riskDescriptionScrolledContent);
    riskDescriptionTab.setControl(riskDescriptionScrolledComposite);

    var vulnerabilityDescriptionTab = new TabItem(tabFolder, SWT.NONE);
    vulnerabilityDescriptionTab.setText("Are you at risk?");
    vulnerabilityDescriptionTab.setToolTipText("Vulnerability description");
    vulnerabilityDescriptionScrolledComposite = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
    vulnerabilityDescriptionScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    vulnerabilityDescriptionScrolledComposite.setExpandHorizontal(true);
    vulnerabilityDescriptionScrolledComposite.setExpandVertical(true);
    vulnerabilityDescriptionScrolledContent = new Composite(vulnerabilityDescriptionScrolledComposite, SWT.NONE);
    vulnerabilityDescriptionScrolledContent.setLayout(new GridLayout(1, false));
    vulnerabilityDescriptionScrolledComposite.setContent(vulnerabilityDescriptionScrolledContent);
    vulnerabilityDescriptionTab.setControl(vulnerabilityDescriptionScrolledComposite);

    var fixRecommendationsTab = new TabItem(tabFolder, SWT.NONE);
    fixRecommendationsTab.setText("How can you fix it?");
    fixRecommendationsTab.setToolTipText("Recommendations");
    fixRecommendationsScrolledComposite = new ScrolledComposite(tabFolder, SWT.V_SCROLL);
    fixRecommendationsScrolledComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    fixRecommendationsScrolledComposite.setExpandHorizontal(true);
    fixRecommendationsScrolledComposite.setExpandVertical(true);
    fixRecommendationsScrolledContent = new Composite(fixRecommendationsScrolledComposite, SWT.NONE);
    fixRecommendationsScrolledContent.setLayout(new GridLayout(1, false));
    fixRecommendationsScrolledComposite.setContent(fixRecommendationsScrolledContent);
    fixRecommendationsTab.setControl(fixRecommendationsScrolledComposite);

    updateScrollCompositeMinSize();
    var listener = new ControlListener() {
      @Override
      public void controlMoved(ControlEvent e) {
      }

      @Override
      public void controlResized(ControlEvent e) {
        updateScrollCompositeMinSize();
      }
    };
    riskDescriptionScrolledComposite.addControlListener(listener);
    vulnerabilityDescriptionScrolledComposite.addControlListener(listener);
    fixRecommendationsScrolledComposite.addControlListener(listener);

    clearRule();

    return form;
  }

  private void updateScrollCompositeMinSize() {
    riskDescriptionScrolledComposite.setMinSize(
      riskDescriptionScrolledContent.computeSize(
        riskDescriptionScrolledComposite.getClientArea().width, SWT.DEFAULT));
    riskDescriptionScrolledComposite.requestLayout();
    vulnerabilityDescriptionScrolledComposite.setMinSize(
      vulnerabilityDescriptionScrolledContent.computeSize(
        vulnerabilityDescriptionScrolledComposite.getClientArea().width, SWT.DEFAULT));
    vulnerabilityDescriptionScrolledComposite.requestLayout();
    fixRecommendationsScrolledComposite.setMinSize(
      fixRecommendationsScrolledContent.computeSize(
        fixRecommendationsScrolledComposite.getClientArea().width, SWT.DEFAULT));
    fixRecommendationsScrolledComposite.requestLayout();
  }

  @Nullable
  private HotspotDetailsDto getSelectedHotspot() {
    var firstElement = hotspotViewer.getStructuredSelection().getFirstElement();
    return firstElement != null ? ((HotspotAndMarker) firstElement).hotspot : null;
  }

  private void createHotspotTable() {
    hotspotViewer = new TableViewer(splitter, SWT.H_SCROLL | SWT.V_SCROLL | SWT.HIDE_SELECTION | SWT.FULL_SELECTION | SWT.SINGLE | SWT.READ_ONLY) {
      @Override
      protected void handleDispose(DisposeEvent event) {
        clearMarkers();
        super.handleDispose(event);
      }
    };

    final var table = hotspotViewer.getTable();
    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    // Deselect line when clicking on a blank space in the table
    table.addListener(SWT.MouseDown, event -> {
      var item = table.getItem(new Point(event.x, event.y));
      if (item == null) {
        // No table item at the click location?
        hotspotViewer.setSelection(StructuredSelection.EMPTY);
      }
    });

    hotspotViewer.addPostSelectionChangedListener(event -> {
      var hotspot = getSelectedHotspot();
      if (hotspot == null) {
        clearRule();
      } else {
        updateRule(hotspot);
      }
    });

    hotspotViewer.addDoubleClickListener(event -> openMarkerOfSelectedHotspot());

    var colPriority = new TableViewerColumn(hotspotViewer, SWT.NONE);
    colPriority.getColumn().setText("Priority");
    colPriority.getColumn().setResizable(true);
    colPriority.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public Image getImage(Object element) {
        switch (((HotspotAndMarker) element).hotspot.getRule().getVulnerabilityProbability()) {
          case "HIGH":
            return SonarLintImages.IMG_HOTSPOT_HIGH;
          case "MEDIUM":
            return SonarLintImages.IMG_HOTSPOT_MEDIUM;
          case "LOW":
            return SonarLintImages.IMG_HOTSPOT_LOW;
          default:
            throw new IllegalStateException("Unexpected probability");
        }
      }

      @Nullable
      @Override
      public String getText(Object element) {
        return null;
      }
    });

    var colDescription = new TableViewerColumn(hotspotViewer, SWT.NONE);
    colDescription.getColumn().setText("Description");
    colDescription.getColumn().setResizable(true);
    colDescription.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        var locationValid = isLocationValid(element);

        return ((HotspotAndMarker) element).hotspot.getMessage() + (locationValid ? "" : " (Local code not matching)");
      }

      private boolean isLocationValid(Object element) {
        boolean locationValid;
        var marker = ((HotspotAndMarker) element).marker;
        if (marker != null) {
          locationValid = marker.exists() && marker.getAttribute(IMarker.CHAR_START, -1) >= 0;
          var editor = LocationsUtils.findOpenEditorFor(marker);
          if (editor != null) {
            var p = LocationsUtils.getMarkerPosition(marker, editor);
            locationValid = locationValid && p != null && !p.isDeleted();
          }
        } else {
          locationValid = false;
        }
        return locationValid;
      }
    });

    var colCategory = new TableViewerColumn(hotspotViewer, SWT.NONE);
    colCategory.getColumn().setText("Category");
    colCategory.getColumn().setResizable(true);
    colCategory.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        var hotspotAndMarker = (HotspotAndMarker) element;
        return SecurityHotspotCategory.findByShortName(hotspotAndMarker.hotspot.getRule().getSecurityCategory())
          .map(SecurityHotspotCategory::getLongName)
          .orElse(hotspotAndMarker.hotspot.getRule().getSecurityCategory());
      }
    });

    var colResource = new TableViewerColumn(hotspotViewer, SWT.NONE);
    colResource.getColumn().setText("Resource");
    colResource.getColumn().setResizable(true);
    colResource.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        var hotspotAndMarker = (HotspotAndMarker) element;
        return hotspotAndMarker.marker != null ? hotspotAndMarker.marker.getResource().getName() : "";
      }
    });

    var colLine = new TableViewerColumn(hotspotViewer, SWT.NONE);
    colLine.getColumn().setText("Location");
    colLine.getColumn().setResizable(true);
    colLine.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        var hotspotAndMarker = (HotspotAndMarker) element;
        return "line " + hotspotAndMarker.hotspot.getTextRange().getStartLine();
      }
    });

    var colRuleKey = new TableViewerColumn(hotspotViewer, SWT.NONE);
    colRuleKey.getColumn().setText("Rule");
    colRuleKey.getColumn().setResizable(true);
    colRuleKey.setLabelProvider(new ColumnLabelProvider() {
      @Override
      public String getText(Object element) {
        var hotspotAndMarker = (HotspotAndMarker) element;
        return hotspotAndMarker.hotspot.getRule().getKey();
      }
    });

    hotspotViewer.setContentProvider(ArrayContentProvider.getInstance());
  }

  private void clearRule() {
    final var NO_SECURITY_HOTSPOTS_SELECTED = "No Security Hotspots selected";

    if (riskDescriptionContent != null && !riskDescriptionContent.isDisposed()) {
      riskDescriptionContent.dispose();
    }
    riskDescriptionContent = new Label(riskDescriptionScrolledContent, SWT.NONE);
    riskDescriptionContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    ((Label) riskDescriptionContent).setText(NO_SECURITY_HOTSPOTS_SELECTED);

    if (vulnerabilityDescriptionContent != null && !vulnerabilityDescriptionContent.isDisposed()) {
      vulnerabilityDescriptionContent.dispose();
    }
    vulnerabilityDescriptionContent = new Label(vulnerabilityDescriptionScrolledContent, SWT.NONE);
    vulnerabilityDescriptionContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    ((Label) vulnerabilityDescriptionContent).setText(NO_SECURITY_HOTSPOTS_SELECTED);

    if (fixRecommendationsContent != null && !fixRecommendationsContent.isDisposed()) {
      fixRecommendationsContent.dispose();
    }
    fixRecommendationsContent = new Label(fixRecommendationsScrolledContent, SWT.NONE);
    fixRecommendationsContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    ((Label) fixRecommendationsContent).setText(NO_SECURITY_HOTSPOTS_SELECTED);

    // None of the labels is really "scrollable", therefore only update the contents!
    riskDescriptionContent.requestLayout();
    vulnerabilityDescriptionContent.requestLayout();
    fixRecommendationsContent.requestLayout();
  }

  private void updateRule(HotspotDetailsDto details) {
    // HotspotRule does not contain the language but it can be derived from the rule key
    var rule = details.getRule();
    var languageKey = rule.getKey().split(":")[0];

    if (riskDescriptionContent != null && !riskDescriptionContent.isDisposed()) {
      riskDescriptionContent.dispose();
    }
    riskDescriptionContent = new RuleDescriptionPanel(riskDescriptionScrolledContent, languageKey, true);
    riskDescriptionContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    ((RuleDescriptionPanel) riskDescriptionContent).updateMonolithicRule(
      new RuleMonolithicDescriptionDto(rule.getRiskDescription()));

    if (vulnerabilityDescriptionContent != null && !vulnerabilityDescriptionContent.isDisposed()) {
      vulnerabilityDescriptionContent.dispose();
    }
    vulnerabilityDescriptionContent = new RuleDescriptionPanel(vulnerabilityDescriptionScrolledContent, languageKey, true);
    vulnerabilityDescriptionContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    ((RuleDescriptionPanel) vulnerabilityDescriptionContent).updateMonolithicRule(
      new RuleMonolithicDescriptionDto(rule.getVulnerabilityDescription()));

    if (fixRecommendationsContent != null && !fixRecommendationsContent.isDisposed()) {
      fixRecommendationsContent.dispose();
    }
    fixRecommendationsContent = new RuleDescriptionPanel(fixRecommendationsScrolledContent, languageKey, true);
    fixRecommendationsContent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    ((RuleDescriptionPanel) fixRecommendationsContent).updateMonolithicRule(
      new RuleMonolithicDescriptionDto(rule.getFixRecommendations()));

    updateScrollCompositeMinSize();
  }

  public void openHotspot(HotspotDetailsDto hotspot, @Nullable IMarker marker) {
    clearMarkers();

    var hotspotAndMarker = new HotspotAndMarker(hotspot, marker);

    book.showPage(hotspotsPage);
    hotspotViewer.setInput(new HotspotAndMarker[] {hotspotAndMarker});
    hotspotViewer.setSelection(new StructuredSelection(hotspotAndMarker));
    hotspotViewer.refresh();
    for (var column : hotspotViewer.getTable().getColumns()) {
      column.pack();
    }
    splitter.requestLayout();

    openMarkerOfSelectedHotspot();
  }

  private void clearMarkers() {
    var previous = (HotspotAndMarker[]) hotspotViewer.getInput();
    if (previous != null) {
      Stream.of(previous).forEach(h -> {
        if (h.marker != null) {
          try {
            h.marker.delete();
          } catch (CoreException e) {
            SonarLintLogger.get().error("Unable to delete previous marker", e);
          }
        }
      });
    }
  }

  private void openMarkerOfSelectedHotspot() {
    var firstElement = hotspotViewer.getStructuredSelection().getFirstElement();
    var marker = firstElement != null ? ((HotspotAndMarker) firstElement).marker : null;
    if (marker != null && marker.exists()) {
      try {
        var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IDE.openEditor(page, marker);
      } catch (PartInitException e) {
        SonarLintLogger.get().error("Unable to open editor with hotspot", e);
      }
    }
  }

  private static class HotspotAndMarker {
    private final HotspotDetailsDto hotspot;
    @Nullable
    private final IMarker marker;

    public HotspotAndMarker(HotspotDetailsDto hotspot, @Nullable IMarker marker) {
      this.hotspot = hotspot;
      this.marker = marker;
    }
  }

  @Override
  public void setFocus() {
    if (hotspotsPage.isVisible()) {
      hotspotViewer.getTable().setFocus();
    } else {
      book.setFocus();
    }
  }
}
