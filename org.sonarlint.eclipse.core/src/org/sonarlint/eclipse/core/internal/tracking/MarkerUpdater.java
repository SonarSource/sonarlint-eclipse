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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.markers.FlatTextRange;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.TextFileContext;

public class MarkerUpdater implements TrackingChangeListener {

  private final ModulePathManager modulePathManager;

  public MarkerUpdater(ModulePathManager modulePathManager) {
    this.modulePathManager = modulePathManager;
  }

  @Override
  public void onTrackingChange(String localModuleKey, String relativePath, Collection<Trackable> issues) {
    String absolutePath = modulePathManager.getFilePath(localModuleKey, relativePath);

    IWorkspace workspace = ResourcesPlugin.getWorkspace();
    IPath location = Path.fromOSString(absolutePath);
    IFile file = workspace.getRoot().getFileForLocation(location);

    try {
      file.deleteMarkers(SonarLintCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);
    } catch (CoreException e) {
      SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
      return;
    }

    ITextFileBufferManager textFileBufferManager = FileBuffers.getTextFileBufferManager();
    if (textFileBufferManager == null) {
      return;
    }

    try (TextFileContext context = new TextFileContext(file)) {
      IDocument document = context.getDocument();
      if (document != null) {
        createMarkers(document, file, issues);
      }
    } catch (CoreException e) {
      SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
    }
  }

  private static void createMarkers(IDocument document, IFile file, Collection<Trackable> issues) throws CoreException {
    for (Trackable issue : issues) {
      if (!issue.isResolved()) {
        createMarker(document, file, issue);
      }
    }
  }

  private static void createMarker(IDocument document, IFile file, Trackable trackable) throws CoreException {
    Map<String, Object> attributes = new HashMap<>();

    attributes.put(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, trackable.getRuleKey());
    attributes.put(MarkerUtils.SONAR_MARKER_RULE_NAME_ATTR, trackable.getRuleName());
    attributes.put(IMarker.PRIORITY, getPriority(trackable.getSeverity()));
    attributes.put(IMarker.SEVERITY, PreferencesUtils.getMarkerSeverity());
    attributes.put(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR, trackable.getSeverity());

    attributes.put(IMarker.MESSAGE, trackable.getMessage());
    attributes.put(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, trackable.getServerIssueKey());
    attributes.put(MarkerUtils.SONAR_MARKER_ASSIGNEE_ATTR, trackable.getAssignee());

    // File level issues (line == null) are displayed on line 1
    attributes.put(IMarker.LINE_NUMBER, trackable.getLine() != null ? trackable.getLine() : 1);

    FlatTextRange textRange = MarkerUtils.getFlatTextRange(document, trackable.getTextRange());
    if (textRange != null) {
      attributes.put(IMarker.CHAR_START, textRange.getStart());
      attributes.put(IMarker.CHAR_END, textRange.getEnd());
    }

    Long creationDate = trackable.getCreationDate();
    if (creationDate != null) {
      attributes.put(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, String.valueOf(creationDate.longValue()));
    }

    IMarker marker = file.createMarker(SonarLintCorePlugin.MARKER_ID);
    marker.setAttributes(attributes);
  }

  /**
   * @return Priority marker attribute. A number from the set of high, normal and low priorities defined by the platform.
   *
   * @see IMarker.PRIORITY_HIGH
   * @see IMarker.PRIORITY_NORMAL
   * @see IMarker.PRIORITY_LOW
   */
  private static int getPriority(final String severity) {
    switch (severity.toLowerCase(Locale.ENGLISH)) {
      case "blocker":
      case "critical":
        return IMarker.PRIORITY_HIGH;
      case "major":
        return IMarker.PRIORITY_NORMAL;
      case "minor":
      case "info":
      default:
        return IMarker.PRIORITY_LOW;
    }
  }
}
