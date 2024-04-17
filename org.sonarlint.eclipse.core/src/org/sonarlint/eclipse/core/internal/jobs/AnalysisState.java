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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.protocol.client.analysis.RawIssueDto;

public class AnalysisState {
  private final UUID id;
  private final ISonarLintProject project;
  private final Map<ISonarLintIssuable, List<RawIssueDto>> issuesPerResource;
  private long issueCount = 0;

  public AnalysisState(UUID analysisId, ISonarLintProject project, Map<ISonarLintIssuable, List<RawIssueDto>> issuesPerResource) {
    this.id = analysisId;
    this.project = project;
    this.issuesPerResource = issuesPerResource;
  }

  public UUID getId() {
    return id;
  }

  public void addRawIssue(RawIssueDto rawIssue) {
    issueCount++;
    var fileUri = rawIssue.getFileUri();
    var issuable = fileUri == null ? project : SonarLintUtils.findFileFromUri(fileUri);
    if (issuable == null) {
      SonarLintLogger.get().error("Cannot retrieve the file on which an issue has been raised. File URI is " + fileUri);
      return;
    }
    issuesPerResource.computeIfAbsent(issuable, k -> new ArrayList<>()).add(rawIssue);
  }

  public long getIssueCount() {
    return issueCount;
  }
}
