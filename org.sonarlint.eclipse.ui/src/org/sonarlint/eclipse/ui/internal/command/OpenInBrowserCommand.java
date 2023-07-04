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
package org.sonarlint.eclipse.ui.internal.command;

import java.util.Map;
import java.util.Optional;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;

public class OpenInBrowserCommand extends AbstractIssueCommand implements IElementUpdater {

  @Override
  public void updateElement(UIElement element, Map parameters) {
    var window = element.getServiceLocator().getService(IWorkbenchWindow.class);
    if (window == null) {
      return;
    }
    var selection = (IStructuredSelection) window.getSelectionService().getSelection();
    var binding = getBinding(getSelectedMarker(selection));
    if (binding.isPresent()) {
      element.setIcon(binding.get().getEngineFacade().isSonarCloud() ? SonarLintImages.SONARCLOUD_16 : SonarLintImages.SONARQUBE_16);
    }
  }

  @Override
  protected void execute(IMarker selectedMarker, IWorkbenchWindow window) {
    try {
      var binding = getBinding(selectedMarker);
      if (binding.isEmpty()) {
        SonarLintLogger.get().info("Unable to open issue in browser: project is not bound");
        return;
      }
      SonarLintCorePlugin.getTelemetry().taintVulnerabilitiesInvestigatedRemotely();
      var issueKey = (String) selectedMarker.getAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR);
      var serverIssueLink = buildLink(binding.get().getEngineFacade().getHost(), binding.get().getProjectBinding().projectKey(), issueKey);
      BrowserUtils.openExternalBrowser(serverIssueLink, window.getShell().getDisplay());
    } catch (Exception e) {
      SonarLintLogger.get().error("Unable to open issue in browser", e);
    }
  }

  private static Optional<ResolvedBinding> getBinding(IMarker marker) {
    var project = Adapters.adapt(marker.getResource().getProject(), ISonarLintProject.class);
    return SonarLintCorePlugin.getServersManager().resolveBinding(project);
  }

  private static String buildLink(String serverUrl, String projectKey, String issueKey) {
    var urlEncodedProjectKey = StringUtils.urlEncode(projectKey);
    var urlEncodedIssueKey = StringUtils.urlEncode(issueKey);
    return serverUrl + "/project/issues?id=" + urlEncodedProjectKey + "&open=" + urlEncodedIssueKey;
  }

}
