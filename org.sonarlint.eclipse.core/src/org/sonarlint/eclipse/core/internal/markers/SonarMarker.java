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
package org.sonarlint.eclipse.core.internal.markers;

import java.util.Date;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

public class SonarMarker {

  private SonarMarker() {
  }

  public static IMarker create(final IDocument iDoc, final IResource resource, final Issue issue) throws CoreException {
    IMarker marker = resource.createMarker(SonarLintCorePlugin.MARKER_ID);
    updateAttributes(marker, issue, iDoc);
    marker.setAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, String.valueOf(new Date().getTime()));
    return marker;
  }

  public static void updateAttributes(final IMarker marker, final Issue issue, final IDocument iDoc) throws CoreException {
    Integer startLine = issue.getStartLine();
    marker.setAttribute(IMarker.PRIORITY, getPriority(issue.getSeverity()));
    marker.setAttribute(IMarker.SEVERITY, PreferencesUtils.getMarkerSeverity());
    // File level issues (line == null) are displayed on line 1
    marker.setAttribute(IMarker.LINE_NUMBER, startLine != null ? startLine : 1);
    marker.setAttribute(IMarker.MESSAGE, getMessage(issue));
    marker.setAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, issue.getRuleKey());
    marker.setAttribute(MarkerUtils.SONAR_MARKER_RULE_NAME_ATTR, issue.getRuleName());
    marker.setAttribute(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR, issue.getSeverity());

    if (iDoc != null && startLine != null) {
      try {
        updateLocationAndChecksum(marker, issue, iDoc);
      } catch (BadLocationException e) {
        SonarLintCorePlugin.getDefault().error("Error while updating markers", e);
      }
    } else {
      marker.setAttribute(IMarker.CHAR_START, null);
      marker.setAttribute(IMarker.CHAR_END, null);
      marker.setAttribute(MarkerUtils.SONAR_MARKER_CHECKSUM_ATTR, null);
    }
  }

  private static void updateLocationAndChecksum(final IMarker marker, final Issue issue, final IDocument iDoc) throws BadLocationException, CoreException {
    Range range = findRangeInFile(issue, iDoc);
    marker.setAttribute(IMarker.CHAR_START, range.getStartOffset());
    marker.setAttribute(IMarker.CHAR_END, range.getEndOffset());
    marker.setAttribute(MarkerUtils.SONAR_MARKER_CHECKSUM_ATTR, checksum(range.getContent()));
  }

  public static Range findRangeInFile(final Issue issue, final IDocument iDoc) throws BadLocationException {
    Integer startLine = issue.getStartLine();
    int startLineStartOffset = iDoc.getLineOffset(startLine - 1);
    Integer issueStartLineOffset = issue.getStartLineOffset();
    int start;
    int end;
    if (issueStartLineOffset != null) {
      start = startLineStartOffset + issueStartLineOffset;
      Integer issueEndLine = issue.getEndLine();
      int endLineStartOffset = issueEndLine != (int) startLine ? iDoc.getLineOffset(issueEndLine - 1) : startLineStartOffset;
      end = endLineStartOffset + issue.getEndLineOffset();
    } else {
      start = startLineStartOffset;
      int length = iDoc.getLineLength((int) startLine - 1);
      String lineDelimiter = iDoc.getLineDelimiter(startLine - 1);
      int lineDelimiterLength = lineDelimiter != null ? lineDelimiter.length() : 0;
      end = startLineStartOffset + length - lineDelimiterLength;
    }
    String content = iDoc.get(start, end - start);
    return new Range(start, end, content);
  }

  public static class Range {
    private final int startOffset;
    private final int endOffset;
    private final String content;

    public Range(int startOffset, int endOffset, String content) {
      this.startOffset = startOffset;
      this.endOffset = endOffset;
      this.content = content;
    }

    public int getStartOffset() {
      return startOffset;
    }

    public int getEndOffset() {
      return endOffset;
    }

    public String getContent() {
      return content;
    }

  }

  public static String getMessage(final Issue issue) {
    return issue.getMessage();
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

  public static int checksum(String content) {
    return content.replaceAll("[\\s]", "").hashCode();
  }

}
