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
package org.sonarlint.eclipse.ui.internal.views.locations;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlow;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlowLocation;
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

  private static class FlowNode {

    private final String label;
    private final MarkerFlowLocation location;

    public FlowNode(MarkerFlowLocation location) {
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
    public int hashCode() {
      return Objects.hash(location.getParent().getNumber(), location.getNumber());
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof FlowNode)) {
        return false;
      }
      FlowNode other = (FlowNode) obj;
      return Objects.equals(location.getParent().getNumber(), other.location.getParent().getNumber()) && Objects.equals(location.getNumber(), other.location.getNumber());
    }

  }

  private static class FlowRootNode {

    private final List<FlowNode> children;
    private final MarkerFlow flow;

    public FlowRootNode(MarkerFlow flow) {
      this.flow = flow;
      children = flow.getLocations().stream()
        // SLE-388 - "Highlight-only" locations don't have a message
        .filter(l -> !StringUtils.isEmpty(l.getMessage()))
        .map(FlowNode::new)
        .collect(toList());
    }

    public MarkerFlow getFlow() {
      return flow;
    }

    public String getLabel() {
      return "Flow " + flow.getNumber();
    }

    public FlowNode[] getChildren() {
      return children.toArray(new FlowNode[0]);
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
      FlowRootNode other = (FlowRootNode) obj;
      return Objects.equals(flow.getNumber(), other.flow.getNumber());
    }

  }

  private static class RootNode {

    private final IMarker rootMarker;
    private final List<MarkerFlow> flowsMarkers;

    public RootNode(IMarker rootMarker, List<MarkerFlow> flowsMarkers) {
      this.rootMarker = rootMarker;
      this.flowsMarkers = flowsMarkers;
    }

    public IMarker getMarker() {
      return rootMarker;
    }

    public List<MarkerFlow> getFlows() {
      return flowsMarkers;
    }

  }

  private static class LocationsProvider implements ITreeContentProvider {

    @Override
    public Object[] getElements(Object inputElement) {
      IMarker sonarlintMarker = (IMarker) inputElement;
      List<MarkerFlow> flowsMarkers = MarkerUtils.getIssueFlows(sonarlintMarker);
      if (!flowsMarkers.isEmpty()) {
        return new Object[] {new RootNode(sonarlintMarker, flowsMarkers)};
      } else {
        return new Object[] {"No additional locations associated with this issue"};
      }
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof RootNode) {
        List<MarkerFlow> flows = ((RootNode) parentElement).getFlows();
        if (flows.size() > 1) {
          // Flatten if all flows have a single location
          if (flows.stream().allMatch(f -> f.getLocations().size() <= 1)) {
            return flows.stream().map(FlowRootNode::new).flatMap(f -> Stream.of(f.getChildren())).toArray();
          } else {
            return flows.stream().map(FlowRootNode::new).toArray();
          }
        } else if (flows.size() == 1) {
          // Don't show flow number
          return new FlowRootNode(flows.get(0)).getChildren();
        } else {
          return new Object[0];
        }
      } else if (parentElement instanceof FlowRootNode) {
        return ((FlowRootNode) parentElement).getChildren();
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
      } else if (element instanceof FlowNode) {
        return ((FlowNode) element).getLabel();
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
      Object element = cell.getElement();
      StyledString styledString = getStyledString(element);
      cell.setText(styledString.toString());
      cell.setImage(getImage(element));
      cell.setStyleRanges(styledString.getStyleRanges());
      super.update(cell);
    }

    private StyledString getStyledString(Object element) {
      return new StyledString(getText(element), isValidLocation(element) ? null : invalidLocationStyler);
    }

  }

  private static boolean isValidLocation(Object element) {
    if (element instanceof RootNode) {
      return ((RootNode) element).getMarker().exists();
    } else if (element instanceof FlowRootNode) {
      return Stream.of(((FlowRootNode) element).getChildren()).anyMatch(IssueLocationsView::isValidFlowLocation);
    } else if (element instanceof FlowNode) {
      return isValidFlowLocation(((FlowNode) element));
    } else if (element instanceof String) {
      return true;
    }
    throw new IllegalArgumentException("Unknow node type: " + element);
  }

  private static boolean isValidFlowLocation(FlowNode flowNode) {
    return flowNode.getLocation().getMarker().exists() && !flowNode.getLocation().isDeleted();
  }

  @Override
  public void markerSelected(Optional<IMarker> marker) {
    locationsViewer.setInput(marker.orElse(null));
    Optional<MarkerFlowLocation> lastSelectedFlowLocation = SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedFlowLocation();
    if (lastSelectedFlowLocation.isPresent()) {
      selectLocation(lastSelectedFlowLocation.get());
    } else {
      Optional<MarkerFlow> lastSelectedFlow = SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedFlow();
      if (lastSelectedFlow.isPresent()) {
        selectFlow(lastSelectedFlow.get());
      }
    }
  }

  @Override
  public void createPartControl(Composite parent) {
    createToolbar();
    Tree tree = new Tree(parent, SWT.SINGLE);
    locationsViewer = new TreeViewer(tree);
    locationsViewer.setAutoExpandLevel(ALL_LEVELS);
    locationsViewer.setUseHashlookup(true);
    locationsViewer.setContentProvider(new LocationsProvider());
    locationsViewer.setLabelProvider(new LocationsTreeLabelProvider());
    locationsViewer.addPostSelectionChangedListener(

      event -> {

        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
          Object firstElement = ((IStructuredSelection) selection).getFirstElement();
          if (firstElement == null) {
            return;
          }
          onTreeNodeSelected(firstElement);
        }
      });
    locationsViewer.addDoubleClickListener(

      event -> {

        ISelection selection = event.getSelection();
        if (selection instanceof IStructuredSelection) {
          Object firstElement = ((IStructuredSelection) selection).getFirstElement();
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
    } else if (selectedNode instanceof FlowNode) {
      SonarLintUiPlugin.getSonarlintMarkerSelectionService().flowLocationSelected(((FlowNode) selectedNode).getLocation());
    } else {
      throw new IllegalStateException("Unsupported node type");
    }
  }

  private static void onTreeNodeDoubleClick(Object node) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    if (node instanceof FlowNode) {
      IMarker flowMarker = ((FlowNode) node).getLocation().getMarker();
      if (flowMarker.exists()) {
        try {
          IDE.openEditor(page, flowMarker);
        } catch (PartInitException e) {
          SonarLintLogger.get().error(e.getMessage(), e);
        }
      }
    }
  }

  private void createToolbar() {
    IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
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
    locationsViewer.setSelection(new StructuredSelection(new FlowNode(location)), true);
  }

  public void selectFlow(MarkerFlow flow) {
    locationsViewer.setSelection(new StructuredSelection(new FlowRootNode(flow)), true);
  }

  public void refreshLabel(MarkerFlowLocation location) {
    locationsViewer.refresh(new FlowNode(location));
  }

}
