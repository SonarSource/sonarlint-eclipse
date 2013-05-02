/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.views;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.ViewPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.resources.ISonarResource;
import org.sonar.ide.eclipse.ui.internal.ISonarConstants;
import org.sonar.ide.eclipse.ui.internal.SonarImages;
import org.sonar.ide.eclipse.ui.internal.SonarUrls;
import org.sonar.ide.eclipse.ui.internal.util.SelectionUtils;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;

/**
 * Display details of a project or Sonar resource in a web browser
 * @author Evgeny Mandrikov
 */
public class WebView extends ViewPart implements ISelectionListener {

  private static final Logger LOG = LoggerFactory.getLogger(WebView.class);

  protected ISonarResource currentViewInput;

  private boolean linking = true;

  private LinkAction toggleLinkAction;

  /**
   * The last selected element if linking was disabled.
   */
  private ISonarResource lastSelection;

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.WebView";

  private Browser browser;

  private final IPartListener2 partListener = new IPartListener2() {
    public void partVisible(IWorkbenchPartReference ref) {
      if (ref.getId().equals(getSite().getId())) {
        IWorkbenchPart activePart = ref.getPage().getActivePart();
        if (activePart != null) {
          selectionChanged(activePart, ref.getPage().getSelection());
        }
      }
    }

    public void partHidden(IWorkbenchPartReference ref) {
    }

    public void partInputChanged(IWorkbenchPartReference ref) {
      if (!ref.getId().equals(getSite().getId())) {
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

  @Override
  public final void createPartControl(Composite parent) {
    browser = new Browser(parent, SWT.NONE);
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
    public LinkAction() {
      super("Link with Selection", SWT.TOGGLE);
      setTitleToolTip("Link with Selection");
      setImageDescriptor(SonarImages.SONARSYNCHRO_IMG);
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

  @Override
  public final void setFocus() {
    browser.setFocus();
  }

  protected void startListeningForSelectionChanges() {
    getSite().getPage().addPostSelectionListener(this);
  }

  protected void stopListeningForSelectionChanges() {
    getSite().getPage().removePostSelectionListener(this);
  }

  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    if (this.equals(part)) {
      return;
    }
    if (!linking) {
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
    if ((selection == null) || selection.isEmpty()) {
      return;
    }
    final ISonarResource input = findSelectedSonarResource(part, selection);
    if (isIgnoringNewInput(input)) {
      return;
    }
    if (input == null) {
      return;
    }
    setInput(input);
  }

  private void setInput(ISonarResource sonarResource) {
    currentViewInput = sonarResource;
    SonarProject sonarProject = SonarProject.getInstance(sonarResource.getProject());
    String url = new SonarUrls().resourceUrl(sonarResource);
    ISonarServer sonarServer = SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
    if (sonarServer == null) {
      browser.setText(NLS.bind(Messages.No_matching_server_in_configuration_for_project, sonarProject.getProject().getName(), url));
      return;
    }

    if (!WSClientFactory.getSonarClient(sonarServer).exists(sonarResource.getKey())) {
      browser.setText("Not found.");
      return;
    }
    LOG.debug("Opening url {} in web view", url);

    if (sonarServer.getUsername() != null) {
      String userpwd = sonarServer.getUsername() + ":" + sonarServer.getPassword();
      byte[] encodedBytes = Base64.encodeBase64(userpwd.getBytes());
      browser.setUrl(url, null, new String[] {"Authorization: Basic " + new String(encodedBytes)});
    }
    else {
      browser.setUrl(url);
    }
  }

  /**
   * Finds and returns the Sonar resource selected in the given part.
   */
  private ISonarResource findSelectedSonarResource(IWorkbenchPart part, ISelection selection) {
    if (part instanceof EditorPart) {
      EditorPart editor = (EditorPart) part;
      IEditorInput editorInput = editor.getEditorInput();
      IResource resource = ResourceUtil.getResource(editorInput);
      return ResourceUtils.adapt(resource);
    } else if (selection instanceof IStructuredSelection) {
      return ResourceUtils.adapt(SelectionUtils.getSingleElement(selection));
    }
    return null;
  }

  /**
   * @return input input of this view or <code>null</code> if no input is set
   */
  protected ISonarResource getInput() {
    return currentViewInput;
  }

  protected boolean isIgnoringNewInput(ISonarResource sonarResource) {
    return (currentViewInput != null) && currentViewInput.equals(sonarResource) && (sonarResource != null);
  }

  @Override
  public void dispose() {
    stopListeningForSelectionChanges();
    getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);

    super.dispose();
  }

}
