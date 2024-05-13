/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

/** Shared logic for all commands on (to be) resolved issues */
public abstract class AbstractResolvedCommand extends AbstractIssueCommand implements IElementUpdater {
  protected static String TITLE;

  protected IWorkbenchWindow currentWindow;

  /** To be implemented by the actual commands */
  protected abstract void execute(IMarker marker, ISonarLintFile file, ISonarLintProject project, String issueKey,
    boolean isTaint);

  @Override
  protected void execute(IMarker selectedMarker, IWorkbenchWindow window) {
    currentWindow = window;

    var slFile = tryGetISonarLintFile(selectedMarker, TITLE,
      "Cannot adapt marker resource to ISonarLintFile, please see the logs for more information");
    if (slFile == null) {
      return;
    }
    var project = slFile.getProject();

    var issueKey = tryGetIssueKey(selectedMarker, TITLE, "No issue key found on marker");
    if (issueKey == null) {
      return;
    }

    var markerType = tryGetMarkerType(selectedMarker, TITLE);
    if (markerType == null) {
      return;
    }

    // run actual command
    execute(selectedMarker, slFile, project, issueKey, markerType.equals(SonarLintCorePlugin.MARKER_TAINT_ID));
  }

  @Override
  public void updateElement(UIElement element, Map parameters) {
    var window = element.getServiceLocator().getService(IWorkbenchWindow.class);
    if (window == null) {
      return;
    }

    /** When opening the context menu on no selected issue marker, this command should not be shown */
    var marker = getSelectedMarker((IStructuredSelection) window.getSelectionService().getSelection());
    if (marker != null) {
      var binding = getBinding(marker);
      if (binding.isPresent()) {
        element.setIcon(binding.get().getConnectionFacade().isSonarCloud()
          ? SonarLintImages.SONARCLOUD_16
          : SonarLintImages.SONARQUBE_16);
      }
    }
  }

  /** Try to get the ISonarLint file from the marker */
  @Nullable
  protected ISonarLintFile tryGetISonarLintFile(IMarker marker, String errorTitle, String errorMessage) {
    var slFile = SonarLintUtils.adapt(marker.getResource(), ISonarLintFile.class,
      "[AbstractResolvedCommand#tryGetISonarLintFile] Try get file of marker '" + marker.toString() + "'");
    if (slFile == null) {
      currentWindow.getShell().getDisplay()
        .asyncExec(() -> MessageDialog.openError(currentWindow.getShell(), errorTitle, errorMessage));
    }

    return slFile;
  }

  /** Try to get the issue key (e.g. server issue key / UUID) */
  @Nullable
  protected String tryGetIssueKey(IMarker marker, String errorTitle, String errorMessage) {
    var serverIssue = marker.getAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, null);
    if (serverIssue == null) {
      serverIssue = marker.getAttribute(MarkerUtils.SONAR_MARKER_TRACKED_ISSUE_ID_ATTR, null);
    }
    if (serverIssue == null) {
      currentWindow.getShell().getDisplay()
        .asyncExec(() -> MessageDialog.openError(currentWindow.getShell(), errorTitle, errorMessage));
    }

    return serverIssue;
  }

  /** Try to get the marker type (normal issue or a taint, different behavior) */
  @Nullable
  protected String tryGetMarkerType(IMarker marker, String errorTitle) {
    String markerType = null;
    try {
      markerType = marker.getType();
    } catch (CoreException err) {
      currentWindow.getShell().getDisplay()
        .asyncExec(() -> MessageDialog.openError(currentWindow.getShell(), errorTitle, err.getMessage()));
    }
    return markerType;
  }
}
