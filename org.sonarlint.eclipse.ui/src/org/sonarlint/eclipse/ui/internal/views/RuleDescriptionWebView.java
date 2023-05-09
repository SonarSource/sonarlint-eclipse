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
package org.sonarlint.eclipse.ui.internal.views;

import java.util.Objects;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.job.DisplayProjectRuleDescriptionJob;
import org.sonarlint.eclipse.ui.internal.rule.RuleDetailsPanel;
import org.sonarlint.eclipse.ui.internal.util.SelectionUtils;

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

  private RuleDetailsPanel ruleDetailsPanel;

  @Override
  public void createPartControl(Composite parent) {
    createToolbar();
    ruleDetailsPanel = new RuleDetailsPanel(parent, true);

    startListeningForSelectionChanges();
  }

  @Override
  public final void setFocus() {
    ruleDetailsPanel.setFocus();
  }

  private void createToolbar() {
    var toolbarManager = getViewSite().getActionBars().getToolBarManager();
    toolbarManager.add(new LinkAction());
    toolbarManager.add(new Separator());
    toolbarManager.update(false);
  }

  private class LinkAction extends Action {
    private static final String LINK_WITH_SELECTION = "Link with Selection";

    public LinkAction() {
      super(LINK_WITH_SELECTION, IAction.AS_CHECK_BOX);
      setTitleToolTip(LINK_WITH_SELECTION);
      setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));
      setChecked(linking);
    }

    @Override
    public void run() {
      setLinkingEnabled(!linking);
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

  protected void startListeningForSelectionChanges() {
    getSite().getPage().addPostSelectionListener(this);
  }

  protected void stopListeningForSelectionChanges() {
    getSite().getPage().removePostSelectionListener(this);
  }

  public void setInput(@Nullable IMarker marker) {
    currentElement = marker;
    if (marker != null) {
      showRuleDescription(marker);
    } else {
      ruleDetailsPanel.clearRule();
    }
  }

  @Override
  public void dispose() {
    stopListeningForSelectionChanges();

    super.dispose();
  }

  private void showRuleDescription(IMarker element) {
    var ruleKey = element.getAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, null);
    if (ruleKey == null) {
      SonarLintLogger.get().error("No rule key on marker");
      return;
    }

    // Update project rule description asynchronous
    new DisplayProjectRuleDescriptionJob(Adapters.adapt(
      element.getResource(), ISonarLintIssuable.class).getProject(),
      ruleKey,
      element.getAttribute(MarkerUtils.SONAR_MARKER_RULE_DESC_CONTEXT_KEY_ATTR, null),
      ruleDetailsPanel)
        .schedule();
  }

  @Override
  public void selectionChanged(IWorkbenchPart part, ISelection selection) {
    var selectedMarker = SelectionUtils.findSelectedSonarLintMarker(selection);
    if (selectedMarker != null) {
      lastSelection = selectedMarker;
      if (linking && !Objects.equals(selectedMarker, currentElement)) {
        setInput(selectedMarker);
      }
    }
  }

}
