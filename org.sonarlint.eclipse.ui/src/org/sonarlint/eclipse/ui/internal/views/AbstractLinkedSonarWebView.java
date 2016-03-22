/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;

public abstract class AbstractLinkedSonarWebView<G> extends AbstractSonarWebView implements ISelectionListener {

  protected G currentElement;

  private boolean linking = true;

  private LinkAction toggleLinkAction;

  /**
   * The last selected element if linking was disabled.
   */
  private G lastSelection;

  private final IPartListener2 partListener = new IPartListener2() {
    @Override
    public void partVisible(IWorkbenchPartReference ref) {
      if (ref.getId().equals(getSite().getId())) {
        IWorkbenchPart activePart = ref.getPage().getActivePart();
        if (activePart != null) {
          selectionChanged(activePart, ref.getPage().getSelection());
        }
      }
    }

    @Override
    public void partHidden(IWorkbenchPartReference ref) {
      // Nothing to do
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference ref) {
      if (!ref.getId().equals(getSite().getId())) {
        computeAndSetInput(ref.getPart(false));
      }
    }

    @Override
    public void partActivated(IWorkbenchPartReference ref) {
      // Nothing to do
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference ref) {
      // Nothing to do
    }

    @Override
    public void partClosed(IWorkbenchPartReference ref) {
      // Nothing to do
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference ref) {
      // Nothing to do
    }

    @Override
    public void partOpened(IWorkbenchPartReference ref) {
      // Nothing to do
    }
  };

  @Override
  public void createPartControl(Composite parent) {
    super.createPartControl(parent);
    createActions();
    createToolbar();

    getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
    startListeningForSelectionChanges();
  }

  protected void createActions() {
    toggleLinkAction = new LinkAction();
  }

  private void createToolbar() {
    IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
    toolbarManager.add(toggleLinkAction);
    toolbarManager.add(new Separator());
    toolbarManager.update(false);
  }

  private class LinkAction extends Action {
    private static final String LINK_WITH_SELECTION = "Link with Selection";

    public LinkAction() {
      super(LINK_WITH_SELECTION, SWT.TOGGLE);
      setTitleToolTip(LINK_WITH_SELECTION);
      setImageDescriptor(PlatformUI.getWorkbench()
        .getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
      setChecked(isLinkingEnabled());
    }

    @Override
    public void run() {
      setLinkingEnabled(!isLinkingEnabled());
    }
  }

  /**
   * Sets whether this info view reacts to selection changes in the workbench.
   *
   * @param enabled
   *          if true then the input is set on selection changes
   * @throws CoreException
   */
  protected void setLinkingEnabled(boolean enabled) {
    linking = enabled;
    if (linking && (lastSelection != null)) {
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

  protected void startListeningForSelectionChanges() {
    getSite().getPage().addPostSelectionListener(this);
  }

  protected void stopListeningForSelectionChanges() {
    getSite().getPage().removePostSelectionListener(this);
  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    if (this.equals(part)) {
      return;
    }
    if (!linking) {
      G element = findSelectedElement(part, selection);
      if (element != null) {
        lastSelection = element;
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
    if ((selection == null) || selection.isEmpty()) {
      return;
    }
    final G element = findSelectedElement(part, selection);
    if (isIgnoringNewInput(element)) {
      return;
    }
    if (element == null) {
      return;
    }
    setInput(element);
  }

  public void setInput(G element) {
    currentElement = element;
    open(element);
  }

  protected abstract void open(G element);

  /**
   * Finds and returns the Sonar resource selected in the given part.
   * @throws CoreException
   */
  protected abstract G findSelectedElement(IWorkbenchPart part, ISelection selection);

  protected boolean isIgnoringNewInput(G element) {
    return (currentElement != null) && currentElement.equals(element) && (element != null);
  }

  @Override
  public void dispose() {
    stopListeningForSelectionChanges();
    getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);

    super.dispose();
  }

}
