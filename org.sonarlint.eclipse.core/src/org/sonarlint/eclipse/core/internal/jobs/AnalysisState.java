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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.RaisedIssueDto;

/**
 *  Hey JB, this has to be reworked, let me explain a bit how we can link the raiseIssues(...) with
 *  "isIntermediatePublication" back to the AnalysisProjectJob (see comments in that file):
 *
 *  - each "analyzeFilesAndTrack" is called on only one configurationScopeId, therefore the huge map can be simplified
 *  - when the IssueMarkerUpdaterJob is finished we somehow should save the information (with a boolean) flag that the
 *    markers are done for the AnalysisProjectJob
 *
 *  Other issues to solve:
 *  - this whole construct currently wouldn't work if you select two projects and hit "Analyze" on them as it will
 *    create two different AnalyzeProjectJob's and therefore two different UUIDs and we delete all markers from the
 *    Report view when running IssuesMarkerUpdaterJob (I think) :(
 *  - connecting two or more UUIDs sounds overly complicated, let's don't do that even if they belong to the same
 *    action invoked by the user (e.g. selecting more than one project and hitting "Analyze")
 */
public class AnalysisState {
  private final UUID id;
  private final TriggerType triggerType;
  private Map<String, Map<URI, List<RaisedIssueDto>>> issuesByFileUriForConfigScopeId = new ConcurrentHashMap<>();
  private boolean isIntermediatePublication;

  public AnalysisState(UUID analysisId, TriggerType triggerType) {
    this.id = analysisId;
    this.triggerType = triggerType;
  }

  public UUID getId() {
    return id;
  }

  public void setRaisedIssues(String configScopeId, Map<URI, List<RaisedIssueDto>> issuesByFileUri, boolean isIntermediatePublication) {
    this.issuesByFileUriForConfigScopeId.put(configScopeId, issuesByFileUri);
    this.isIntermediatePublication = isIntermediatePublication;
  }

  public TriggerType getTriggerType() {
    return triggerType;
  }

  public long getIssueCount(String configScopeId) {
    return issuesByFileUriForConfigScopeId.getOrDefault(configScopeId, Map.of())
      .values().stream().map(l -> l.size()).reduce((a, b) -> a + b).orElse(0);
  }

  public Map<URI, List<RaisedIssueDto>> getIssuesByFileUri(String configScopeId) {
    return issuesByFileUriForConfigScopeId.getOrDefault(configScopeId, Map.of());
  }

  public boolean isIntermediatePublication() {
    return isIntermediatePublication;
  }
}
