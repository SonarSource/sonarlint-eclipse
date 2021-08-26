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
import java.util.List;
import java.util.Map;

import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;

public class SonarLintIssueListener implements IssueListener {
  private final Map<ISonarLintIssuable, List<Issue>> issuesPerResource;
  private final ISonarLintProject project;
  private long issueCount = 0;

  public SonarLintIssueListener(ISonarLintProject project, Map<ISonarLintIssuable, List<Issue>> issuesPerResource) {
    this.issuesPerResource = issuesPerResource;
    this.project = project;
  }

  @Override
  public void handle(Issue issue) {
    issueCount++;
    ISonarLintIssuable r;
    ClientInputFile inputFile = issue.getInputFile();
    if (inputFile == null) {
      r = project;
    } else {
      r = inputFile.getClientObject();
    }
    if (!issuesPerResource.containsKey(r)) {
      issuesPerResource.put(r, new ArrayList<Issue>());
    }
    issuesPerResource.get(r).add(issue);
    SonarLintNotifications.get().showNotificationIfFirstSecretDetected(issue);
  }

  public long getIssueCount() {
    return issueCount;
  }
}
