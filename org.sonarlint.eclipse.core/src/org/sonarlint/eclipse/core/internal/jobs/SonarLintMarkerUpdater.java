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
package org.sonarlint.eclipse.core.internal.jobs;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;

public class SonarLintMarkerUpdater {

  public static void createOrUpdateMarkers(IResource resource, IDocument document, Collection<Trackable> issues, TriggerType triggerType) {
    try {
      Set<IMarker> previousMarkersToDelete;
      if (triggerType == TriggerType.CHANGESET) {
        previousMarkersToDelete = new HashSet<>(Arrays.asList(resource.findMarkers(SonarLintCorePlugin.MARKER_CHANGESET_ID, false, IResource.DEPTH_ZERO)));
      } else {
        previousMarkersToDelete = new HashSet<>(Arrays.asList(resource.findMarkers(SonarLintCorePlugin.MARKER_ID, false, IResource.DEPTH_ZERO)));
      }

      createOrUpdateMarkers(document, resource, issues, triggerType, previousMarkersToDelete);

      for (IMarker marker : previousMarkersToDelete) {
        marker.delete();
      }
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  public static void updateMarkersWithServerSideData(IResource resource, Collection<Trackable> issues) {
    try {
      // We are expecting every issue to be associated to an existing marker
      for (Trackable issue : issues) {
        IMarker marker = resource.findMarker(issue.getMarkerId());
        if (issue.isResolved()) {
          marker.delete();
          issue.setMarkerId(null);
        } else {
          updateServerMarkerAttributes(issue, marker);
        }
      }
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  private static void createOrUpdateMarkers(IDocument document, IResource file, Collection<Trackable> issues,
    TriggerType triggerType, Set<IMarker> previousMarkersToDelete) throws CoreException {
    for (Trackable issue : issues) {
      if (!issue.isResolved()) {
        if (issue.getMarkerId() == null || file.findMarker(issue.getMarkerId()) == null) {
          createMarker(document, file, issue, triggerType);
        } else {
          IMarker marker = file.findMarker(issue.getMarkerId());
          updateMarkerAttributes(document, issue, marker);
          previousMarkersToDelete.remove(marker);
        }
      } else {
        issue.setMarkerId(null);
      }
    }
  }

  private static void createMarker(IDocument document, IResource file, Trackable trackable, TriggerType triggerType) throws CoreException {
    IMarker marker = file.createMarker(triggerType == TriggerType.CHANGESET ? SonarLintCorePlugin.MARKER_CHANGESET_ID : SonarLintCorePlugin.MARKER_ID);
    trackable.setMarkerId(marker.getId());

    updateMarkerAttributes(document, trackable, marker);
  }

  private static void updateMarkerAttributes(IDocument document, Trackable trackable, IMarker marker) throws CoreException {
    Map<String, Object> existingAttributes = marker.getAttributes();

    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, trackable.getRuleKey());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RULE_NAME_ATTR, trackable.getRuleName());
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.SEVERITY, PreferencesUtils.getMarkerSeverity());

    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.MESSAGE, trackable.getMessage());

    // File level issues (line == null) are displayed on line 1
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.LINE_NUMBER, trackable.getLine() != null ? trackable.getLine() : 1);

    Position position = MarkerUtils.getPosition(document, trackable.getTextRange());
    if (position != null) {
      setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.CHAR_START, position.getOffset());
      setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.CHAR_END, position.getOffset() + position.getLength());
    }

    updateServerMarkerAttributes(trackable, marker);
  }

  /**
   * After tracking issue from server, only a few attributes need to be updated:
   *   - severity (may be changed on server side)
   *   - server issue key
   *   - creation date
   */
  private static void updateServerMarkerAttributes(Trackable trackable, IMarker marker) throws CoreException {
    Map<String, Object> existingAttributes = marker.getAttributes();

    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.PRIORITY, getPriority(trackable.getSeverity()));
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR, trackable.getSeverity());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, trackable.getServerIssueKey());

    Long creationDate = trackable.getCreationDate();
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, creationDate != null ? String.valueOf(creationDate.longValue()) : null);
  }

  private static void setMarkerAttributeIfDifferent(IMarker marker, @Nullable Map<String, Object> existingAttributes, String attributeName, @Nullable Object value)
    throws CoreException {
    if (!Objects.equals(value, existingAttributes != null ? existingAttributes.get(attributeName) : null)) {
      marker.setAttribute(attributeName, value);
    }
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
