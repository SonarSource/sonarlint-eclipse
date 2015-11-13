/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.core.internal.markers;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.sonar.runner.api.Issue;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

public class SonarMarker {

  private final IResource resource;
  private final IMarker marker;

  private SonarMarker(final IResource resource) throws CoreException {
    this.resource = resource;
    this.marker = resource.createMarker(SonarLintCorePlugin.MARKER_ID);
  }

  public static void create(final IResource resource, final Issue issue) throws CoreException {
    if (StringUtils.isNotBlank(issue.getResolution())) {
      // Don't display resolved issues
      return;
    }
    new SonarMarker(resource).from(issue);
  }

  private SonarMarker from(final Issue issue) throws CoreException {
    final Map<String, Object> markerAttributes = new HashMap<>();
    Integer startLine = issue.getStartLine();
    markerAttributes.put(IMarker.PRIORITY, getPriority(issue.getSeverity()));
    markerAttributes.put(IMarker.SEVERITY, PreferencesUtils.getMarkerSeverity());
    // File level issues (line == null) are displayed on line 1
    markerAttributes.put(IMarker.LINE_NUMBER, startLine != null ? startLine : 1);
    markerAttributes.put(IMarker.MESSAGE, getMessage(issue));

    setPosition(markerAttributes, issue, resource);
    markerAttributes.put(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, issue.getRuleKey());
    markerAttributes.put(MarkerUtils.SONAR_MARKER_RULE_NAME_ATTR, issue.getRuleName());
    markerAttributes.put(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR, issue.getSeverity());

    marker.setAttributes(markerAttributes);
    return this;
  }

  private static String getMessage(final Issue issue) {
    return issue.getRuleKey() + " : " + issue.getMessage();
  }

  /**
   * @return Priority marker attribute. A number from the set of high, normal and low priorities defined by the platform.
   *
   * @see IMarker.PRIORITY_HIGH
   * @see IMarker.PRIORITY_NORMAL
   * @see IMarker.PRIORITY_LOW
   */
  private static Integer getPriority(final String severity) {
    int result = IMarker.PRIORITY_LOW;
    if ("blocker".equalsIgnoreCase(severity) || "critical".equalsIgnoreCase(severity)) {
      result = IMarker.PRIORITY_HIGH;
    } else if ("major".equalsIgnoreCase(severity)) {
      result = IMarker.PRIORITY_NORMAL;
    } else if ("minor".equalsIgnoreCase(severity) || "info".equalsIgnoreCase(severity)) {
      result = IMarker.PRIORITY_LOW;
    }
    return result;
  }

  // Visible for testing
  public static void setPosition(final Map<String, Object> markerAttributes, final Issue issue, final IResource resource) {
    if (resource instanceof IFile) {
      IFile iFile = (IFile) resource;
      Integer startLine = issue.getStartLine();
      if (startLine == null) {
        return;
      }
      ITextFileBufferManager iTextFileBufferManager = FileBuffers.getTextFileBufferManager();
      try {
        iTextFileBufferManager.connect(iFile.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
        ITextFileBuffer iTextFileBuffer = iTextFileBufferManager.getTextFileBuffer(iFile.getFullPath(), LocationKind.IFILE);
        IDocument iDoc = iTextFileBuffer.getDocument();
        int startLineStartOffset = iDoc.getLineOffset(startLine - 1);
        Integer issueStartLineOffset = issue.getStartLineOffset();
        if (issueStartLineOffset != null) {
          markerAttributes.put(IMarker.CHAR_START, startLineStartOffset + issueStartLineOffset);
          Integer issueEndLine = issue.getEndLine();
          int endLineStartOffset = issueEndLine != (int) startLine ? iDoc.getLineOffset(issueEndLine - 1) : startLineStartOffset;
          markerAttributes.put(IMarker.CHAR_END, endLineStartOffset + issue.getEndLineOffset());
        } else {
          markerAttributes.put(IMarker.CHAR_START, startLineStartOffset);
          int length = iDoc.getLineLength((int) startLine - 1);
          String lineDelimiter = iDoc.getLineDelimiter(startLine - 1);
          int lineDelimiterLength = lineDelimiter != null ? lineDelimiter.length() : 0;
          markerAttributes.put(IMarker.CHAR_END, startLineStartOffset + length - lineDelimiterLength);
        }
      } catch (Exception e) {
        SonarLintCorePlugin.getDefault().error("Unable to compute position of SonarLint marker on resource " + resource.getName() + ": " + e.getMessage());
      } finally {
        try {
          iTextFileBufferManager.disconnect(iFile.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
        } catch (CoreException e) {
          // Ignore
        }
      }

    }
  }

}
