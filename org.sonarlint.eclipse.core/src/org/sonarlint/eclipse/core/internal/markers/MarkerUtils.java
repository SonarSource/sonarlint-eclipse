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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

public final class MarkerUtils {

  public static final String SONAR_MARKER_RULE_KEY_ATTR = "rulekey";
  public static final String SONAR_MARKER_RULE_NAME_ATTR = "rulename";
  public static final String SONAR_MARKER_ISSUE_SEVERITY_ATTR = "sonarseverity";
  public static final String SONAR_MARKER_CREATION_DATE_ATTR = "creationdate";

  public static final String SONAR_MARKER_SERVER_ISSUE_KEY_ATTR = "serverissuekey";

  private MarkerUtils() {
  }

  public static void deleteIssuesMarkers(IResource resource) {
    deleteMarkers(resource, SonarLintCorePlugin.MARKER_ID);
  }

  public static void deleteChangeSetIssuesMarkers(IResource resource) {
    deleteMarkers(resource, SonarLintCorePlugin.MARKER_CHANGESET_ID);
  }

  public static void deleteMarkers(IResource resource, String markerId) {
    try {
      resource.deleteMarkers(markerId, true, IResource.DEPTH_INFINITE);
    } catch (CoreException e) {
      SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
    }
  }

  public static List<IMarker> findMarkers(IResource resource) {
    try {
      return Arrays.asList(resource.findMarkers(SonarLintCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE));
    } catch (CoreException e) {
      SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  public static void updateAllSonarMarkerSeverity() throws CoreException {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (project.isAccessible()) {
        for (IMarker marker : project.findMarkers(SonarLintCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE)) {
          marker.setAttribute(IMarker.SEVERITY, PreferencesUtils.getMarkerSeverity());
        }
      }
    }
  }

  @CheckForNull
  public static FlatTextRange getFlatTextRange(final IDocument document, @Nullable TextRange textRange) {
    if (textRange == null || textRange.getStartLine() == null) {
      return null;
    }
    if (textRange.getStartLineOffset() == null) {
      return getFlatTextRange(document, textRange.getStartLine());
    }
    return getFlatTextRange(document, textRange.getStartLine(), textRange.getStartLineOffset(), textRange.getEndLine(), textRange.getEndLineOffset());
  }

  @CheckForNull
  public static FlatTextRange getFlatTextRange(final IDocument document, int startLine) {
    int startLineStartOffset;
    int length;
    String lineDelimiter;
    try {
      startLineStartOffset = document.getLineOffset(startLine - 1);
      length = document.getLineLength(startLine - 1);
      lineDelimiter = document.getLineDelimiter(startLine - 1);
    } catch (BadLocationException e) {
      SonarLintCorePlugin.getDefault().error("failed to compute flat text range for line " + startLine, e);
      return null;
    }

    int lineDelimiterLength = lineDelimiter != null ? lineDelimiter.length() : 0;

    int start = startLineStartOffset;
    int end = startLineStartOffset + length - lineDelimiterLength;
    return new FlatTextRange(start, end);
  }

  @CheckForNull
  private static FlatTextRange getFlatTextRange(final IDocument document, int startLine, int startLineOffset, int endLine, int endLineOffset) {
    int startLineStartOffset;
    int endLineStartOffset;
    try {
      startLineStartOffset = document.getLineOffset(startLine - 1);
      endLineStartOffset = endLine != startLine ? document.getLineOffset(endLine - 1) : startLineStartOffset;
    } catch (BadLocationException e) {
      SonarLintCorePlugin.getDefault().error("failed to compute line offsets for start, end = " + startLine + ", " + endLine, e);
      return null;
    }

    int start = startLineStartOffset + startLineOffset;
    int end = endLineStartOffset + endLineOffset;
    return new FlatTextRange(start, end);
  }

}
