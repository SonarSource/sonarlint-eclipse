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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.RaisedIssueDto;

public class AnalysisState {
  private final UUID id;
  private final TriggerType triggerType;
  private Map<URI, List<RaisedIssueDto>> issuesByFileUri = new HashMap<>();
  private boolean isIntermediatePublication;

  public AnalysisState(UUID analysisId, TriggerType triggerType) {
    this.id = analysisId;
    this.triggerType = triggerType;
  }

  public UUID getId() {
    return id;
  }

  public void setRaisedIssues(Map<URI, List<RaisedIssueDto>> issuesByFileUri, boolean isIntermediatePublication) {
    this.issuesByFileUri = issuesByFileUri;
    this.isIntermediatePublication = isIntermediatePublication;
  }

  public TriggerType getTriggerType() {
    return triggerType;
  }

  public long getIssueCount() {
    return issuesByFileUri.values().stream().map(l -> l.size()).reduce((a, b) -> a + b).orElse(0);
  }

  public Map<URI, List<RaisedIssueDto>> getIssuesByFileUri() {
    return issuesByFileUri;
  }

  public boolean isIntermediatePublication() {
    return isIntermediatePublication;
  }
}
