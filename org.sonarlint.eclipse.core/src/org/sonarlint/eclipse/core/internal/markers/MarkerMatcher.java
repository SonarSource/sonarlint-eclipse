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

import java.util.Optional;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.Position;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarsource.sonarlint.core.clientapi.client.issue.ShowIssueParams;
import org.sonarsource.sonarlint.core.commons.TextRange;

/**
 *  When we open an existing SonarQube issue in the IDE we directly want to match the incomplete issue information we
 *  get with the markers we already have on the project files and not go via the
 *  {@link org.sonarlint.eclipse.core.internal.tracking.TrackedIssue} or
 *  {@link org.sonarsource.sonarlint.core.serverconnection.issues.ServerTaintIssue} objects.
 *  
 *  Sometimes issues might not be found because of the workspace preferences regarding new code / issue filter!
 */
public class MarkerMatcher {
  private MarkerMatcher() {
    // utility class
  }
  
  public static Optional<IMarker> tryMatchIssueWithOnTheFlyMarker(ShowIssueParams params,
    ISonarLintFile file) throws CoreException {
    return tryMatchIssueWithMarker(params, file, SonarLintCorePlugin.MARKER_ON_THE_FLY_ID);
  }
  
  public static Optional<IMarker> tryMatchIssueWithReportMarker(ShowIssueParams params,
    ISonarLintFile file) throws CoreException {
    return tryMatchIssueWithMarker(params, file, SonarLintCorePlugin.MARKER_REPORT_ID);
  }
  
  public static Optional<IMarker> tryMatchIssueWithTaintMarker(ShowIssueParams params,
    ISonarLintFile file) throws CoreException {
    return tryMatchIssueWithMarker(params, file, SonarLintCorePlugin.MARKER_TAINT_ID);
  }
  
  /**
   *  Tries to match {@link ShowIssueParams} with any marker of a specific file
   *  
   *  @param params when using "Open in IDE" in SonarQube
   *  @param file the local file corresponding to the issue
   *  @param markerType the type of markers we take into account
   *  @return marker if found, none otherwise
   */
  private static Optional<IMarker> tryMatchIssueWithMarker(ShowIssueParams params,
    ISonarLintFile file, String markerType) throws CoreException {
    var markers = file.getResource().findMarkers(markerType, true, IResource.DEPTH_ONE);
    if (markers.length == 0) {
      return Optional.empty();
    }
    
    var textRangeDto = params.getTextRange();
    var position = MarkerUtils.getPosition(file.getDocument(),
      new TextRange(textRangeDto.getStartLine(), textRangeDto.getStartLineOffset(), textRangeDto.getEndLine(),
        textRangeDto.getEndLineOffset()));
    
    for (var marker : markers) {
      if (issueMatchesWithMarker(params, marker, position)) {
        return Optional.of(marker);
      }
    }
    
    return Optional.empty();
  }
  
  /**
   *  Check if an issue matches a specific marker
   *  
   *  INFO: We don't match the creation date as the information is provided completely different on ShowIssueParams,
   *  ServerTaintIssue and TrackedIssue.
   *  
   *  @param params when using "Open in IDE" in SonarQube
   *  @param marker the marker in the local file
   *  @param position position inside the file
   *  @return whether the issue matches the marker
   */
  private static boolean issueMatchesWithMarker(ShowIssueParams params, IMarker marker,
    @Nullable Position position) throws CoreException {
    var result = params.getIssueKey().equals(marker.getAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR))
      && params.getRuleKey().equals(marker.getAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR))
      && params.getMessage().equals(marker.getAttribute(IMarker.MESSAGE));

    if (position != null) {
      result &= params.getTextRange().getStartLine() == marker.getAttribute(IMarker.LINE_NUMBER, -1)
        && position.getOffset() == marker.getAttribute(IMarker.CHAR_START, -1)
        && position.getOffset() + position.getLength() == marker.getAttribute(IMarker.CHAR_END, -1);
    }
    
    return result;
  }
}
