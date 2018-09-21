/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.browser.Browser;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;

/**
 * Display details of a rule in a web browser
 */
public class RuleDescriptionWebView extends AbstractLinkedSonarWebView<IMarker> implements ISelectionListener {

  public static final String ID = SonarLintUiPlugin.PLUGIN_ID + ".views.RuleDescriptionWebView";

  @Override
  protected void open(IMarker element) {
    Browser browser = getBrowser();
    if (browser == null) {
      return;
    }

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

    if (ruleDetails == null) {
      browser.setText("No rule description available");
      return;
    }

    new RuleDescriptionPart(browser).updateView(ruleDetails);
  }

  @Override
  protected IMarker findSelectedElement(IWorkbenchPart part, ISelection selection) {
    return findSelectedSonarIssue(selection);
  }

}
