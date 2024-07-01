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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlow;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlowLocation;
import org.sonarlint.eclipse.core.internal.markers.MarkerFlows;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFix;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerQuickFixes;
import org.sonarlint.eclipse.core.internal.quickfixes.MarkerTextEdit;
import org.sonarlint.eclipse.core.internal.utils.DigestUtils;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.listener.TaintVulnerabilitiesListener;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.TaintVulnerabilityDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.TaintVulnerabilityDto.FlowDto.LocationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.TextRangeWithHashDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.QuickFixDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.RaisedIssueDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.TextRangeDto;

public class SonarLintMarkerUpdater {

  private static TaintVulnerabilitiesListener taintVulnerabilitiesListener;

  private SonarLintMarkerUpdater() {
  }

  public static void setTaintVulnerabilitiesListener(TaintVulnerabilitiesListener listener) {
    taintVulnerabilitiesListener = listener;
  }

  public static void deleteAllMarkersFromReport() {
    SonarLintUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen)
      .forEach(p -> {
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_REPORT_ID);
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_REPORT_FLOW_ID);
      });
  }

  public static void deleteAllMarkersFromTaint() {
    SonarLintUtils.allProjects().stream()
      .filter(ISonarLintProject::isOpen)
      .forEach(p -> {
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_TAINT_ID);
        p.deleteAllMarkers(SonarLintCorePlugin.MARKER_TAINT_FLOW_ID);
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

  public static void clearMarkers(ISonarLintFile file) {
    try {
      file.getResource().deleteMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, false, IResource.DEPTH_ZERO);
      file.getResource().deleteMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_FLOW_ID, false, IResource.DEPTH_ZERO);
      file.getResource().deleteMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_QUICK_FIX_ID, false, IResource.DEPTH_ZERO);
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  public static void createOrUpdateMarkers(ISonarLintFile file, List<RaisedIssueDto> issues, boolean issuesAreOnTheFly, final boolean issuesIncludingResolved,
    final boolean issuesOnlyNewCode, final boolean viableForStatusChange) {

    try {
      var markersForFile = Stream.of(file.getResource().findMarkers(
        issuesAreOnTheFly ? SonarLintCorePlugin.MARKER_ON_THE_FLY_ID : SonarLintCorePlugin.MARKER_REPORT_ID,
        false,
        IResource.DEPTH_ZERO))
        .collect(Collectors.toMap(MarkerUtils::getTrackedIssueId, marker -> marker));

      var issueIds = issues.stream().map(issue -> issue.getId()).collect(Collectors.toSet());

      // All markers that have no associated issue (anymore)
      // -> markers not to be shown based on New Code / Issue Filter will be determined one by one and deleted directly
      Set<IMarker> previousMarkersToDelete = markersForFile.entrySet().stream()
        .filter(entry -> !issueIds.contains(entry.getKey()))
        .map(Entry::getValue)
        .collect(Collectors.toSet());

      if (!issues.isEmpty()) {
        createOrUpdateMarkers(file, markersForFile, issues, issuesAreOnTheFly, issuesIncludingResolved, issuesOnlyNewCode, viableForStatusChange);
      }

      for (var marker : previousMarkersToDelete) {
        marker.delete();
      }
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  public static void refreshMarkersForTaint(ISonarLintFile currentFile, ConnectionFacade facade, final boolean issuesIncludingResolved,
    final boolean issuesOnlyNewCode, IProgressMonitor monitor) throws InterruptedException, ExecutionException {
    deleteTaintMarkers(currentFile);

    var project = currentFile.getProject();
    var config = SonarLintCorePlugin.loadConfig(project);
    var projectBinding = config.getProjectBinding();
    if (projectBinding.isEmpty()) {
      return;
    }
    var binding = projectBinding.get();

    var future = SonarLintBackendService.get().listAllTaintVulnerabilities(project);
    var response = JobUtils.waitForFuture(monitor, future);

    var taintVulnerabilities = response.getTaintVulnerabilities();

    var boundSiblingProjects = facade.getBoundProjects(binding.getProjectKey());
    var bindings = boundSiblingProjects.stream()
      .collect(Collectors.toMap(p -> p, p -> SonarLintCorePlugin.loadConfig(p).getProjectBinding().get()));

    var actualTaintMarkersCreated = false;
    for (var taintIssue : taintVulnerabilities) {
      if (!(shouldHideResolvedTaintMarker(taintIssue, issuesIncludingResolved)
        || shouldHidePreNewCodeTaintMarker(taintIssue, issuesOnlyNewCode))) {
        var optFileForTaint = findFileForLocationInBoundProjects(bindings, taintIssue.getIdeFilePath());
        if (optFileForTaint.isEmpty()) {
          continue;
        }
        var fileForTaint = optFileForTaint.get();
        if (!fileForTaint.equals(currentFile)) {
          continue;
        }

        createTaintMarker(fileForTaint.getDocument(), fileForTaint, taintIssue, bindings);
        actualTaintMarkersCreated = true;
      }
    }
    if (actualTaintMarkersCreated && taintVulnerabilitiesListener != null) {
      taintVulnerabilitiesListener.markersCreated(facade.isSonarCloud());
    }

  }

  private static Optional<ISonarLintFile> findFileForLocationInBoundProjects(Map<ISonarLintProject, EclipseProjectBinding> bindingsPerProjects, Path filePath) {
    for (var entry : bindingsPerProjects.entrySet()) {
      var primaryLocationFile = entry.getKey().find(filePath.toString());
      if (primaryLocationFile.isPresent()) {
        return primaryLocationFile;
      }
    }
    return Optional.empty();
  }

  private static void createOrUpdateMarkers(ISonarLintFile file, Map<UUID, IMarker> markersForFile,
    List<RaisedIssueDto> issues, boolean issuesAreOnTheFly, final boolean issuesIncludingResolved,
    final boolean issuesOnlyNewCode, final boolean viableForStatusChange) throws CoreException {

    var lazyInitDocument = file.getDocument();

    for (var issue : issues) {
      var issueId = issue.getId();
      var markerForIssue = markersForFile.get(issueId);

      // Delete all issue markers that
      if (shouldHideResolvedIssueMarker(issue, issuesIncludingResolved)
        || shouldHidePreNewCodeIssueMarker(issue, issuesOnlyNewCode)) {
        if (markerForIssue != null) {
          markerForIssue.delete();
        }
        continue;
      }

      // try to update the marker (if possible), otherwise create it
      if (markerForIssue == null) {
        markerForIssue = createMarker(issueId, file, issuesAreOnTheFly);
      }

      updateMarkerAttributes(lazyInitDocument, issue, markerForIssue, viableForStatusChange);
      createFlowMarkersForLocalIssues(lazyInitDocument, file, issue, markerForIssue, issuesAreOnTheFly);
      createQuickFixMarkersForLocalIssues(lazyInitDocument, file, issue, markerForIssue, issuesAreOnTheFly);
    }
  }

  private static IMarker createMarker(UUID id, ISonarLintIssuable issuable, boolean issuesAreOnTheFly) throws CoreException {
    var marker = issuable.getResource()
      .createMarker(issuesAreOnTheFly ? SonarLintCorePlugin.MARKER_ON_THE_FLY_ID : SonarLintCorePlugin.MARKER_REPORT_ID);
    marker.setAttribute(MarkerUtils.SONAR_MARKER_TRACKED_ISSUE_ID_ATTR, MarkerUtils.encodeUuid(id));
    return marker;
  }

  private static String markerIdForFlows(boolean issuesAreOnTheFly) {
    return issuesAreOnTheFly ? SonarLintCorePlugin.MARKER_ON_THE_FLY_FLOW_ID : SonarLintCorePlugin.MARKER_REPORT_FLOW_ID;
  }

  private static void createTaintMarker(IDocument document, ISonarLintIssuable issuable, TaintVulnerabilityDto taintIssue,
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
        SonarLintLogger.get().debug("Position cannot be set for taint issue '" + taintIssue.getId() + "' in '" + taintIssue.getIdeFilePath() + "'");
      }

      marker.setAttribute(IMarker.PRIORITY, getPriority(taintIssue.getSeverity()));
      marker.setAttribute(MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR, taintIssue.getSeverity().name());
      marker.setAttribute(MarkerUtils.SONAR_MARKER_ISSUE_TYPE_ATTR, taintIssue.getType().name());
      marker.setAttribute(MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR, taintIssue.getSonarServerKey());
      marker.setAttribute(MarkerUtils.SONAR_MARKER_RESOLVED_ATTR, taintIssue.isResolved());

      var creationDate = taintIssue.getIntroductionDate().toEpochMilli();
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

  private static void updateMarkerAttributes(IDocument document, RaisedIssueDto issue, IMarker marker,
    final boolean viableForStatusChange) throws CoreException {
    var existingAttributes = marker.getAttributes();

    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, issue.getRuleKey());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RULE_DESC_CONTEXT_KEY_ATTR,
      issue.getRuleDescriptionContextKey());
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.SEVERITY, SonarLintGlobalConfiguration.getMarkerSeverity());
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.PRIORITY, getPriority(issue.getSeverity()));

    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.MESSAGE, issue.getPrimaryMessage());

    var textRange = issue.getTextRange();
    var position = MarkerUtils.getPosition(document, textRange);

    // File level issues (line == null) are displayed on line 1
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.LINE_NUMBER, textRange != null ? textRange.getStartLine() : 1);

    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.CHAR_START, position != null ? position.getOffset() : null);
    setMarkerAttributeIfDifferent(marker, existingAttributes, IMarker.CHAR_END, position != null ? (position.getOffset() + position.getLength()) : null);

    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_SEVERITY_ATTR,
      issue.getSeverity());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_TYPE_ATTR,
      issue.getType());

    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_ATTRIBUTE_ATTR,
      issue.getCleanCodeAttribute());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_IMPACTS_ATTR,
      MarkerUtils.encodeImpacts(issue.getImpacts()));
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ISSUE_HIGHEST_IMPACT_ATTR,
      MarkerUtils.encodeHighestImpact(issue.getImpacts()));

    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_SERVER_ISSUE_KEY_ATTR,
      issue.getServerKey());
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_ANTICIPATED_ISSUE_ATTR,
      viableForStatusChange);
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_RESOLVED_ATTR,
      issue.isResolved());

    var introductionDate = issue.getIntroductionDate().toEpochMilli();
    setMarkerAttributeIfDifferent(marker, existingAttributes, MarkerUtils.SONAR_MARKER_CREATION_DATE_ATTR,
      String.valueOf(introductionDate));
  }

  private static void createFlowMarkersForLocalIssues(IDocument document, ISonarLintIssuable issuable, RaisedIssueDto issue, IMarker marker, boolean issuesAreOnTheFly)
    throws CoreException {
    var flowMarkerId = markerIdForFlows(issuesAreOnTheFly);
    var flows = new ArrayList<MarkerFlow>();
    var i = 1;
    for (var engineFlow : issue.getFlows()) {
      var flow = new MarkerFlow(i);
      flows.add(flow);
      var locations = new ArrayList<>(engineFlow.getLocations());
      Collections.reverse(locations);
      for (var l : locations) {
        var flowLocation = new MarkerFlowLocation(flow, l.getMessage());
        createMarkerForTextRange(document, issuable.getResource(), flowMarkerId, l.getMessage(), l.getTextRange()).ifPresent(flowLocation::setMarker);
      }
      i++;
    }
    marker.setAttribute(MarkerUtils.SONAR_MARKER_EXTRA_LOCATIONS_ATTR, new MarkerFlows(flows));
  }

  private static void createQuickFixMarkersForLocalIssues(IDocument document, ISonarLintIssuable issuable, RaisedIssueDto issue, IMarker marker, boolean issuesAreOnTheFly)
    throws CoreException {
    if (!issuesAreOnTheFly) {
      return;
    }
    var qfs = new ArrayList<MarkerQuickFix>();
    for (var engineQuickFix : issue.getQuickFixes()) {
      createQuickFix(document, issuable, qfs, engineQuickFix);
    }
    marker.setAttribute(MarkerUtils.SONAR_MARKER_QUICK_FIXES_ATTR, new MarkerQuickFixes(qfs));
  }

  private static void createQuickFix(IDocument document, ISonarLintIssuable issuable, List<MarkerQuickFix> qfs, QuickFixDto rpcQuickFix) {
    var qf = new MarkerQuickFix(rpcQuickFix.message());
    for (var edits : rpcQuickFix.fileEdits()) {
      var fileWithEdit = SonarLintUtils.findFileFromUri(edits.target());
      if (!issuable.equals(fileWithEdit)) {
        SonarLintLogger.get().debug("Quick fix on multiple files is not supported yet: " + rpcQuickFix.message());
        return;
      }
      // should we discard the quick fix if the document has changed since the analysis?
      for (var txtEditFromEngine : edits.textEdits()) {
        var markerForTextEdit = createMarkerForTextRange(document, issuable.getResource(), SonarLintCorePlugin.MARKER_ON_THE_FLY_QUICK_FIX_ID, null,
          txtEditFromEngine.range());
        if (markerForTextEdit.isPresent()) {
          var textEdit = new MarkerTextEdit(markerForTextEdit.get(), txtEditFromEngine.newText());
          qf.addTextEdit(textEdit);
        } else {
          SonarLintLogger.get().debug("Unable to create text edit marker for quick fix: " + rpcQuickFix.message());
          return;
        }
      }
    }
    qfs.add(qf);
  }

  private static Optional<IMarker> createMarkerForTextRange(IDocument document, IResource resource, String markerId, @Nullable String message, @Nullable TextRangeDto textRange) {
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

  private static void createFlowMarkersForTaint(TaintVulnerabilityDto taintIssue, IMarker primaryLocationMarker, Map<ISonarLintProject, EclipseProjectBinding> bindingsPerProjects)
    throws CoreException {
    var flows = new ArrayList<MarkerFlow>();
    var i = 1;
    for (var rpcFlow : taintIssue.getFlows()) {
      var flow = new MarkerFlow(i);
      flows.add(flow);
      var locations = new ArrayList<>(rpcFlow.getLocations());
      Collections.reverse(locations);
      for (var l : locations) {
        var filePath = l.getFilePath();
        if (filePath == null) {
          // Can we really have project level locations?
          continue;
        }
        var flowLocation = new MarkerFlowLocation(flow, l.getMessage(), filePath);

        var locationFile = findFileForLocationInBoundProjects(bindingsPerProjects, filePath);
        if (locationFile.isEmpty()) {
          continue;
        }
        var file = locationFile.get();
        try {
          var marker = createMarker(file, l);
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
  private static IMarker createMarker(ISonarLintFile file, LocationDto l) throws BadLocationException, CoreException {
    var document = file.getDocument();
    var textRange = l.getTextRange();
    if (textRange != null) {
      return createMarkerIfCodeMatches(file, document, textRange, l);
    } else {
      return createFileLevelMarker(file, l);
    }
  }

  private static IMarker createFileLevelMarker(ISonarLintFile file, LocationDto l) throws CoreException {
    var marker = file.getResource().createMarker(SonarLintCorePlugin.MARKER_TAINT_FLOW_ID);
    marker.setAttribute(IMarker.MESSAGE, l.getMessage());
    marker.setAttribute(IMarker.LINE_NUMBER, 1);
    return marker;
  }

  @Nullable
  private static IMarker createMarkerIfCodeMatches(ISonarLintFile file, IDocument document, TextRangeWithHashDto textRange, LocationDto l)
    throws BadLocationException, CoreException {
    var startOffset = document.getLineOffset(textRange.getStartLine() - 1) + textRange.getStartLineOffset();
    var endOffset = document.getLineOffset(textRange.getEndLine() - 1) + textRange.getEndLineOffset();
    var inEditorCode = document.get(startOffset, endOffset - startOffset);
    var inEditorDigest = DigestUtils.digest(inEditorCode);
    if (inEditorDigest.equals(textRange.getHash())) {
      var marker = file.getResource().createMarker(SonarLintCorePlugin.MARKER_TAINT_FLOW_ID);
      marker.setAttribute(IMarker.MESSAGE, l.getMessage());
      marker.setAttribute(IMarker.LINE_NUMBER, textRange.getStartLine());
      var flowPosition = MarkerUtils.getPosition(document, textRange);
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
  private static int getPriority(final org.sonarsource.sonarlint.core.rpc.protocol.common.@Nullable IssueSeverity severity) {
    switch (severity != null ? severity : org.sonarsource.sonarlint.core.rpc.protocol.common.IssueSeverity.INFO) {
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

  /** Markers should not be set / should be removed for issues already resolved when preference is set */
  private static boolean shouldHideResolvedIssueMarker(RaisedIssueDto issue, final boolean issuesIncludingResolved) {
    return !issuesIncludingResolved && issue.isResolved();
  }

  /**
   *  Markers should not be set / should be removed for issues not on new code when preference is set. Of course
   *  markers should stay in standalone mode because the preference is only applied in connected mode!
   */
  private static boolean shouldHidePreNewCodeIssueMarker(RaisedIssueDto issue, final boolean issuesOnlyNewCode) {
    return issuesOnlyNewCode && !issue.isOnNewCode();
  }

  /** Taint marker should not be set / should be removed for issues already resolved when preference is set */
  private static boolean shouldHideResolvedTaintMarker(TaintVulnerabilityDto issue, final boolean issuesIncludingResolved) {
    return !issuesIncludingResolved && issue.isResolved();
  }

  /** Taint markers should not be set / should be removed for issues not on new code when preference is set! */
  private static boolean shouldHidePreNewCodeTaintMarker(TaintVulnerabilityDto issue, final boolean issuesOnlyNewCode) {
    return issuesOnlyNewCode && !issue.isOnNewCode();
  }
}
