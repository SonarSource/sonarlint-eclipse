/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
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

/**
 * Display details of a rule in a web browser
 */
public class IssueLocationsView extends ViewPart implements ISelectionListener, AnalysisListener {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.IssueLocationsView";

  private LocationsViewer locationsViewer;
  private IFile currentFile;
  private ITextEditor currentEditor;
  private IDocument currentDoc;

  private static class FlowRootNode {

    private final String label;
    private final List<ExtraPosition> positions;

    public FlowRootNode(String label, List<ExtraPosition> positions) {
      this.label = label;
      this.positions = positions;
    }

    public String getLabel() {
      return label;
    }

    public List<ExtraPosition> getPositions() {
      return positions;
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
          current = positions.stream().filter(p -> p.getParent() == currentValue).findFirst();
        }
        result.add(flow);
      }
      return result;
    }

    public IMarker getRoot() {
      return rootMarker;
    }

    public List<List<ExtraPosition>> getFlows() {
      return flows;
    }

  }
  private class LocationsProvider implements ITreeContentProvider {

    @Override
    public Object[] getElements(Object inputElement) {
      IMarker sonarlintMarker = (IMarker) inputElement;
      if (sonarlintMarker.getAttribute(MarkerUtils.SONAR_MARKER_HAS_EXTRA_LOCATION_KEY_ATTR, false)) {
        if (currentDoc == null) {
          return new Object[] {"Please open the file containing this issue in an editor to see the flows"};
        }
        return new Object[] {new RootNode(sonarlintMarker, positions(p -> p.getMarkerId() == sonarlintMarker.getId()))};
      } else {
        return new Object[] {"No aditional locations associated with this issue"};
      }
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof RootNode) {
        List<List<ExtraPosition>> flows = ((RootNode) parentElement).getFlows();
        if (flows.size() > 1) {
          AtomicInteger counter = new AtomicInteger(0);
          return flows.stream().map(f -> f.size() > 1 ? new FlowRootNode("Flow " + counter.incrementAndGet(), f) : f.get(0)).toArray();
        } else if (flows.size() == 1) {
          return flows.get(0).toArray();
        } else {
          return new Object[0];
        }
      } else if (parentElement instanceof FlowRootNode) {
        return ((FlowRootNode) parentElement).getPositions().toArray();
      } else {
        return new Object[0];
      }
    }

    private List<ExtraPosition> positions(Predicate<? super ExtraPosition> filter) {
      try {
        return Arrays.asList(currentDoc.getPositions(MarkerUtils.SONARLINT_EXTRA_POSITIONS_CATEGORY))
          .stream()
          .map(p -> (ExtraPosition) p)
          .filter(filter)
          .collect(Collectors.toList());
      } catch (BadPositionCategoryException e) {
        SonarLintLogger.get().error("Unable to read positions", e);
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

  private static class LocationsViewer extends TreeViewer {

    public LocationsViewer(Tree tree) {
      super(tree);
      setAutoExpandLevel(ALL_LEVELS);
      setUseHashlookup(true);
    }
  }

  private static class LocationsTreeLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
      if (element instanceof RootNode) {
        return ((RootNode) element).getRoot().getAttribute(IMarker.MESSAGE, "No message");
      } else if (element instanceof FlowRootNode) {
        return ((FlowRootNode) element).getLabel();
      } else if (element instanceof String) {
        return (String) element;
      }
      return ((ExtraPosition) element).getMessage();
    }

    @Override
    public Image getImage(Object element) {
      if (element instanceof RootNode) {
        return SonarLintImages.IMG_ISSUE;
      }
      return super.getImage(element);
    }

  }

  public void setInput(@Nullable IMarker sonarlintMarker) {
    if (sonarlintMarker == null) {
      clearInput();
      return;
    }
    // Find IFile and open Editor
    // Super defensing programming because we don't really understand what is initialized at startup (SLE-122)
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      showMarkerWithNoEditor(sonarlintMarker);
      return;
    }
    IWorkbenchPage page = window.getActivePage();
    if (page == null) {
      showMarkerWithNoEditor(sonarlintMarker);
      return;
    }
    boolean editorFound = findEditor(sonarlintMarker, page);
    if (!editorFound) {
      showMarkerWithNoEditor(sonarlintMarker);
    }
  }

  private boolean findEditor(IMarker sonarlintMarker, IWorkbenchPage page) {
    for (IEditorReference editor : page.getEditorReferences()) {
      IEditorInput editorInput;
      try {
        editorInput = editor.getEditorInput();
      } catch (PartInitException e) {
        SonarLintLogger.get().error("Unable to restore editor", e);
        continue;
      }
      // Cast needed for older Eclipse versions
      IFile file = (IFile) editorInput.getAdapter(IFile.class);
      if (file != null && sonarlintMarker.getResource().equals(file)) {
        IEditorPart editorPart = editor.getEditor(false);
        if (editorPart instanceof ITextEditor) {
          this.currentEditor = (ITextEditor) editorPart;
          this.currentDoc = currentEditor.getDocumentProvider().getDocument(editorPart.getEditorInput());
          this.currentFile = file;
          locationsViewer.setInput(sonarlintMarker);
          ShowIssueFlowsMarkerResolver.toggleAnnotations((IMarker) locationsViewer.getInput(), currentEditor, true);
          return true;
        }
      }
    }
    return false;
  }

  private void showMarkerWithNoEditor(IMarker sonarlintMarker) {
    clearCurrentEditor();
    locationsViewer.setInput(sonarlintMarker);
  }

  private void clearInput() {
    clearCurrentEditor();
    locationsViewer.setInput(null);
  }

  private void clearCurrentEditor() {
    this.currentEditor = null;
    this.currentDoc = null;
    this.currentFile = null;
  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    IMarker selectedMarker = AbstractSonarWebView.findSelectedSonarIssue(selection);
    if (selectedMarker != null && selectedMarker != locationsViewer.getInput()) {
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
    locationsViewer = new LocationsViewer(tree);
    locationsViewer.setContentProvider(new LocationsProvider());
    locationsViewer.setLabelProvider(new LocationsTreeLabelProvider());
    locationsViewer.addSelectionChangedListener(event -> {
      ISelection selection = event.getSelection();
      if (selection instanceof IStructuredSelection) {
        Object firstElement = ((IStructuredSelection) selection).getFirstElement();
        if (firstElement == null) {
          return;
        }
        onTreeNodeSelected(firstElement);
      }
    });
    startListeningForSelectionChanges();
    SonarLintCorePlugin.getAnalysisListenerManager().addListener(this);
  }

  private void onTreeNodeSelected(Object node) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try {
      if (node instanceof RootNode) {
        IDE.openEditor(page, ((RootNode) node).getRoot());
      } else if (node instanceof ExtraPosition) {
        IEditorPart editor = IDE.openEditor(page, currentFile);
        if (editor instanceof ITextEditor) {
          ExtraPosition pos = (ExtraPosition) node;
          ((ITextEditor) editor).selectAndReveal(pos.offset, pos.length);
        }
      }
    } catch (PartInitException e) {
      SonarLintLogger.get().error("Unable to open editor", e);
    }
  }

  private void createToolbar() {
    IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
    toolbarManager.add(new ClearLocationsAnnotationsAction());
    toolbarManager.add(new Separator());
    toolbarManager.update(false);
  }

  private class ClearLocationsAnnotationsAction extends Action {
    private static final String MSG = "Clear locations annotations";

    public ClearLocationsAnnotationsAction() {
      super(MSG, IAction.AS_PUSH_BUTTON);
      setTitleToolTip(MSG);
      setImageDescriptor(PlatformUI.getWorkbench()
        .getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVEALL));
    }

    @Override
    public void run() {
      ShowIssueFlowsMarkerResolver.removeAllAnnotations();
    }
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
  public void analysisCompleted(AnalysisEvent event) {
    IMarker marker = (IMarker) locationsViewer.getInput();
    Display.getDefault().asyncExec(() -> {
      if (marker != null && marker.exists()) {
        setInput(marker);
      } else {
        clearInput();
      }
    });
  }

}
