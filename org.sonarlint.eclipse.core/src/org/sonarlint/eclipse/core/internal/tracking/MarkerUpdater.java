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
        createMarker(file, issue);
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
    marker.setAttribute(MarkerUtils.SONAR_MARKER_RESOLVED_ATTR, issue.isResolved());
    marker.setAttribute(MarkerUtils.SONAR_MARKER_ASSIGNEE_ATTR, issue.getAssignee());

    if (issue.getCreationDate() != null) {
      marker.setAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, String.valueOf(issue.getCreationDate().longValue()));
    }
  }

}
