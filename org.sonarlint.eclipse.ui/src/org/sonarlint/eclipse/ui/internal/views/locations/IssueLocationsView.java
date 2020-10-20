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
import java.util.stream.Stream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.event.AnalysisEvent;
import org.sonarlint.eclipse.core.internal.event.AnalysisListener;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlow;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlowLocation;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.codemining.SonarLintCodeMiningProvider;
import org.sonarlint.eclipse.ui.internal.markers.AbstractMarkerSelectionListener;
import org.sonarlint.eclipse.ui.internal.markers.ShowIssueFlowsMarkerResolver;
import org.sonarlint.eclipse.ui.internal.util.LocationsUtils;

import static java.util.stream.Collectors.toList;

/**
 * Display details of a rule in a web browser
 */
public class IssueLocationsView extends ViewPart implements AbstractMarkerSelectionListener, AnalysisListener {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.IssueLocationsView";

  private TreeViewer locationsViewer;

  private ToggleAnnotationsAction showAnnotationsAction;

  private Object selectedNode;

  private Integer selectedFlow;

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
      children = flow.getLocations().stream().map(FlowNode::new).collect(toList());
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
      List<MarkerFlow> flowsMarkers = MarkerUtils.getIssueFlow(sonarlintMarker);
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

  private static class LocationsTreeLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
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

    @Override
    public Image getImage(Object element) {
      if (element instanceof RootNode) {
        return SonarLintImages.ISSUE_ANNOTATION;
      }
      return super.getImage(element);
    }

  }

  public void setInput(@Nullable IMarker sonarlintMarker) {
    ShowIssueFlowsMarkerResolver.removeAllAnnotations();
    locationsViewer.setInput(sonarlintMarker);
    if (sonarlintMarker != null && showAnnotationsAction.isChecked()) {
      ITextEditor editorFound = LocationsUtils.findOpenEditorFor(sonarlintMarker);
      if (editorFound != null) {
        selectedNode = null;
        selectedFlow = null;
        ShowIssueFlowsMarkerResolver.removeAllAnnotations();
        SonarLintCodeMiningProvider.setCurrentMarker(sonarlintMarker, getSelectedFlow());
      }
    }
  }


  @Override
  public void sonarlintIssueMarkerSelected(IMarker selectedMarker) {
    if (!Objects.equals(selectedMarker, locationsViewer.getInput())) {
      setInput(selectedMarker);
    }
  }

  @Override
  public void createPartControl(Composite parent) {
    createToolbar();
    Tree tree = new Tree(parent, SWT.MULTI);
    locationsViewer = new TreeViewer(tree);
    locationsViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
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
    startListeningForSelectionChanges(getSite().getPage());
    SonarLintCorePlugin.getAnalysisListenerManager().addListener(this);
  }

  private void onTreeNodeSelected(Object node) {
    selectedNode = node;
    showAnnotationsAction.run();
  }

  private static void onTreeNodeDoubleClick(Object node) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    if (node instanceof FlowNode) {
      IMarker flowMarker = ((FlowNode) node).getLocation().getMarker();
      try {
        IDE.openEditor(page, flowMarker);
      } catch (PartInitException e) {
        SonarLintLogger.get().error(e.getMessage(), e);
      }
    }
  }

  private void createToolbar() {
    IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
    showAnnotationsAction = new ToggleAnnotationsAction();
    toolbarManager.add(showAnnotationsAction);
    toolbarManager.add(new Separator());
    toolbarManager.update(false);
  }

  @Override
  public void setFocus() {
    // Nothing to do
  }

  @Override
  public void dispose() {
    stopListeningForSelectionChanges(getSite().getPage());
    SonarLintCorePlugin.getAnalysisListenerManager().removeListener(this);
    ShowIssueFlowsMarkerResolver.removeAllAnnotations();
    super.dispose();
  }

  @Override
  public void usedAnalysis(AnalysisEvent event) {
    IMarker marker = (IMarker) locationsViewer.getInput();
    Display.getDefault().asyncExec(() -> {
      if (marker != null && marker.exists()) {
        setInput(marker);
      } else {
        setInput(null);
      }
    });
  }

  public class ToggleAnnotationsAction extends Action {

    /**
     * Constructs a new action.
     */
    public ToggleAnnotationsAction() {
      super("Toggle annotations");
      setDescription("Show/hide annotations in editor");
      setToolTipText("Show/hide annotations in editor");
      setImageDescriptor(SonarLintImages.MARK_OCCURENCES_IMG);
      setChecked(true);
    }

    /**
     * Runs the action.
     */
    @Override
    public void run() {
      if (isChecked()) {
        showAnnotations();
      } else {
        ShowIssueFlowsMarkerResolver.removeAllAnnotations();
      }
    }

  }

  public void showAnnotations() {
    IMarker sonarlintMarker = (IMarker) locationsViewer.getInput();
    if (sonarlintMarker != null) {
      ITextEditor editorFound = LocationsUtils.findOpenEditorFor(sonarlintMarker);
      if (editorFound != null) {
        Integer newSelectedFlow = getSelectedFlow();
        // Clean annotations only when a different flow is selected to avoid flickering
        if (!newSelectedFlow.equals(selectedFlow)) {
          ShowIssueFlowsMarkerResolver.removeAllAnnotations();
          selectedFlow = newSelectedFlow;
        }
        ShowIssueFlowsMarkerResolver.showAnnotations(sonarlintMarker, editorFound, getSelectedFlow());
        SonarLintCodeMiningProvider.setCurrentMarker(sonarlintMarker, getSelectedFlow());
      }
    }
  }

  private int getSelectedFlow() {
    if (selectedNode instanceof FlowRootNode) {
      return ((FlowRootNode) selectedNode).getFlow().getNumber();
    } else if (selectedNode instanceof FlowNode) {
      return ((FlowNode) selectedNode).getLocation().getParent().getNumber();
    } else {
      // When root is selected, return first flow
      return 1;
    }
  }

  public void setShowAnnotations(boolean b) {
    showAnnotationsAction.setChecked(b);
  }

  public void selectLocation(MarkerFlowLocation location) {
    locationsViewer.setSelection(new StructuredSelection(new FlowNode(location)), true);
  }

}
