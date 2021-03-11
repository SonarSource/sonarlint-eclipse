/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlow;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlowLocation;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlows;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.listener.TaintVulnerabilitiesListener;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueLocation;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue.Flow;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssueLocation;

public class SonarLintMarkerUpdater {

  private static TaintVulnerabilitiesListener taintVulnerabilitiesListener;

  private SonarLintMarkerUpdater() {
  }

  public static void setTaintVulnerabilitiesListener(TaintVulnerabilitiesListener listener) {
    taintVulnerabilitiesListener = listener;
  }

  public static void createOrUpdateMarkers(ISonarLintFile file, Optional<IDocument> openedDocument, Collection<Trackable> issues, TriggerType triggerType) {
    try {
      Set<IMarker> previousMarkersToDelete;
      if (triggerType.isOnTheFly()) {
        file.getResource().deleteMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_FLOW_ID, false, IResource.DEPTH_ZERO);
        previousMarkersToDelete = new HashSet<>(Arrays.asList(file.getResource().findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, false, IResource.DEPTH_ZERO)));
      } else {
        previousMarkersToDelete = Collections.emptySet();
      }

      createOrUpdateMarkers(file, openedDocument, issues, triggerType, previousMarkersToDelete);

      for (IMarker marker : previousMarkersToDelete) {
        marker.delete();
      }
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  public static void refreshMarkersForTaint(ISonarLintFile currentFile, ConnectedEngineFacade facade) {
    deleteTaintMarkers(currentFile);

    ISonarLintProject project = currentFile.getProject();
    SonarLintProjectConfiguration config = SonarLintCorePlugin.loadConfig(project);
    config.getProjectBinding().ifPresent(binding -> {
      List<ServerIssue> taintVulnerabilities = facade.getServerIssues(binding, currentFile.getProjectRelativePath())
        .stream()
        .filter(i -> i.ruleKey().contains("security"))
        .filter(i -> StringUtils.isEmpty(i.resolution()))
        .collect(Collectors.toList());

      List<ISonarLintProject> boundSiblingProjects = facade.getBoundProjects(binding.projectKey());
      Map<ISonarLintProject, EclipseProjectBinding> bindings = boundSiblingProjects.stream()
        .collect(Collectors.toMap(p -> p, p -> SonarLintCorePlugin.loadConfig(p).getProjectBinding().get()));

      for (ServerIssue taintIssue : taintVulnerabilities) {
        Optional<ISonarLintFile> primaryLocationFile = findFileForLocationInBoundProjects(bindings, taintIssue.getFilePath());
        if (primaryLocationFile.isPresent()) {
          createTaintMarker(primaryLocationFile.get().getDocument(), primaryLocationFile.get(), taintIssue, bindings);
        }
      }
      if (!taintVulnerabilities.isEmpty() && taintVulnerabilitiesListener != null) {
        taintVulnerabilitiesListener.markersCreated(facade.isSonarCloud());
      }
    });

  }

