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
package org.sonarlint.eclipse.core.internal.tracking;

import java.util.Collection;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;

public class MarkerUpdater implements TrackingChangeListener {

  @Override
  public void onTrackingChange(String moduleKey, String relativePath, Collection<? extends Trackable> issues) {
    // TODO find the absolute path from moduleKey and file (relative path)
    String absolutePath = "/home/janosgyerik/dev/git/sonar/sonar-scanner-cli/" + relativePath;

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IPath location = Path.fromOSString(absolutePath);
    IFile file = workspace.getRoot().getFileForLocation(location);

    try {
      file.deleteMarkers(SonarLintCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    } catch (CoreException e) {
      SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
      return;
    }

    try {
      for (Trackable issue : issues) {
        if (!issue.isResolved()) {
          createMarker(file, issue);
        }
      }
    } catch (CoreException e) {
      SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
    }
  }

  private void createMarker(IFile file, Trackable issue) throws CoreException {
    IMarker marker = file.createMarker(SonarLintCorePlugin.MARKER_ID);

    // TODO
    // marker.setAttribute(IMarker.PRIORITY, getPriority(issue.getSeverity()));
    marker.setAttribute(IMarker.SEVERITY, PreferencesUtils.getMarkerSeverity());

    // File level issues (line == null) are displayed on line 1
    marker.setAttribute(IMarker.LINE_NUMBER, issue.getLine() != null ? issue.getLine() : 1);
    marker.setAttribute(IMarker.MESSAGE, issue.getMessage());
    marker.setAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, issue.getServerIssueKey());
    marker.setAttribute(MarkerUtils.SONAR_MARKER_ASSIGNEE_ATTR, issue.getAssignee());

    if (issue.getCreationDate() != null) {
      marker.setAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, String.valueOf(issue.getCreationDate().longValue()));
    }
  }

}
