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
package org.sonarlint.eclipse.ui.internal.views;

import java.util.Optional;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.flowlocations.SonarLintMarkerSelectionListener;
import org.sonarlint.eclipse.ui.internal.util.SonarLintRuleBrowser;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.exceptions.SonarLintException;

/**
 * Display details of a rule in a web browser
 */
public class RuleDescriptionWebView extends ViewPart implements SonarLintMarkerSelectionListener {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.RuleDescriptionWebView";

  private boolean linking = true;

  private SonarLintRuleBrowser browser;

  @Override
  public void createPartControl(Composite parent) {
    createToolbar();
    browser = new SonarLintRuleBrowser(parent, true);

    SonarLintUiPlugin.getSonarlintMarkerSelectionService().addMarkerSelectionListener(this);
  }

  @Override
  public final void setFocus() {
    browser.setFocus();
  }

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
    if (linking) {
      setInput(SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedMarker().orElse(null));
    }
  }

  @Override
  public void markerSelected(Optional<IMarker> marker) {
    if (linking) {
      setInput(marker.orElse(null));
    }
  }

  public void setInput(@Nullable IMarker marker) {
    if (marker != null) {
      showRuleDescription(marker);
    } else {
      clear();
    }
  }

  private void clear() {
    browser.updateRule(null);
  }

  @Override
  public void dispose() {
    SonarLintUiPlugin.getSonarlintMarkerSelectionService().removeMarkerSelectionListener(this);
    super.dispose();
  }

  private void showRuleDescription(IMarker element) {
    String ruleKey;
    try {
      ruleKey = element.getAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR).toString();
    } catch (CoreException e) {
      SonarLintLogger.get().error("Unable to open rule description", e);
      return;
    }

    ISonarLintIssuable issuable = Adapters.adapt(element.getResource(), ISonarLintIssuable.class);
    ISonarLintProject p = issuable.getProject();

    Optional<ResolvedBinding> resolveBindingOpt = SonarLintCorePlugin.getServersManager().resolveBinding(p);
    RuleDetails ruleDetails;
    if (resolveBindingOpt.isPresent()) {
      ResolvedBinding resolvedBinding = resolveBindingOpt.get();
      try {
        ruleDetails = resolvedBinding.getEngineFacade().getRuleDescription(ruleKey, resolvedBinding.getProjectBinding().projectKey());
        browser.updateRule(ruleDetails);
      } catch (SonarLintException e) {
        SonarLintLogger.get().error("Unable to display rule descrioption", e);
      }
    } else {
      ruleDetails = SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade().getRuleDescription(ruleKey);
      browser.updateRule(ruleDetails);
    }

  }

}
