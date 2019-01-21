/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.event.AnalysisEvent;
import org.sonarlint.eclipse.core.internal.event.AnalysisListener;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils.ExtraPosition;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.markers.ShowIssueFlowsMarkerResolver;
import org.sonarlint.eclipse.ui.internal.util.LocationsUtils;
import org.sonarlint.eclipse.ui.internal.views.RuleDescriptionWebView;

/**
 * Display details of a rule in a web browser
 */
public class IssueLocationsView extends ViewPart implements ISelectionListener, AnalysisListener {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.IssueLocationsView";

  private TreeViewer locationsViewer;

  private ToggleAnnotationsAction showAnnotationsAction;

  private static class FlowNode {

    private final String label;
    private final ExtraPosition position;

    public FlowNode(ExtraPosition position) {
      this(positionLabel(position.getMessage()), position);
    }

    public FlowNode(String label, ExtraPosition position) {
      this.label = label;
      this.position = position;
    }

    public String getLabel() {
      return label;
    }

    public ExtraPosition getPosition() {
      return position;
    }

  }

  static List<FlowNode> toFlowNodes(List<ExtraPosition> positions) {
    List<FlowNode> result = new ArrayList<>();
    int id = 1;
    for (ExtraPosition extraPosition : positions) {
      String childLabel = positions.size() > 1 ? (id + ": " + positionLabel(extraPosition.getMessage())) : positionLabel(extraPosition.getMessage());
      result.add(new FlowNode(childLabel, extraPosition));
      id++;
    }
    return result;
  }

  private static String positionLabel(@Nullable String message) {
    return message != null ? message : "";
  }

  private static class FlowRootNode {

    private final String label;
    private final List<FlowNode> children;

    public FlowRootNode(String label, List<ExtraPosition> positions) {
      this.label = label;
      children = toFlowNodes(positions);
    }

    public String getLabel() {
      return label;
    }

    public FlowNode[] getChildren() {
      return children.toArray(new FlowNode[0]);
    }

  }

  private static class RootNode {

    private final IMarker rootMarker;
    private final List<List<ExtraPosition>> flows;

    public RootNode(IMarker rootMarker, List<ExtraPosition> positions) {
      this.rootMarker = rootMarker;
      this.flows = rebuildFlows(positions);
    }

    private static List<List<ExtraPosition>> rebuildFlows(List<ExtraPosition> positions) {
      List<List<ExtraPosition>> result = new ArrayList<>();
      List<ExtraPosition> roots = positions.stream().filter(p -> p.getParent() == null).collect(Collectors.toList());
      for (ExtraPosition root : roots) {
        List<ExtraPosition> flow = new ArrayList<>();
        Optional<ExtraPosition> current = Optional.of(root);
        while (current.isPresent()) {
          ExtraPosition currentValue = current.get();
          flow.add(currentValue);
          current = positions.stream().filter(

            p -> p.getParent() == currentValue).findFirst();
        }
        result.add(flow);

      }
      return result;
    }

    public IMarker getMarker() {
      return rootMarker;
    }

    public List<List<ExtraPosition>> getFlows() {
      return flows;
    }

  }

  private static class LocationsProvider implements ITreeContentProvider {

