/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.views.locations;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlow;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlowLocation;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlows;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.flowlocations.SonarLintMarkerSelectionListener;

import static java.util.stream.Collectors.toList;
import static org.eclipse.jface.viewers.AbstractTreeViewer.ALL_LEVELS;

/**
 * Display details of a rule in a web browser
 */
public class IssueLocationsView extends ViewPart implements SonarLintMarkerSelectionListener {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.IssueLocationsView";

  private TreeViewer locationsViewer;

  private ToggleAnnotationsAction showAnnotationsAction;

  private interface LocationNode {
    boolean isValid();
  }

  private static class FlowLocationNode implements LocationNode {

    private final String label;
    private final MarkerFlowLocation location;

    public FlowLocationNode(MarkerFlowLocation location) {
      this.label = location.getParent().getLocations().size() > 1 ? (location.getNumber() + ": " + location.getMessage()) : location.getMessage();
      this.location = location;
    }

    public String getLabel() {
      return label;
    }

    public MarkerFlowLocation getLocation() {
      return location;
    }

    @Override
    public boolean isValid() {
      var marker = location.getMarker();
      return marker != null && marker.exists() && !location.isDeleted();
    }

    @Override
    public int hashCode() {
      return Objects.hash(location.getParent().getNumber(), location.getNumber());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof FlowLocationNode)) {
        return false;
      }
      FlowLocationNode other = (FlowLocationNode) obj;
      return Objects.equals(location.getParent().getNumber(), other.location.getParent().getNumber()) && Objects.equals(location.getNumber(), other.location.getNumber());
    }

  }

  private static class FlowRootNode {

    private final List<LocationNode> children;
    private final MarkerFlow flow;

    public FlowRootNode(MarkerFlow flow) {
      this.flow = flow;
      if (flow.areAllLocationsInSameFile()) {
        children = flow.getLocations().stream()
          // SLE-388 - "Highlight-only" locations don't have a message
          .filter(l -> !StringUtils.isEmpty(l.getMessage()))
          .map(FlowLocationNode::new)
          .collect(toList());
      } else {
        children = new ArrayList<>();
        LocationFileGroupNode lastNode = null;
        for (var location : flow.getLocations()) {
          if (lastNode == null || !lastNode.getFilePath().equals(location.getFilePath())) {
            lastNode = new LocationFileGroupNode(children.size(), location.getFilePath());
            children.add(lastNode);
          }
          lastNode.addLocation(new FlowLocationNode(location));
        }
      }
    }

    public MarkerFlow getFlow() {
      return flow;
    }

    public String getLabel() {
      return "Flow " + flow.getNumber();
    }

    public LocationNode[] getChildren() {
      return children.toArray(new LocationNode[0]);
    }

    @Override
    public int hashCode() {
      return Objects.hash(flow.getNumber());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof FlowRootNode)) {
        return false;
      }
      var other = (FlowRootNode) obj;
      return Objects.equals(flow.getNumber(), other.flow.getNumber());
    }

  }

  private static class LocationFileGroupNode implements LocationNode {

    private final int groupIndex;
    private final String filePath;
    private List<FlowLocationNode> children = new ArrayList<>();

    public LocationFileGroupNode(int groupIndex, String filePath) {
      this.groupIndex = groupIndex;
      this.filePath = filePath;
    }

    public void addLocation(FlowLocationNode flowLocationNode) {
      children.add(flowLocationNode);
    }

    public @Nullable String getFilePath() {
      return filePath;
    }

    public List<FlowLocationNode> getChildren() {
      return children;
    }

    @Override
    public boolean isValid() {
      return children.get(0).isValid();
    }

    @Override
    public int hashCode() {
      return Objects.hash(filePath, groupIndex);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof FlowRootNode)) {
        return false;
      }
      var other = (LocationFileGroupNode) obj;
      return Objects.equals(filePath, other.filePath) && Objects.equals(groupIndex, other.groupIndex);
    }

  }

  private static class RootNode {

    private final IMarker rootMarker;
    private final MarkerFlows flows;

    public RootNode(IMarker rootMarker, MarkerFlows flows) {
      this.rootMarker = rootMarker;
      this.flows = flows;
    }

    public IMarker getMarker() {
      return rootMarker;
    }

    public MarkerFlows getFlows() {
      return flows;
    }

  }

  private static class LocationsProvider implements ITreeContentProvider {

    @Override
    public Object[] getElements(Object inputElement) {
      var sonarlintMarker = (IMarker) inputElement;
      var flowsMarkers = MarkerUtils.getIssueFlows(sonarlintMarker);
      if (!flowsMarkers.isEmpty()) {
        return new Object[] {new RootNode(sonarlintMarker, flowsMarkers)};
      } else {
        return new Object[] {"No additional locations associated with this issue"};
      }
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof RootNode) {
        var flows = ((RootNode) parentElement).getFlows();
        if (flows.count() > 1) {
          // Flatten if all flows have a single location
          if (flows.isSecondaryLocations()) {
            return flows.getFlows().stream().map(FlowRootNode::new).flatMap(f -> Stream.of(f.getChildren())).toArray();
          } else {
            return flows.getFlows().stream().map(FlowRootNode::new).toArray();
          }
        } else if (flows.count() == 1) {
          // Don't show flow number
          return new FlowRootNode(flows.getFlows().get(0)).getChildren();
        } else {
          return new Object[0];
        }
      } else if (parentElement instanceof FlowRootNode) {
        return ((FlowRootNode) parentElement).getChildren();
      } else if (parentElement instanceof LocationFileGroupNode) {
        return ((LocationFileGroupNode) parentElement).getChildren().toArray();
      } else {
        return new Object[0];
      }
    }

    @Nullable
    @Override
    public Object getParent(Object element) {
      return null;
    }

    @Override
    public boolean hasChildren(Object element) {
      return getChildren(element).length > 0;
    }

    @Override
    public void dispose() {
      // Do nothing
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // Do nothing
    }

  }

  private static class LocationsTreeLabelProvider extends StyledCellLabelProvider {

    private final Styler invalidLocationStyler = new Styler() {
      @Override
      public void applyStyles(TextStyle textStyle) {
        textStyle.strikeout = true;
      }
    };

    private static String getText(Object element) {
      if (element instanceof RootNode) {
        return ((RootNode) element).getMarker().getAttribute(IMarker.MESSAGE, "No message");
      } else if (element instanceof FlowRootNode) {
        return ((FlowRootNode) element).getLabel();
      } else if (element instanceof FlowLocationNode) {
        return ((FlowLocationNode) element).getLabel();
      } else if (element instanceof LocationFileGroupNode) {
        return Paths.get(((LocationFileGroupNode) element).getFilePath()).getFileName().toString();
      } else if (element instanceof String) {
        return (String) element;
      }
      throw new IllegalArgumentException("Unknow node type: " + element);
    }

    private static @Nullable Image getImage(Object element) {
      if (element instanceof RootNode) {
        return SonarLintImages.ISSUE_ANNOTATION;
      }
      return null;
    }

    @Override
    public void update(ViewerCell cell) {
      var element = cell.getElement();
      var styledString = getStyledString(element);
      cell.setText(styledString.toString());
      cell.setImage(getImage(element));
      cell.setStyleRanges(styledString.getStyleRanges());
      super.update(cell);
    }

    private StyledString getStyledString(Object element) {
      return new StyledString(getText(element), isValidLocation(element) ? null : invalidLocationStyler);
    }

    private static boolean isValidLocation(Object element) {
      if (element instanceof RootNode) {
        return ((RootNode) element).getMarker().exists();
      } else if (element instanceof FlowRootNode) {
        return Stream.of(((FlowRootNode) element).getChildren()).anyMatch(LocationNode::isValid);
      } else if (element instanceof FlowLocationNode) {
        return ((FlowLocationNode) element).isValid();
      } else if (element instanceof LocationFileGroupNode) {
        return ((LocationFileGroupNode) element).isValid();
      } else if (element instanceof String) {
        return true;
      }
      throw new IllegalArgumentException("Unknown node type: " + element);
    }

  }

  @Override
  public void markerSelected(Optional<IMarker> marker) {
    locationsViewer.setInput(marker.orElse(null));
    var lastSelectedFlowLocation = SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedFlowLocation();
    if (lastSelectedFlowLocation.isPresent()) {
      selectLocation(lastSelectedFlowLocation.get());
    } else {
      var lastSelectedFlow = SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedFlow();
      if (lastSelectedFlow.isPresent()) {
        selectFlow(lastSelectedFlow.get());
      }
    }
  }

  @Override
  public void createPartControl(Composite parent) {
    createToolbar();
    var tree = new Tree(parent, SWT.SINGLE);
    locationsViewer = new TreeViewer(tree);
    locationsViewer.setAutoExpandLevel(ALL_LEVELS);
    locationsViewer.setUseHashlookup(true);
    locationsViewer.setContentProvider(new LocationsProvider());
    locationsViewer.setLabelProvider(new LocationsTreeLabelProvider());
    locationsViewer.addPostSelectionChangedListener(

      event -> {
        var selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
          var firstElement = ((IStructuredSelection) selection).getFirstElement();
          if (firstElement == null) {
            return;
          }
          onTreeNodeSelected(firstElement);
        }
      });
    locationsViewer.addDoubleClickListener(

      event -> {
        var selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
          var firstElement = ((IStructuredSelection) selection).getFirstElement();
          if (firstElement == null) {
            return;
          }
          onTreeNodeDoubleClick(firstElement);
        }
      });
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().addMarkerSelectionListener(this);
    locationsViewer.setInput(SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedMarker().orElse(null));
  }

  private static void onTreeNodeSelected(Object selectedNode) {
    if (selectedNode instanceof RootNode) {
      SonarLintUiPlugin.getSonarlintMarkerSelectionService().flowSelected(null);
    } else if (selectedNode instanceof FlowRootNode) {
      SonarLintUiPlugin.getSonarlintMarkerSelectionService().flowSelected(((FlowRootNode) selectedNode).getFlow());
    } else if (selectedNode instanceof FlowLocationNode) {
      SonarLintUiPlugin.getSonarlintMarkerSelectionService().flowLocationSelected(((FlowLocationNode) selectedNode).getLocation());
    } else if (selectedNode instanceof LocationFileGroupNode) {
      SonarLintUiPlugin.getSonarlintMarkerSelectionService().flowLocationSelected(((LocationFileGroupNode) selectedNode).getChildren().get(0).getLocation());
    } else if (selectedNode instanceof String) {
      // No secondary locations: nothing to react upon
    } else {
      throw new IllegalStateException("Unsupported node type");
    }
  }

  private static void onTreeNodeDoubleClick(Object node) {
    var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    MarkerFlowLocation location = null;
    if (node instanceof FlowLocationNode) {
      location = ((FlowLocationNode) node).getLocation();
    } else if (node instanceof LocationFileGroupNode) {
      location = ((LocationFileGroupNode) node).getChildren().get(0).getLocation();
    }
    if (location != null) {
      var flowMarker = location.getMarker();
      if (flowMarker != null && flowMarker.exists()) {
        try {
          IDE.openEditor(page, flowMarker);
        } catch (PartInitException e) {
          SonarLintLogger.get().error(e.getMessage(), e);
        }
      }
    }
  }

  private void createToolbar() {
    var toolbarManager = getViewSite().getActionBars().getToolBarManager();
    showAnnotationsAction = new ToggleAnnotationsAction();
    showAnnotationsAction.setChecked(SonarLintUiPlugin.getSonarlintMarkerSelectionService().isShowAnnotationsInEditor());
    toolbarManager.add(showAnnotationsAction);
    toolbarManager.add(new Separator());
    toolbarManager.update(false);
  }

  @Override
  public void setFocus() {
    locationsViewer.getTree().setFocus();
  }

  @Override
  public void dispose() {
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().removeMarkerSelectionListener(this);
    // Unselect marker to make annotations disappear
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().markerSelected(null, false, false);
    super.dispose();
  }

  private static class ToggleAnnotationsAction extends Action {

    /**
     * Constructs a new action.
     */
    public ToggleAnnotationsAction() {
      super("Toggle annotations");
      setDescription("Show/hide annotations in editor");
      setToolTipText("Show/hide annotations in editor");
      setImageDescriptor(SonarLintImages.MARK_OCCURENCES_IMG);
    }

    /**
     * Runs the action.
     */
    @Override
    public void run() {
      SonarLintUiPlugin.getSonarlintMarkerSelectionService().setShowAnnotationsInEditor(isChecked());
    }

  }

  public void setShowAnnotations(boolean b) {
    showAnnotationsAction.setChecked(b);
  }

  public void selectLocation(MarkerFlowLocation location) {
    locationsViewer.setSelection(new StructuredSelection(new FlowLocationNode(location)), true);
  }

  public void selectFlow(MarkerFlow flow) {
    locationsViewer.setSelection(new StructuredSelection(new FlowRootNode(flow)), true);
  }

  public void refreshLabel(MarkerFlowLocation location) {
    locationsViewer.refresh(new FlowLocationNode(location));
  }

}
