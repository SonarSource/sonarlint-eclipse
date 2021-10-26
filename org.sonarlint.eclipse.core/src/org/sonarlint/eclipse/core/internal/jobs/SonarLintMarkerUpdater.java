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
import java.util.stream.Stream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
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
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFix;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFixes;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerTextEdit;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.listener.TaintVulnerabilitiesListener;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.QuickFix;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;
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
        previousMarkersToDelete = new HashSet<>(List.of(file.getResource().findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, false, IResource.DEPTH_ZERO)));
      } else {
        previousMarkersToDelete = Collections.emptySet();
      }

      createOrUpdateMarkers(file, openedDocument, issues, triggerType, previousMarkersToDelete);

      for (var marker : previousMarkersToDelete) {
        marker.delete();
      }
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  public static void refreshMarkersForTaint(ISonarLintFile currentFile, ConnectedEngineFacade facade) {
    deleteTaintMarkers(currentFile);

    var project = currentFile.getProject();
    var config = SonarLintCorePlugin.loadConfig(project);
    config.getProjectBinding().ifPresent(binding -> {
      var taintVulnerabilities = facade.getServerIssues(binding, currentFile.getProjectRelativePath())
        .stream()
        .filter(i -> i.ruleKey().contains("security"))
        .filter(i -> StringUtils.isEmpty(i.resolution()))
        .collect(Collectors.toList());

      var boundSiblingProjects = facade.getBoundProjects(binding.projectKey());
      var bindings = boundSiblingProjects.stream()
        .collect(Collectors.toMap(p -> p, p -> SonarLintCorePlugin.loadConfig(p).getProjectBinding().get()));

      for (var taintIssue : taintVulnerabilities) {
        findFileForLocationInBoundProjects(bindings, taintIssue.getFilePath())
          .ifPresent(primaryLocationFile -> createTaintMarker(primaryLocationFile.getDocument(), primaryLocationFile, taintIssue, bindings));
      }
      if (!taintVulnerabilities.isEmpty() && taintVulnerabilitiesListener != null) {
        taintVulnerabilitiesListener.markersCreated(facade.isSonarCloud());
      }
    });

  }

  public static void deleteTaintMarkers(ISonarLintFile currentFile) {
    try {
      var markersToDelete = new HashSet<>(List.of(currentFile.getResource().findMarkers(SonarLintCorePlugin.MARKER_TAINT_ID, false, IResource.DEPTH_ZERO)));
      for (var primaryLocationMarker : markersToDelete) {
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
    for (var entry : bindingsPerProjects.entrySet()) {
      var idePath = entry.getValue().serverPathToIdePath(serverIssuePath);
      if (idePath.isPresent()) {
        var primaryLocationFile = entry.getKey().find(idePath.get());
        if (primaryLocationFile.isPresent()) {
          return primaryLocationFile;
        }
      }
    }
    return Optional.empty();
  }

  public static Set<IResource> getResourcesWithMarkers(ISonarLintProject project) throws CoreException {
    return Stream.of(project.getResource().findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, false, IResource.DEPTH_INFINITE))
      .map(IMarker::getResource)
      .collect(Collectors.toSet());
  }

  public static void clearMarkers(ISonarLintFile file) {
    try {
      file.getResource().deleteMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, false, IResource.DEPTH_ZERO);
      file.getResource().deleteMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_FLOW_ID, false, IResource.DEPTH_ZERO);
      file.getResource().deleteMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_QUICK_FIX_ID, false, IResource.DEPTH_ZERO);
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  public static void updateMarkersWithServerSideData(ISonarLintIssuable issuable, IDocument document, Collection<Trackable> issues, TriggerType triggerType) {
    try {
      for (var issue : issues) {
        updateMarkerWithServerSideData(issuable, document, issue, triggerType);
      }
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  private static void updateMarkerWithServerSideData(ISonarLintIssuable issuable, IDocument document, Trackable issue,
    TriggerType triggerType)
    throws CoreException {
    var markerId = issue.getMarkerId();
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
    var lazyInitDocument = openedDocument.orElse(null);
    for (var issue : issues) {
      if (!issue.isResolved()) {
        lazyInitDocument = lazyInitDocument != null ? lazyInitDocument : file.getDocument();
        if (!triggerType.isOnTheFly() || issue.getMarkerId() == null || file.getResource().findMarker(issue.getMarkerId()) == null) {
          createMarker(lazyInitDocument, file, issue, triggerType);
        } else {
          var marker = file.getResource().findMarker(issue.getMarkerId());
          updateMarkerAttributes(lazyInitDocument, issue, marker);
          createFlowMarkersForLocalIssues(lazyInitDocument, file, issue, marker, triggerType);
          createQuickFixMarkersForLocalIssues(lazyInitDocument, file, issue, marker, triggerType);
          previousMarkersToDelete.remove(marker);
        }
      } else {
        issue.setMarkerId(null);
      }
    }
  }

  private static void createMarker(IDocument document, ISonarLintIssuable issuable, Trackable trackable, TriggerType triggerType) throws CoreException {
    var marker = issuable.getResource()
      .createMarker(triggerType.isOnTheFly() ? SonarLintCorePlugin.MARKER_ON_THE_FLY_ID : SonarLintCorePlugin.MARKER_REPORT_ID);
    if (triggerType.isOnTheFly()) {
      trackable.setMarkerId(marker.getId());
    }

    setMarkerViewUtilsAttributes(issuable, marker);

    updateMarkerAttributes(document, trackable, marker);
    createFlowMarkersForLocalIssues(document, issuable, trackable, marker, triggerType);
    createQuickFixMarkersForLocalIssues(document, issuable, trackable, marker, triggerType);

  }

  private static String markerIdForFlows(TriggerType triggerType) {
    return triggerType.isOnTheFly() ? SonarLintCorePlugin.MARKER_ON_THE_FLY_FLOW_ID : SonarLintCorePlugin.MARKER_REPORT_FLOW_ID;
  }

  private static void createTaintMarker(IDocument document, ISonarLintIssuable issuable, ServerIssue taintIssue,
    Map<ISonarLintProject, EclipseProjectBinding> bindingsPerProjects) {
    try {
      var marker = issuable.getResource().createMarker(SonarLintCorePlugin.MARKER_TAINT_ID);

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
    var existingAttributes = marker.getAttributes();

    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, trackable.getRuleKey());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RULE_NAME_ATTR, trackable.getRuleName());
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.SEVERITY, SonarLintGlobalConfiguration.getMarkerSeverity());

    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.MESSAGE, trackable.getMessage());

    // File level issues (line == null) are displayed on line 1
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.LINE_NUMBER, trackable.getLine() != null ? trackable.getLine() : 1);

    var position = MarkerUtils.getPosition(document, trackable.getTextRange());
    if (position != null) {
      setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.CHAR_START, position.getOffset());
      setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.CHAR_END, position.getOffset() + position.getLength());
    }

    updateServerMarkerAttributes(trackable, marker);
  }

  private static void createFlowMarkersForLocalIssues(IDocument document, ISonarLintIssuable issuable, Trackable trackable, IMarker marker, TriggerType triggerType)
    throws CoreException {
    var flowMarkerId = markerIdForFlows(triggerType);
    var flows = new ArrayList<MarkerFlow>();
    var i = 1;
    for (var engineFlow : trackable.getFlows()) {
      var flow = new MarkerFlow(i);
      flows.add(flow);
      var locations = new ArrayList<>(engineFlow.locations());
      Collections.reverse(locations);
      for (var l : locations) {
        var flowLocation = new MarkerFlowLocation(flow, l.getMessage());
        createMarkerForTextRange(document, issuable.getResource(), flowMarkerId, l.getMessage(), l.getTextRange()).ifPresent(flowLocation::setMarker);
      }
      i++;
    }
    marker.setAttribute(MarkerUtils.SONAR_MARKER_EXTRA_LOCATIONS_ATTR, new MarkerFlows(flows));
  }

  private static void createQuickFixMarkersForLocalIssues(IDocument document, ISonarLintIssuable issuable, Trackable trackable, IMarker marker, TriggerType triggerType)
    throws CoreException {
    if (!triggerType.isOnTheFly()) {
      return;
    }
    var qfs = new ArrayList<MarkerQuickFix>();
    for (var engineQuickFix : trackable.getQuickFix()) {
      createQuickFix(document, issuable, qfs, engineQuickFix);
    }
    marker.setAttribute(MarkerUtils.SONAR_MARKER_QUICK_FIXES_ATTR, new MarkerQuickFixes(qfs));
  }

  private static void createQuickFix(IDocument document, ISonarLintIssuable issuable, List<MarkerQuickFix> qfs, QuickFix engineQuickFix) {
    var qf = new MarkerQuickFix(engineQuickFix.message());
    for (var edits : engineQuickFix.inputFileEdits()) {
      var inputFile = (EclipseInputFile) edits.target();
      if (!issuable.equals(inputFile.getClientObject())) {
        SonarLintLogger.get().debug("Quick fix on multiple files is not supported yet: " + engineQuickFix.message());
        return;
      }
      if (inputFile.hasDocumentOlderThan(document)) {
        SonarLintLogger.get().debug("Document has changed since quick fix was contributed: " + engineQuickFix.message());
        return;
      }
      for (var txtEditFromEngine : edits.textEdits()) {
        var markerForTextEdit = createMarkerForTextRange(document, issuable.getResource(), SonarLintCorePlugin.MARKER_ON_THE_FLY_QUICK_FIX_ID, null,
          txtEditFromEngine.range());
        if (markerForTextEdit.isPresent()) {
          var textEdit = new MarkerTextEdit(markerForTextEdit.get(), txtEditFromEngine.newText());
          qf.addTextEdit(textEdit);
        } else {
          SonarLintLogger.get().debug("Unable to create text edit marker for quick fix: " + engineQuickFix.message());
          return;
        }
      }
    }
    qfs.add(qf);
  }

  private static Optional<IMarker> createMarkerForTextRange(IDocument document, IResource resource, String markerId, @Nullable String message,
    org.sonarsource.sonarlint.core.client.api.common.@Nullable TextRange textRange) {
    try {
      var marker = resource.createMarker(markerId);
      if (message != null) {
        marker.setAttribute(IMarker.MESSAGE, message);
      }
      if (textRange == null) {
        // File level
        marker.setAttribute(IMarker.LINE_NUMBER, 1);
      } else {
        marker.setAttribute(IMarker.LINE_NUMBER, textRange.getStartLine());
        var position = MarkerUtils.getPosition(document,
          TextRange.get(textRange.getStartLine(), textRange.getStartLineOffset(), textRange.getEndLine(), textRange.getEndLineOffset()));
        if (position != null) {
          marker.setAttribute(IMarker.CHAR_START, position.getOffset());
          marker.setAttribute(IMarker.CHAR_END, position.getOffset() + position.getLength());
        }
      }
      return Optional.of(marker);
    } catch (Exception e) {
      SonarLintLogger.get().debug("Unable to create marker", e);
      return Optional.empty();
    }
  }

  private static void createFlowMarkersForTaint(ServerIssue taintIssue, IMarker primaryLocationMarker, Map<ISonarLintProject, EclipseProjectBinding> bindingsPerProjects)
    throws CoreException {
    var flows = new ArrayList<MarkerFlow>();
    var i = 1;
    for (var engineFlow : taintIssue.getFlows()) {
      var flow = new MarkerFlow(i);
      flows.add(flow);
      var locations = new ArrayList<>(engineFlow.locations());
      Collections.reverse(locations);
      for (var l : locations) {
        var flowLocation = new MarkerFlowLocation(flow, l.getMessage(), l.getFilePath());

        var locationFile = findFileForLocationInBoundProjects(bindingsPerProjects, l.getFilePath());
        if (locationFile.isEmpty()) {
          continue;
        }
        var file = locationFile.get();
        try {
          var marker = createMarkerIfCodeMatches(file, l);
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
    var document = file.getDocument();
    var startOffset = document.getLineOffset(location.getStartLine() - 1) + location.getStartLineOffset();
    var endOffset = document.getLineOffset(location.getEndLine() - 1) + location.getEndLineOffset();
    var inEditorCode = document.get(startOffset, endOffset - startOffset);
    if (inEditorCode.equals(location.getCodeSnippet())) {
      var marker = file.getResource().createMarker(SonarLintCorePlugin.MARKER_TAINT_FLOW_ID);
      marker.setAttribute(IMarker.MESSAGE, location.getMessage());
      marker.setAttribute(IMarker.LINE_NUMBER, location.getStartLine() != null ? location.getStartLine() : 1);
      var flowPosition = MarkerUtils.getPosition(document,
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
    var existingAttributes = marker.getAttributes();

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
