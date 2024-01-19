/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFix;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFixes;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerTextEdit;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.tracking.DigestUtils;
import org.sonarlint.eclipse.core.internal.tracking.TrackedIssue;
import org.sonarlint.eclipse.core.listener.TaintVulnerabilitiesListener;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.analysis.api.QuickFix;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.serverconnection.issues.ServerTaintIssue;
import org.sonarsource.sonarlint.core.serverconnection.issues.ServerTaintIssue.ServerIssueLocation;

public class SonarLintMarkerUpdater {

  private static TaintVulnerabilitiesListener taintVulnerabilitiesListener;

  private SonarLintMarkerUpdater() {
  }

  public static void setTaintVulnerabilitiesListener(TaintVulnerabilitiesListener listener) {
    taintVulnerabilitiesListener = listener;
  }

  public static void createOrUpdateMarkers(ISonarLintFile file, Optional<IDocument> openedDocument,
    Collection<? extends TrackedIssue> issues, TriggerType triggerType, final String issuePeriodPreference,
    final String issueFilterPreference, final boolean viableForStatusChange) {
    try {
      Set<IMarker> previousMarkersToDelete;
      if (triggerType.isOnTheFly()) {
        previousMarkersToDelete = new HashSet<>(List.of(file.getResource().findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, false, IResource.DEPTH_ZERO)));
      } else {
        previousMarkersToDelete = Collections.emptySet();
      }

      createOrUpdateMarkers(file, openedDocument, issues, triggerType, previousMarkersToDelete, issuePeriodPreference,
        issueFilterPreference, viableForStatusChange);

      for (var marker : previousMarkersToDelete) {
        marker.delete();
      }
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  public static void refreshMarkersForTaint(ISonarLintFile currentFile, String branchName,
    ConnectedEngineFacade facade, final String issuePeriodPreference, final String issueFilterPreference) {
    deleteTaintMarkers(currentFile);

    var project = currentFile.getProject();
    var config = SonarLintCorePlugin.loadConfig(project);
    config.getProjectBinding().ifPresent(binding -> {
      var includeResolvedByPreference = Objects.equals(
        SonarLintGlobalConfiguration.PREF_ISSUE_DISPLAY_FILTER_ALL, issueFilterPreference);
      
      var taintVulnerabilities = facade.getServerTaintIssues(binding, branchName,
        currentFile.getProjectRelativePath(), includeResolvedByPreference)
        .stream()
        .collect(Collectors.toList());

      var boundSiblingProjects = facade.getBoundProjects(binding.projectKey());
      var bindings = boundSiblingProjects.stream()
        .collect(Collectors.toMap(p -> p, p -> SonarLintCorePlugin.loadConfig(p).getProjectBinding().get()));

      var actualTaintMarkersCreated = false;
      for (var taintIssue : taintVulnerabilities) {
        if (!(shouldHideResolvedTaintMarker(taintIssue, issueFilterPreference)
          || shouldHidePreNewCodeTaintMarker(taintIssue, issuePeriodPreference))) {
          findFileForLocationInBoundProjects(bindings, taintIssue.getFilePath())
            .ifPresent(primaryLocationFile -> createTaintMarker(primaryLocationFile.getDocument(), primaryLocationFile, taintIssue, bindings));
          actualTaintMarkersCreated = true;
        }
      }
      if (actualTaintMarkersCreated && taintVulnerabilitiesListener != null) {
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

  public static void updateMarkersWithServerSideData(ISonarLintIssuable issuable, IDocument document,
    Collection<TrackedIssue> issues, TriggerType triggerType, final String issuePeriodPreference,
    final String issueFilterPreference, final boolean viableForStatusChange) {
    try {
      for (var issue : issues) {
        updateMarkerWithServerSideData(issuable, document, issue, triggerType, issuePeriodPreference,
          issueFilterPreference, viableForStatusChange);
      }
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  private static void updateMarkerWithServerSideData(ISonarLintIssuable issuable, IDocument document,
    TrackedIssue issue, TriggerType triggerType, final String issuePeriodPreference,
    final String issueFilterPreference, final boolean viableForStatusChange) throws CoreException {
    var markerId = issue.getMarkerId();
    IMarker marker = null;
    if (markerId != null) {
      marker = issuable.getResource().findMarker(markerId);
    }
    if (shouldHideResolvedIssueMarker(issue, issueFilterPreference)
      || shouldHidePreNewCodeIssueMarker(issue, issuePeriodPreference)) {
      if (marker != null) {
        // For makers of issues in connected mode: Issue is associated to a marker, means it was not marked as resolved
        // in previous analysis, but now it is, so clear marker!
        marker.delete();
      }
      issue.setMarkerId(null);
    } else {
      if (marker != null) {
        updateServerMarkerAttributes(issue, marker, viableForStatusChange);
      } else {
        // Issue was previously resolved, and is now reopen, so we need to recreate a marker
        createMarker(document, issuable, issue, triggerType, viableForStatusChange);
      }
    }
  }

  private static void createOrUpdateMarkers(ISonarLintFile file, Optional<IDocument> openedDocument,
    Collection<? extends TrackedIssue> issues, TriggerType triggerType, Set<IMarker> previousMarkersToDelete,
    final String issuePeriodPreference, final String issueFilterPreference,
    final boolean viableForStatusChange) throws CoreException {
    var lazyInitDocument = openedDocument.orElse(null);

    for (var issue : issues) {
      if (shouldHideResolvedIssueMarker(issue, issueFilterPreference)
        || shouldHidePreNewCodeIssueMarker(issue, issuePeriodPreference)) {
        issue.setMarkerId(null);
      } else {
        lazyInitDocument = lazyInitDocument != null ? lazyInitDocument : file.getDocument();
        if (!triggerType.isOnTheFly()
          || issue.getMarkerId() == null
          || file.getResource().findMarker(issue.getMarkerId()) == null) {
          createMarker(lazyInitDocument, file, issue, triggerType, viableForStatusChange);
        } else {
          var marker = file.getResource().findMarker(issue.getMarkerId());
          updateMarkerAttributes(lazyInitDocument, issue, marker, viableForStatusChange);
          createFlowMarkersForLocalIssues(lazyInitDocument, file, issue, marker, triggerType);
          createQuickFixMarkersForLocalIssues(lazyInitDocument, file, issue, marker, triggerType);
          previousMarkersToDelete.remove(marker);
        }
      }
    }
  }

  private static void createMarker(IDocument document, ISonarLintIssuable issuable, TrackedIssue trackable,
    TriggerType triggerType, final boolean viableForStatusChange) throws CoreException {
    var marker = issuable.getResource()
      .createMarker(triggerType.isOnTheFly() ? SonarLintCorePlugin.MARKER_ON_THE_FLY_ID : SonarLintCorePlugin.MARKER_REPORT_ID);
    if (triggerType.isOnTheFly()) {
      trackable.setMarkerId(marker.getId());
    }

    setMarkerViewUtilsAttributes(issuable, marker);

    updateMarkerAttributes(document, trackable, marker, viableForStatusChange);
    createFlowMarkersForLocalIssues(document, issuable, trackable, marker, triggerType);
    createQuickFixMarkersForLocalIssues(document, issuable, trackable, marker, triggerType);

  }

  private static String markerIdForFlows(TriggerType triggerType) {
    return triggerType.isOnTheFly() ? SonarLintCorePlugin.MARKER_ON_THE_FLY_FLOW_ID : SonarLintCorePlugin.MARKER_REPORT_FLOW_ID;
  }

  private static void createTaintMarker(IDocument document, ISonarLintIssuable issuable, ServerTaintIssue taintIssue,
    Map<ISonarLintProject, EclipseProjectBinding> bindingsPerProjects) {
    try {
      var marker = issuable.getResource().createMarker(SonarLintCorePlugin.MARKER_TAINT_ID);

      setMarkerViewUtilsAttributes(issuable, marker);

      marker.setAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, taintIssue.getRuleKey());
      marker.setAttribute(MarkerUtils.SONAR_MARKER_RULE_DESC_CONTEXT_KEY_ATTR, taintIssue.getRuleDescriptionContextKey());
      marker.setAttribute(IMarker.SEVERITY, SonarLintGlobalConfiguration.getMarkerSeverity());

      marker.setAttribute(IMarker.MESSAGE, taintIssue.getMessage());

      // File level issues (line == null) are displayed on line 1
      marker.setAttribute(IMarker.LINE_NUMBER, taintIssue.getTextRange() != null ? taintIssue.getTextRange().getStartLine() : 1);

      var position = MarkerUtils.getPosition(document, taintIssue.getTextRange());
      if (position != null) {
        marker.setAttribute(IMarker.CHAR_START, position.getOffset());
        marker.setAttribute(IMarker.CHAR_END, position.getOffset() + position.getLength());
      } else {
        SonarLintLogger.get().debug("Position cannot be set for taint issue '" + taintIssue.getKey() + "' in '"
          + taintIssue.getFilePath() + "'");
      }

      marker.setAttribute(IMarker.PRIORITY, getPriority(taintIssue.getSeverity()));
      marker.setAttribute(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR, taintIssue.getSeverity().name());
      marker.setAttribute(MarkerUtils.SONAR_MARKER_ISSUE_TYPE_ATTR, taintIssue.getType().name());
      marker.setAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, taintIssue.getKey());
      marker.setAttribute(MarkerUtils.SONAR_MARKER_RESOLVED_ATTR, taintIssue.isResolved());

      var creationDate = taintIssue.getCreationDate().toEpochMilli();
      marker.setAttribute(MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR, String.valueOf(creationDate));

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

  private static void updateMarkerAttributes(IDocument document, TrackedIssue trackedIssue, IMarker marker,
    final boolean viableForStatusChange) throws CoreException {
    var existingAttributes = marker.getAttributes();

    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, trackedIssue.getRuleKey());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RULE_DESC_CONTEXT_KEY_ATTR,
      trackedIssue.getIssueFromAnalysis().getRuleDescriptionContextKey().orElse(null));
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.SEVERITY, SonarLintGlobalConfiguration.getMarkerSeverity());

    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.MESSAGE, trackedIssue.getMessage());

    // File level issues (line == null) are displayed on line 1
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.LINE_NUMBER, trackedIssue.getLine() != null ? trackedIssue.getLine() : 1);

    var position = MarkerUtils.getPosition(document, trackedIssue.getIssueFromAnalysis().getTextRange());
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.CHAR_START, position != null ? position.getOffset() : null);
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.CHAR_END, position != null ? (position.getOffset() + position.getLength()) : null);

    var issueFromAnalysis = trackedIssue.getIssueFromAnalysis();
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_ATTRIBUTE_ATTR,
      issueFromAnalysis.getCleanCodeAttribute().orElse(null));
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_IMPACTS_ATTR,
      MarkerUtils.encodeImpacts(issueFromAnalysis.getImpacts()));
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_HIGHEST_IMPACT_ATTR,
      MarkerUtils.encodeHighestImpact(issueFromAnalysis.getImpacts()));

    updateServerMarkerAttributes(trackedIssue, marker, viableForStatusChange);
  }

  private static void createFlowMarkersForLocalIssues(IDocument document, ISonarLintIssuable issuable, TrackedIssue trackedIssue, IMarker marker, TriggerType triggerType)
    throws CoreException {
    var flowMarkerId = markerIdForFlows(triggerType);
    var flows = new ArrayList<MarkerFlow>();
    var i = 1;
    for (var engineFlow : trackedIssue.getIssueFromAnalysis().flows()) {
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

  private static void createQuickFixMarkersForLocalIssues(IDocument document, ISonarLintIssuable issuable, TrackedIssue trackedIssue, IMarker marker, TriggerType triggerType)
    throws CoreException {
    if (!triggerType.isOnTheFly()) {
      return;
    }
    var qfs = new ArrayList<MarkerQuickFix>();
    for (var engineQuickFix : trackedIssue.getIssueFromAnalysis().quickFixes()) {
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
    org.sonarsource.sonarlint.core.commons.@Nullable TextRange textRange) {
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
        var position = MarkerUtils.getPosition(document, textRange);
        if (position != null) {
          marker.setAttribute(IMarker.CHAR_START, position.getOffset());
          marker.setAttribute(IMarker.CHAR_END, position.getOffset() + position.getLength());
        } else {
          SonarLintLogger.get().debug("Position cannot be set on resource '" + resource.getFullPath() + "'");
        }
      }
      return Optional.of(marker);
    } catch (Exception e) {
      SonarLintLogger.get().debug("Unable to create marker", e);
      return Optional.empty();
    }
  }

  private static void createFlowMarkersForTaint(ServerTaintIssue taintIssue, IMarker primaryLocationMarker, Map<ISonarLintProject, EclipseProjectBinding> bindingsPerProjects)
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
    var startOffset = document.getLineOffset(location.getTextRange().getStartLine() - 1) + location.getTextRange().getStartLineOffset();
    var endOffset = document.getLineOffset(location.getTextRange().getEndLine() - 1) + location.getTextRange().getEndLineOffset();
    var inEditorCode = document.get(startOffset, endOffset - startOffset);
    var inEditorDigest = DigestUtils.digest(inEditorCode);
    if (inEditorDigest.equals(location.getTextRange().getHash())) {
      var marker = file.getResource().createMarker(SonarLintCorePlugin.MARKER_TAINT_FLOW_ID);
      marker.setAttribute(IMarker.MESSAGE, location.getMessage());
      marker.setAttribute(IMarker.LINE_NUMBER, location.getTextRange() != null ? location.getTextRange().getStartLine() : 1);
      var flowPosition = MarkerUtils.getPosition(document, location.getTextRange());
      if (flowPosition != null) {
        marker.setAttribute(IMarker.CHAR_START, flowPosition.getOffset());
        marker.setAttribute(IMarker.CHAR_END, flowPosition.getOffset() + flowPosition.getLength());
      } else {
        SonarLintLogger.get().debug("Position cannot be set for flow on '" + file.getProjectRelativePath() + "'");
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
  private static void updateServerMarkerAttributes(TrackedIssue trackedIssue, IMarker marker,
    final boolean viableForStatusChange) throws CoreException {
    var existingAttributes = marker.getAttributes();

    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.PRIORITY, getPriority(trackedIssue.getSeverity()));
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_TRACKED_ISSUE_ID_ATTR,
      MarkerUtils.encodeUuid(trackedIssue.getId()));
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR,
      trackedIssue.getSeverity());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_TYPE_ATTR,
      trackedIssue.getType());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR,
      trackedIssue.getServerIssueKey());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ANTICIPATED_ISSUE_ATTR,
      viableForStatusChange);
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RESOLVED_ATTR,
      trackedIssue.isResolved());

    Long creationDate = trackedIssue.getCreationDate();
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR,
      creationDate != null ? String.valueOf(creationDate) : null);
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
  private static int getPriority(@Nullable final IssueSeverity severity) {
    switch (severity != null ? severity : IssueSeverity.INFO) {
      case BLOCKER:
      case CRITICAL:
        return IMarker.PRIORITY_HIGH;
      case MAJOR:
        return IMarker.PRIORITY_NORMAL;
      case MINOR:
      case INFO:
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
  
  /** Markers should not be set / should be removed for issues already resolved when preference is set */
  private static boolean shouldHideResolvedIssueMarker(TrackedIssue issue, final String issueFilterPreference) {
    return Objects.equals(SonarLintGlobalConfiguration.PREF_ISSUE_DISPLAY_FILTER_NONRESOLVED, issueFilterPreference)
      && issue.isResolved();
  }

  /**
   *  Markers should not be set / should be removed for issues not on new code when preference is set. Of course
   *  markers should stay in standalone mode because the preference is only applied in connected mode!
   */
  private static boolean shouldHidePreNewCodeIssueMarker(TrackedIssue issue, final String issuePeriodPreference) {
    return issue.getServerIssueKey() != null
      && Objects.equals(SonarLintGlobalConfiguration.PREF_ISSUE_PERIOD_NEWCODE, issuePeriodPreference)
      && !issue.isNewCode();
  }
  
  /** Taint marker should not be set / should be removed for issues already resolved when preference is set */
  private static boolean shouldHideResolvedTaintMarker(ServerTaintIssue issue, final String issueFilterPreference) {
    return Objects.equals(SonarLintGlobalConfiguration.PREF_ISSUE_DISPLAY_FILTER_NONRESOLVED, issueFilterPreference)
      && issue.isResolved();
  }

  /** Taint markers should not be set / should be removed for issues not on new code when preference is set! */
  private static boolean shouldHidePreNewCodeTaintMarker(ServerTaintIssue issue, final String issuePeriodPreference) {
    return Objects.equals(SonarLintGlobalConfiguration.PREF_ISSUE_PERIOD_NEWCODE, issuePeriodPreference)
      && !issue.isOnNewCode();
  }
}
