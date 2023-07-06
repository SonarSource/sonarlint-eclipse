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
package org.sonarlint.eclipse.core.internal.markers;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFixes;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.commons.RuleKey;
import org.sonarsource.sonarlint.core.commons.TextRange;

public final class MarkerUtils {

  public static final String SONAR_MARKER_RULE_KEY_ATTR = "rulekey";
  public static final String SONAR_MARKER_ISSUE_SEVERITY_ATTR = "sonarseverity";
  public static final String SONAR_MARKER_ISSUE_TYPE_ATTR = "issuetype";
  public static final String SONAR_MARKER_CREATION_DATE_ATTR = "creationdate";

  public static final String SONAR_MARKER_SERVER_ISSUE_KEY_ATTR = "serverissuekey";
  public static final String SONAR_MARKER_EXTRA_LOCATIONS_ATTR = "extralocations";
  public static final String SONAR_MARKER_QUICK_FIXES_ATTR = "quickfixes";
  public static final String SONAR_MARKER_RULE_DESC_CONTEXT_KEY_ATTR = "rulecontextkey";

  public static final Set<String> SONARLINT_PRIMARY_MARKER_IDS = Set.of(
    SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, SonarLintCorePlugin.MARKER_REPORT_ID, SonarLintCorePlugin.MARKER_TAINT_ID);
  
  /** Matching status of an issue: Either found locally, on SonarCloud or SonarQube */
  public enum FindingMatchingStatus {
    NOT_MATCHED,
    MATCHED_WITH_SQ,
    MATCHED_WITH_SC
  }

  private MarkerUtils() {
  }

  public static void updateAllSonarMarkerSeverity() throws CoreException {
    for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
      if (project.isAccessible()) {
        for (IMarker marker : project.findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_INFINITE)) {
          marker.setAttribute(IMarker.SEVERITY, SonarLintGlobalConfiguration.getMarkerSeverity());
        }
      }
    }
  }
  
  /**
   *  Get the matching status of a specific markers' issue by id
   *  
   *  @param markerId for the marker <-> project connection
   *  @param markerServerKey marker information from the connection, null if not on server
   *  @return specific matching status of a markers' issue
   */
  public static FindingMatchingStatus getMatchingStatus(IMarker marker, @Nullable String markerServerKey) {
    var bindingOptional = SonarLintCorePlugin.getServersManager()
      .resolveBinding(Adapters.adapt(marker.getResource().getProject(), ISonarLintProject.class));
    if (bindingOptional.isEmpty() || markerServerKey == null) {
      return FindingMatchingStatus.NOT_MATCHED;
    }
    if (bindingOptional.get().getEngineFacade().isSonarCloud()) {
      return FindingMatchingStatus.MATCHED_WITH_SC;
    }
    return FindingMatchingStatus.MATCHED_WITH_SQ;
  }

  @Nullable
  public static Position getPosition(final IDocument document, @Nullable TextRange textRange) {
    if (textRange == null) {
      return null;
    }
    try {
      return convertToGlobalOffset(document, textRange, Position::new);
    } catch (BadLocationException e) {
      SonarLintLogger.get().error("failed to compute line offsets for start, end = " + textRange.getStartLine() + ", " + textRange.getEndLine(), e);
      return null;
    }
  }

  @Nullable
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

    var lineDelimiterLength = lineDelimiter != null ? lineDelimiter.length() : 0;

    return new Position(startLineStartOffset, length - lineDelimiterLength);
  }

  private static <G> G convertToGlobalOffset(final IDocument document, TextRange textRange, BiFunction<Integer, Integer, G> function)
    throws BadLocationException {
    var startLineStartOffset = document.getLineOffset(textRange.getStartLine() - 1);
    var endLineStartOffset = textRange.getEndLine() != textRange.getStartLine() ? document.getLineOffset(textRange.getEndLine() - 1) : startLineStartOffset;
    var start = startLineStartOffset + textRange.getStartLineOffset();
    var end = endLineStartOffset + textRange.getEndLineOffset();
    return function.apply(start, end - start);
  }

  @Nullable
  public static RuleKey getRuleKey(IMarker marker) {
    var repositoryAndKey = marker.getAttribute(SONAR_MARKER_RULE_KEY_ATTR, null);
    if (repositoryAndKey == null) {
      return null;
    }
    return RuleKey.parse(repositoryAndKey);
  }

  public static MarkerFlows getIssueFlows(IMarker marker) {
    try {
      return Optional.ofNullable((MarkerFlows) marker.getAttribute(SONAR_MARKER_EXTRA_LOCATIONS_ATTR)).orElseGet(() -> new MarkerFlows(Collections.emptyList()));
    } catch (CoreException e) {
      return new MarkerFlows(Collections.emptyList());
    }
  }

  public static MarkerQuickFixes getIssueQuickFixes(IMarker marker) {
    try {
      return Optional.ofNullable((MarkerQuickFixes) marker.getAttribute(SONAR_MARKER_QUICK_FIXES_ATTR)).orElseGet(() -> new MarkerQuickFixes(Collections.emptyList()));
    } catch (CoreException e) {
      return new MarkerQuickFixes(Collections.emptyList());
    }
  }

}