  public static void deleteTaintMarkers(ISonarLintFile currentFile) {
    try {
      Set<IMarker> markersToDelete = new HashSet<>(Arrays.asList(currentFile.getResource().findMarkers(SonarLintCorePlugin.MARKER_TAINT_ID, false, IResource.DEPTH_ZERO)));
      for (IMarker primaryLocationMarker : markersToDelete) {
        MarkerUtils.getIssueFlows(primaryLocationMarker).deleteAllMarkers();
        primaryLocationMarker.delete();
      }
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  private static Optional<ISonarLintFile> findFileForLocationInBoundProjects(Map<ISonarLintProject, EclipseProjectBinding> bindingsPerProjects, @Nullable String serverIssuePath) {
    if (serverIssuePath == null) {
      // Should never occur, no taint issues are at file level
      return Optional.empty();
    }
    for (Map.Entry<ISonarLintProject, EclipseProjectBinding> entry : bindingsPerProjects.entrySet()) {
      Optional<String> idePath = entry.getValue().serverPathToIdePath(serverIssuePath);
      if (idePath.isPresent()) {
        Optional<ISonarLintFile> primaryLocationFile = entry.getKey().find(idePath.get());
        if (primaryLocationFile.isPresent()) {
          return primaryLocationFile;
        }
      }
    }
    return Optional.empty();
  }

  public static Set<IResource> getResourcesWithMarkers(ISonarLintProject project) throws CoreException {
    return Arrays.stream(project.getResource().findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, false, IResource.DEPTH_INFINITE))
      .map(IMarker::getResource)
      .collect(Collectors.toSet());
  }

  public static void clearMarkers(ISonarLintFile file) {
    try {
      file.getResource().deleteMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, false, IResource.DEPTH_ZERO);
      file.getResource().deleteMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_FLOW_ID, false, IResource.DEPTH_ZERO);
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  public static void updateMarkersWithServerSideData(ISonarLintIssuable issuable, IDocument document, Collection<Trackable> issues, TriggerType triggerType) {
    try {
      for (Trackable issue : issues) {
        updateMarkerWithServerSideData(issuable, document, issue, triggerType);
      }
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  private static void updateMarkerWithServerSideData(ISonarLintIssuable issuable, IDocument document, Trackable issue,
    TriggerType triggerType)
    throws CoreException {
    Long markerId = issue.getMarkerId();
    IMarker marker = null;
    if (markerId != null) {
      marker = issuable.getResource().findMarker(markerId);
    }
    if (issue.isResolved()) {
      if (marker != null) {
        // Issue is associated to a marker, means it was not marked as resolved in previous analysis, but now it is, so clear marker
        marker.delete();
      }
      issue.setMarkerId(null);
    } else {
      if (marker != null) {
        updateServerMarkerAttributes(issue, marker);
      } else {
        // Issue was previously resolved, and is now reopen, so we need to recreate a marker
        createMarker(document, issuable, issue, triggerType);
      }
    }
  }

  private static void createOrUpdateMarkers(ISonarLintFile file, Optional<IDocument> openedDocument, Collection<Trackable> issues,
    TriggerType triggerType, Set<IMarker> previousMarkersToDelete) throws CoreException {
    IDocument lazyInitDocument = openedDocument.orElse(null);
    for (Trackable issue : issues) {
      if (!issue.isResolved()) {
        lazyInitDocument = lazyInitDocument != null ? lazyInitDocument : file.getDocument();
        if (!triggerType.isOnTheFly() || issue.getMarkerId() == null || file.getResource().findMarker(issue.getMarkerId()) == null) {
          createMarker(lazyInitDocument, file, issue, triggerType);
        } else {
          IMarker marker = file.getResource().findMarker(issue.getMarkerId());
          updateMarkerAttributes(lazyInitDocument, issue, marker);
          createFlowMarkersForLocalIssues(lazyInitDocument, file, issue, marker, markerIdForFlows(triggerType));
          previousMarkersToDelete.remove(marker);
        }
      } else {
        issue.setMarkerId(null);
      }
    }
  }

  private static void createMarker(IDocument document, ISonarLintIssuable issuable, Trackable trackable, TriggerType triggerType) throws CoreException {
    IMarker marker = issuable.getResource()
      .createMarker(triggerType.isOnTheFly() ? SonarLintCorePlugin.MARKER_ON_THE_FLY_ID : SonarLintCorePlugin.MARKER_REPORT_ID);
    if (triggerType.isOnTheFly()) {
      trackable.setMarkerId(marker.getId());
    }

    setMarkerViewUtilsAttributes(issuable, marker);

    updateMarkerAttributes(document, trackable, marker);
    createFlowMarkersForLocalIssues(document, issuable, trackable, marker, markerIdForFlows(triggerType));

  }

  private static String markerIdForFlows(TriggerType triggerType) {
    return triggerType.isOnTheFly() ? SonarLintCorePlugin.MARKER_ON_THE_FLY_FLOW_ID : SonarLintCorePlugin.MARKER_REPORT_FLOW_ID;
  }

  private static void createTaintMarker(IDocument document, ISonarLintIssuable issuable, ServerIssue taintIssue,
    Map<ISonarLintProject, EclipseProjectBinding> bindingsPerProjects) {
    try {
      IMarker marker = issuable.getResource().createMarker(SonarLintCorePlugin.MARKER_TAINT_ID);

      setMarkerViewUtilsAttributes(issuable, marker);

      updateMarkerAttributes(document, new ServerIssueTrackable(taintIssue), marker);
      createFlowMarkersForTaint(taintIssue, marker, bindingsPerProjects);
    } catch (CoreException e) {
      SonarLintLogger.get().error("Unable to create marker", e);
    }
  }

  private static void setMarkerViewUtilsAttributes(ISonarLintIssuable issuable, IMarker marker) throws CoreException {
    // See MarkerViewUtil
    marker.setAttribute("org.eclipse.ui.views.markers.name", issuable.getResourceNameForMarker());
    marker.setAttribute("org.eclipse.ui.views.markers.path", issuable.getResourceContainerForMarker());
  }

  private static void updateMarkerAttributes(IDocument document, Trackable trackable, IMarker marker) throws CoreException {
    Map<String, Object> existingAttributes = marker.getAttributes();

    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, trackable.getRuleKey());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RULE_NAME_ATTR, trackable.getRuleName());
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.SEVERITY, SonarLintGlobalConfiguration.getMarkerSeverity());

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

  private static void createFlowMarkersForLocalIssues(IDocument document, ISonarLintIssuable issuable, Trackable trackable, IMarker marker, String flowMarkerId)
    throws CoreException {
    List<MarkerFlow> flows = new ArrayList<>();
    int i = 1;
    for (org.sonarsource.sonarlint.core.client.api.common.analysis.Issue.Flow engineFlow : trackable.getFlows()) {
      MarkerFlow flow = new MarkerFlow(i);
      flows.add(flow);
      List<IssueLocation> locations = new ArrayList<>(engineFlow.locations());
      Collections.reverse(locations);
      for (IssueLocation l : locations) {
        l.getInputFile();
        MarkerFlowLocation flowLocation = new MarkerFlowLocation(flow, l.getMessage());
        try {
          IMarker m = issuable.getResource().createMarker(flowMarkerId);
          m.setAttribute(IMarker.MESSAGE, l.getMessage());
          m.setAttribute(IMarker.LINE_NUMBER, l.getStartLine() != null ? l.getStartLine() : 1);
          Position flowPosition = MarkerUtils.getPosition(document, TextRange.get(l.getStartLine(), l.getStartLineOffset(), l.getEndLine(), l.getEndLineOffset()));
          if (flowPosition != null) {
            m.setAttribute(IMarker.CHAR_START, flowPosition.getOffset());
            m.setAttribute(IMarker.CHAR_END, flowPosition.getOffset() + flowPosition.getLength());
          }
          flowLocation.setMarker(m);
        } catch (Exception e) {
          SonarLintLogger.get().debug("Unable to create flow marker", e);
        }
      }
      i++;
    }
    marker.setAttribute(MarkerUtils.SONAR_MARKER_EXTRA_LOCATIONS_ATTR, new MarkerFlows(flows));
  }

  private static void createFlowMarkersForTaint(ServerIssue taintIssue, IMarker primaryLocationMarker, Map<ISonarLintProject, EclipseProjectBinding> bindingsPerProjects)
    throws CoreException {
    List<MarkerFlow> flows = new ArrayList<>();
    int i = 1;
    for (Flow engineFlow : taintIssue.getFlows()) {
      MarkerFlow flow = new MarkerFlow(i);
      flows.add(flow);
      List<ServerIssueLocation> locations = new ArrayList<>(engineFlow.locations());
      Collections.reverse(locations);
      for (ServerIssueLocation l : locations) {
        MarkerFlowLocation flowLocation = new MarkerFlowLocation(flow, l.getMessage(), l.getFilePath());

        Optional<ISonarLintFile> locationFile = findFileForLocationInBoundProjects(bindingsPerProjects, l.getFilePath());
        if (!locationFile.isPresent()) {
          continue;
        }
        ISonarLintFile file = locationFile.get();
        try {
          IMarker marker = createMarkerIfCodeMatches(file, l);
          if (marker != null) {
            flowLocation.setMarker(marker);
          } else {
            flowLocation.setDeleted(true);
          }
        } catch (Exception e) {
          SonarLintLogger.get().debug("Unable to create flow marker", e);
        }
      }
      i++;
    }
    primaryLocationMarker.setAttribute(MarkerUtils.SONAR_MARKER_EXTRA_LOCATIONS_ATTR, new MarkerFlows(flows));
  }

  @Nullable
  private static IMarker createMarkerIfCodeMatches(ISonarLintFile file, ServerIssueLocation location) throws BadLocationException, CoreException {
    IDocument document = file.getDocument();
    int startOffset = document.getLineOffset(location.getStartLine() - 1) + location.getStartLineOffset();
    int endOffset = document.getLineOffset(location.getEndLine() - 1) + location.getEndLineOffset();
    String inEditorCode = document.get(startOffset, endOffset - startOffset);
    if (inEditorCode.equals(location.getCodeSnippet())) {
      IMarker marker = file.getResource().createMarker(SonarLintCorePlugin.MARKER_TAINT_FLOW_ID);
      marker.setAttribute(IMarker.MESSAGE, location.getMessage());
      marker.setAttribute(IMarker.LINE_NUMBER, location.getStartLine() != null ? location.getStartLine() : 1);
      Position flowPosition = MarkerUtils.getPosition(document,
        TextRange.get(location.getStartLine(), location.getStartLineOffset(), location.getEndLine(), location.getEndLineOffset()));
      if (flowPosition != null) {
        marker.setAttribute(IMarker.CHAR_START, flowPosition.getOffset());
        marker.setAttribute(IMarker.CHAR_END, flowPosition.getOffset() + flowPosition.getLength());
      }
      return marker;
    }
    return null;
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
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_TYPE_ATTR, trackable.getType());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, trackable.getServerIssueKey());

    Long creationDate = trackable.getCreationDate();
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, creationDate != null ? String.valueOf(creationDate) : null);
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

  public static void deleteAllMarkersFromReport() {
    ProjectsProviderUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen)
      .forEach(p -> {
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_REPORT_ID);
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_REPORT_FLOW_ID);
      });
  }

  public static void deleteAllMarkersFromTaint() {
    ProjectsProviderUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen)
      .forEach(p -> {
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_TAINT_ID);
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_TAINT_FLOW_ID);
      });
  }
}
