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
package org.sonarlint.eclipse.ui.internal.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.util.SonarLintRuleBrowser;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;

/**
 * Display details of a rule in a web browser
 */
public class RuleDescriptionWebView extends ViewPart implements ISelectionListener {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.RuleDescriptionWebView";

  protected IMarker currentElement;

  private boolean linking = true;

  /**
   * The last selected element if linking was disabled.
   */
  private IMarker lastSelection;

  private SonarLintRuleBrowser browser;

  @Override
  public void createPartControl(Composite parent) {
    createToolbar();
    browser = new SonarLintRuleBrowser(parent);

    getSite().getWorkbenchWindow().getPartService().addPartListener(partListener);
    startListeningForSelectionChanges();
  }

  @Override
  public final void setFocus() {
    browser.setFocus();
  }

  @CheckForNull
  public static IMarker findSelectedSonarIssue(ISelection selection) {
    try {
      if (selection instanceof IStructuredSelection) {
        List<IMarker> selectedSonarMarkers = new ArrayList<>();

        @SuppressWarnings("rawtypes")
        List elems = ((IStructuredSelection) selection).toList();
        for (Object elem : elems) {
          processElement(selectedSonarMarkers, elem);
        }

        if (!selectedSonarMarkers.isEmpty()) {
          return selectedSonarMarkers.get(0);
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  private static void processElement(List<IMarker> selectedSonarMarkers, Object elem) throws CoreException {
    IMarker marker = Adapters.adapt(elem, IMarker.class);
    if (marker != null && isSonarLintMarker(marker)) {
      selectedSonarMarkers.add(marker);
    }
  }

  private static boolean isSonarLintMarker(IMarker marker) throws CoreException {
    return SonarLintCorePlugin.MARKER_ON_THE_FLY_ID.equals(marker.getType()) || SonarLintCorePlugin.MARKER_REPORT_ID.equals(marker.getType());
  }

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

  private void createToolbar() {
    IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
    toolbarManager.add(new LinkAction());
    toolbarManager.add(new Separator());
    toolbarManager.update(false);
  }

  private class LinkAction extends Action {
    private static final String LINK_WITH_SELECTION = "Link with Selection";

    public LinkAction() {
      super(LINK_WITH_SELECTION, IAction.AS_CHECK_BOX);
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
      IMarker element = findSelectedSonarIssue(selection);
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
    final IMarker element = findSelectedSonarIssue(selection);
    if (isIgnoringNewInput(element)) {
      return;
    }
    if (element == null) {
      return;
    }
    setInput(element);
  }

  public void setInput(IMarker element) {
    currentElement = element;
    open(element);
  }

  protected boolean isIgnoringNewInput(@Nullable IMarker element) {
    return (currentElement != null) && currentElement.equals(element) && (element != null);
  }

  @Override
  public void dispose() {
    stopListeningForSelectionChanges();
    getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);

    super.dispose();
  }

  private void open(IMarker element) {

    String ruleKey;
    try {
      ruleKey = element.getAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR).toString();
    } catch (CoreException e) {
      SonarLintLogger.get().error("Unable to open rule description", e);
      return;
    }

    ISonarLintIssuable issuable = Adapters.adapt(element.getResource(), ISonarLintIssuable.class);
    ISonarLintProject p = issuable.getProject();
    RuleDetails ruleDetails;
    Optional<IServer> server = SonarLintCorePlugin.getServersManager().forProject(p);
    if (server.isPresent()) {
      ruleDetails = server.get().getRuleDescription(ruleKey);
    } else {
      ruleDetails = SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade().getRuleDescription(ruleKey);
    }

    browser.updateRule(ruleDetails);
  }

}
