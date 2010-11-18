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

package org.sonar.ide.eclipse.internal.ui.views;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.ViewPart;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.internal.ui.SonarImages;
import org.sonar.ide.eclipse.ui.util.PlatformUtils;
import org.sonar.ide.eclipse.ui.util.SelectionUtils;

/**
 * Abstract class for views which show information for a given element. Inspired by org.eclipse.jdt.internal.ui.infoviews.AbstractInfoView
 * 
 * @since 0.3
 */
public abstract class AbstractSonarInfoView extends ViewPart implements ISelectionListener {

  protected ISonarResource currentViewInput;

  private boolean linking = true;

  private LinkAction toggleLinkAction;

  /**
   * The last selected element if linking was disabled.
   */
  private ISonarResource lastSelection;

  private IPartListener2 partListener = new IPartListener2() {
    public void partVisible(IWorkbenchPartReference ref) {
      if (ref.getId().equals(getSite().getId())) {
        IWorkbenchPart activePart = ref.getPage().getActivePart();
        if (activePart != null) {
          selectionChanged(activePart, ref.getPage().getSelection());
        }
        startListeningForSelectionChanges();
      }
    }

    public void partHidden(IWorkbenchPartReference ref) {
      if (ref.getId().equals(getSite().getId())) {
        stopListeningForSelectionChanges();
      }
    }

    public void partInputChanged(IWorkbenchPartReference ref) {
      if ( !ref.getId().equals(getSite().getId())) {
        computeAndSetInput(ref.getPart(false));
      }
    }

    public void partActivated(IWorkbenchPartReference ref) {
    }

    public void partBroughtToTop(IWorkbenchPartReference ref) {
    }

    public void partClosed(IWorkbenchPartReference ref) {
    }

    public void partDeactivated(IWorkbenchPartReference ref) {
    }

    public void partOpened(IWorkbenchPartReference ref) {
    }
  };

  /**
   * Create the part control.
   * 
   * @param parent
   *          the parent control
   * @see IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
   */
  abstract protected void internalCreatePartControl(Composite parent);

  /**
   * @return the view's primary control.
   */
  abstract protected Control getControl();

  /**
   * Set the input of this view.
   */
  abstract protected void doSetInput(Object input);

  @Override
  public final void createPartControl(Composite parent) {
    internalCreatePartControl(parent);
    getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
    createActions();
    createToolbar();
  }

  protected void createActions() {
    toggleLinkAction = new LinkAction();
  }

  private void createToolbar() {
    // TODO Godin: review how we create actions
    IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
    toolbarManager.add(toggleLinkAction);
    toolbarManager.add(new Separator());
    toolbarManager.update(false);
  }

  private class LinkAction extends Action {
    public LinkAction() {
      super("Link with Selection", SWT.TOGGLE);
      setTitleToolTip("Link with Selection");
      setImageDescriptor(SonarImages.SONARSYNCHRO_IMG);
      setChecked(isLinkingEnabled());
    }

    @Override
    public void run() {
      setLinkingEnabled( !isLinkingEnabled());
    }
  }

  /**
   * Sets whether this info view reacts to selection changes in the workbench.
   * 
   * @param enabled
   *          if true then the input is set on selection changes
   */
  protected void setLinkingEnabled(boolean enabled) {
    linking = enabled;
    if (linking && lastSelection != null) {
      setInput(lastSelection);
    }
  }

  /**
   * Returns whether this info view reacts to selection changes in the workbench.
   * 
   * @return true if linking with selection is enabled
   */
  protected boolean isLinkingEnabled() {
    return linking;
  }

  @Override
  public final void setFocus() {
    getControl().setFocus();
  }

  /**
   * Start to listen for selection changes.
   */
  protected void startListeningForSelectionChanges() {
    getSite().getPage().addPostSelectionListener(this);
  }

  /**
   * Stop to listen for selection changes.
   */
  protected void stopListeningForSelectionChanges() {
    getSite().getPage().removePostSelectionListener(this);
  }

  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    if (part.equals(this)) {
      return;
    }
    if ( !linking) {
      ISonarResource sonarResource = findSelectedSonarResource(part, selection);
      if (sonarResource != null) {
        lastSelection = sonarResource;
      }
    } else {
      lastSelection = null;
      computeAndSetInput(part);
    }
  }

  private void computeAndSetInput(final IWorkbenchPart part) {
    ISelectionProvider provider = part.getSite().getSelectionProvider();
    if (provider == null) {
      return;
    }
    final ISelection selection = provider.getSelection();
    if (selection == null || selection.isEmpty()) {
      return;
    }
    final ISonarResource input = findSelectedSonarResource(part, selection);
    if (isIgnoringNewInput(input, part, selection)) {
      return;
    }
    if (input == null) {
      return;
    }
    setInput(input);
  }

  private void setInput(ISonarResource input) {
    currentViewInput = input;
    doSetInput(input);
  }

  /**
   * Finds and returns the Sonar resource selected in the given part.
   */
  private ISonarResource findSelectedSonarResource(IWorkbenchPart part, ISelection selection) {
    if (part instanceof EditorPart) {
      EditorPart editor = (EditorPart) part;
      IEditorInput editorInput = editor.getEditorInput();
      IResource resource = ResourceUtil.getResource(editorInput);
      return findSonarResource(resource);
    } else if (selection instanceof IStructuredSelection) {
      return findSonarResource(SelectionUtils.getSingleElement(selection));
    }
    return null;
  }

  /**
   * Tries to get a Sonar resource out of the given element.
   */
  protected ISonarResource findSonarResource(Object element) {
    if (element == null) {
      return null;
    }
    if (element instanceof IAdaptable) {
      // return (ISonarResource) ((IAdaptable) element).getAdapter(ISonarResource.class);
      return PlatformUtils.adapt(element, ISonarResource.class);
    }
    return null;
  }

  /**
   * @return input input of this view or <code>null</code> if no input is set
   */
  protected ISonarResource getInput() {
    return currentViewInput;
  }

  protected boolean isIgnoringNewInput(ISonarResource sonarResource, IWorkbenchPart part, ISelection selection) {
    return currentViewInput != null && currentViewInput.equals(sonarResource) && sonarResource != null;
  }
}
