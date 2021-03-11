/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.net.URL;
import java.util.Map;
import java.util.Optional;
import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public class OpenInBrowserCommand extends AbstractIssueCommand implements IElementUpdater {

  @Override
  public void updateElement(UIElement element, Map parameters) {
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window != null) {
      IStructuredSelection selection = (IStructuredSelection) window.getSelectionService().getSelection();
      Optional<ResolvedBinding> binding = getBinding(getSelectedMarker(selection));
      if (binding.isPresent()) {
        element.setIcon(binding.get().getEngineFacade().isSonarCloud() ? SonarLintImages.SONARCLOUD_16 : SonarLintImages.SONARQUBE_16);
      }
    }
  }

  @Override
  protected void execute(IMarker selectedMarker) {
    try {
      Optional<ResolvedBinding> binding = getBinding(selectedMarker);
      if (!binding.isPresent()) {
        SonarLintLogger.get().info("Unable to open issue in browser: project is not bound");
        return;
      }
      SonarLintCorePlugin.getTelemetry().taintVulnerabilitiesInvestigatedRemotely();
      String issueKey = (String) selectedMarker.getAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR);
      String serverIssueLink = buildLink(binding.get().getEngineFacade().getHost(), binding.get().getProjectBinding().projectKey(), issueKey);
      PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(serverIssueLink));
    } catch (Exception e) {
      SonarLintLogger.get().error("Unable to open issue in browser", e);
    }
  }

  private static Optional<ResolvedBinding> getBinding(IMarker marker) {
    ISonarLintProject project = Adapters.adapt(marker.getResource().getProject(), ISonarLintProject.class);
    return SonarLintCorePlugin.getServersManager().resolveBinding(project);
  }

  private static String buildLink(String serverUrl, String projectKey, String issueKey) {
    String urlEncodedProjectKey = StringUtils.urlEncode(projectKey);
    String urlEncodedIssueKey = StringUtils.urlEncode(issueKey);
    return serverUrl + "/project/issues?id=" + urlEncodedProjectKey + "&open=" + urlEncodedIssueKey;
  }

}