    @Override
    public Object[] getElements(Object inputElement) {
      IMarker sonarlintMarker = (IMarker) inputElement;
      try {
        if (SonarLintCorePlugin.MARKER_REPORT_ID.equals(sonarlintMarker.getType())) {
          return new Object[] {"Information not available from the Report view"};
        }
      } catch (CoreException e) {
        // Ignore
      }
      if (sonarlintMarker.getAttribute(MarkerUtils.SONAR_MARKER_HAS_EXTRA_LOCATION_KEY_ATTR, false)) {
        ITextEditor openEditor = LocationsUtils.findOpenEditorFor(sonarlintMarker);
        if (openEditor == null) {
          return new Object[] {"Please open the file containing this issue in an editor to see the flows"};
        }
        IDocument document = openEditor.getDocumentProvider().getDocument(openEditor.getEditorInput());
        return new Object[] {new RootNode(sonarlintMarker, positions(document, p -> p.getMarkerId() == sonarlintMarker.getId()))};
      } else {
        return new Object[] {"No additional locations associated with this issue"};
      }
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof RootNode) {
        List<List<ExtraPosition>> flows = ((RootNode) parentElement).getFlows();
        if (flows.size() > 1) {
          AtomicInteger counter = new AtomicInteger(0);
          return flows.stream().map(

            f -> f.size() > 1 ? new FlowRootNode("Flow " + counter.incrementAndGet(), f) : new FlowNode(f.get(0))).toArray();

        } else if (flows.size() == 1) {
          return toFlowNodes(flows.get(0)).toArray();
        } else {
          return new Object[0];
        }
      } else if (parentElement instanceof FlowRootNode) {
        return ((FlowRootNode) parentElement).getChildren();
      } else {
        return new Object[0];
      }
    }

    private static List<ExtraPosition> positions(IDocument document, Predicate<? super ExtraPosition> filter) {
      try {
        return Arrays.asList(document.getPositions(MarkerUtils.SONARLINT_EXTRA_POSITIONS_CATEGORY))
          .stream()
          .map(

            p -> (ExtraPosition) p)
          .filter(filter).collect(Collectors.toList());
      } catch (BadPositionCategoryException e) {
        SonarLintLogger.get().debug("No extra positions found, should maybe trigger a new analysis");
        return Collections.emptyList();
      }
    }

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
    locationsViewer.setInput(sonarlintMarker);
    if (sonarlintMarker != null && showAnnotationsAction.isChecked()) {
      ITextEditor editorFound = LocationsUtils.findOpenEditorFor(sonarlintMarker);
      if (editorFound != null) {
        ShowIssueFlowsMarkerResolver.showAnnotations(sonarlintMarker, editorFound);
      }
    }
  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    IMarker selectedMarker = RuleDescriptionWebView.findSelectedSonarIssue(selection);
    if (selectedMarker != null && !Objects.equals(selectedMarker, locationsViewer.getInput())) {
      setInput(selectedMarker);
    }
  }

  private void startListeningForSelectionChanges() {
    getSite().getPage().addPostSelectionListener(this);
  }

  private void stopListeningForSelectionChanges() {
    getSite().getPage().removePostSelectionListener(this);
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
    startListeningForSelectionChanges();
    SonarLintCorePlugin.getAnalysisListenerManager().addListener(this);
  }

  private void onTreeNodeSelected(Object node) {
    if (node instanceof FlowNode) {
      IMarker sonarlintMarker = (IMarker) locationsViewer.getInput();
      ExtraPosition pos = ((FlowNode) node).getPosition();
      ITextEditor openEditor = LocationsUtils.findOpenEditorFor(sonarlintMarker);
      if (openEditor != null) {
        openEditor.setHighlightRange(pos.offset, pos.length, true);
      }
    }
  }

  private void onTreeNodeDoubleClick(Object node) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try {
      if (node instanceof RootNode) {
        IDE.openEditor(page, ((RootNode) node).getMarker());
      } else if (node instanceof FlowNode) {
        IMarker sonarlintMarker = (IMarker) locationsViewer.getInput();
        ExtraPosition pos = ((FlowNode) node).getPosition();
        IEditorPart editor = IDE.openEditor(page, sonarlintMarker);
        if (editor instanceof ITextEditor) {
          ((ITextEditor) editor).selectAndReveal(pos.offset, pos.length);
        }
      }
    } catch (

    PartInitException e) {
      SonarLintLogger.get().error("Unable to open editor", e);
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
    stopListeningForSelectionChanges();
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
        ShowIssueFlowsMarkerResolver.showAnnotations(sonarlintMarker, editorFound);
      }
    }
  }

  public void setShowAnnotations(boolean b) {
    showAnnotationsAction.setChecked(b);
  }

}
