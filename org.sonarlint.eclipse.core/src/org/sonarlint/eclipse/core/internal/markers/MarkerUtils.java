/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
import java.util.Objects;
import java.util.function.BiFunction;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.markers.TextRange.FullTextRange;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

public final class MarkerUtils {

  public static final String SONAR_MARKER_RULE_KEY_ATTR = "rulekey";
  public static final String SONAR_MARKER_RULE_NAME_ATTR = "rulename";
  public static final String SONAR_MARKER_ISSUE_SEVERITY_ATTR = "sonarseverity";
  public static final String SONAR_MARKER_ISSUE_TYPE_ATTR = "issuetype";
  public static final String SONAR_MARKER_CREATION_DATE_ATTR = "creationdate";

  public static final String SONAR_MARKER_SERVER_ISSUE_KEY_ATTR = "serverissuekey";
  public static final String SONAR_MARKER_HAS_EXTRA_LOCATION_KEY_ATTR = "hasextralocation";
  public static final String SONAR_MARKER_EXTRA_LOCATIONS_ATTR = "extralocation";

  public static final String SONARLINT_EXTRA_POSITIONS_CATEGORY = "sonarlintextralocations";

  private MarkerUtils() {
  }

  public static List<IMarker> findReportIssuesMarkers(IResource resource) {
    try {
      return Arrays.asList(resource.findMarkers(SonarLintCorePlugin.MARKER_REPORT_ID, true, IResource.DEPTH_INFINITE));
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
      return Collections.emptyList();
    }
  }

  public static void updateAllSonarMarkerSeverity() throws CoreException {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (project.isAccessible()) {
        for (IMarker marker : project.findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_INFINITE)) {
          marker.setAttribute(IMarker.SEVERITY, PreferencesUtils.getMarkerSeverity());
        }
      }
    }
  }

  @CheckForNull
  public static Position getPosition(final IDocument document, @Nullable TextRange textRange) {
    if (textRange == null || ! textRange.isValid()) {
      return null;
    }
    if (textRange.isLineOnly()) {
      return getPosition(document, textRange.getStartLine());
    }
    return getPosition(document, (FullTextRange) textRange);
  }

  @CheckForNull
  public static Position getPosition(final IDocument document, int startLine) {
    int startLineStartOffset;
    int length;
    String lineDelimiter;
    try {
      startLineStartOffset = document.getLineOffset(startLine - 1);
      length = document.getLineLength(startLine - 1);
      lineDelimiter = document.getLineDelimiter(startLine - 1);
    } catch (BadLocationException e) {
      SonarLintLogger.get().error("failed to compute flat text range for line " + startLine, e);
      return null;
    }

    int lineDelimiterLength = lineDelimiter != null ? lineDelimiter.length() : 0;

    return new Position(startLineStartOffset, length - lineDelimiterLength);
  }

  @CheckForNull
  public static Position getPosition(final IDocument document, FullTextRange textRange) {
    try {
      return convertToGlobalOffset(document, textRange, Position::new);
    } catch (BadLocationException e) {
      SonarLintLogger.get().error("failed to compute line offsets for start, end = " + textRange.getStartLine() + ", " + textRange.getEndLine(), e);
      return null;
    }
  }

  @CheckForNull
  public static ExtraPosition getExtraPosition(final IDocument document, TextRange textRange, String message, long markerId,
    ExtraPosition parent) {
    try {
      return convertToGlobalOffset(document, (FullTextRange) textRange, (o, l) -> new ExtraPosition(o, l, message, markerId, parent));
    } catch (BadLocationException e) {
      SonarLintLogger.get().error("failed to compute line offsets for start, end = " + textRange.getStartLine() + ", " + textRange.getEndLine(), e);
      return null;
    }
  }

  private static <G> G convertToGlobalOffset(final IDocument document, FullTextRange textRange, BiFunction<Integer, Integer, G> function)
    throws BadLocationException {
    int startLineStartOffset = document.getLineOffset(textRange.getStartLine() - 1);
    int endLineStartOffset = textRange.getEndLine() != textRange.getStartLine() ? document.getLineOffset(textRange.getEndLine() - 1) : startLineStartOffset;
    int start = startLineStartOffset + textRange.getStartLineOffset();
    int end = endLineStartOffset + textRange.getEndLineOffset();
    return function.apply(start, end - start);
  }

  @CheckForNull
  public static RuleKey getRuleKey(IMarker marker) {
    String repositoryAndKey = marker.getAttribute(SONAR_MARKER_RULE_KEY_ATTR, null);
    if (repositoryAndKey == null) {
      return null;
    }
    return RuleKey.parse(repositoryAndKey);
  }

  public static class ExtraPosition extends Position {
    private final String message;
    private final long markerId;
    private final ExtraPosition previous;

    public ExtraPosition(int offset, int length, @Nullable String message, long markerId, @Nullable ExtraPosition previous) {
      super(offset, length);
      this.message = message;
      this.markerId = markerId;
      this.previous = previous;
    }

    @CheckForNull
    public String getMessage() {
      return message;
    }

    public long getMarkerId() {
      return markerId;
    }

    @CheckForNull
    public ExtraPosition getParent() {
      return previous;
    }

    public boolean isDescendantOf(@Nullable ExtraPosition possibleAncestor) {
      if (previous == null) {
        return this.equals(possibleAncestor);
      } else {
        return previous.isDescendantOf(possibleAncestor);
      }
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof ExtraPosition) {
        ExtraPosition rp = (ExtraPosition) other;
        return (rp.offset == offset)
          && (rp.length == length)
          && (rp.markerId == markerId)
          && Objects.equals(rp.message, message)
          && Objects.equals(rp.previous, previous);
      }
      return super.equals(other);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (int) (markerId ^ (markerId >>> 32));
      return result;
    }
  }

}
