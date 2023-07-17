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
package org.sonarlint.eclipse.ui.internal;

import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.IServerEventListener;
import org.sonarlint.eclipse.core.internal.jobs.TaintIssuesUpdateAfterSyncJob;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarsource.sonarlint.core.serverapi.push.IssueChangedEvent;
import org.sonarsource.sonarlint.core.serverapi.push.ServerEvent;
import org.sonarsource.sonarlint.core.serverapi.push.TaintVulnerabilityClosedEvent;
import org.sonarsource.sonarlint.core.serverapi.push.TaintVulnerabilityRaisedEvent;

public class SonarLintServerEventListener implements IServerEventListener {

  @Override
  public void eventReceived(ConnectedEngineFacade facade, ServerEvent event) {
    // FIXME Very inefficient implementation. Should be acceptable as we don't expect to have too many taint vulnerabilities
    if (event instanceof TaintVulnerabilityClosedEvent) {
      var projectKey = ((TaintVulnerabilityClosedEvent) event).getProjectKey();
      refreshTaintVulnerabilitiesForProjectsBoundToProjectKey(facade, projectKey);
    } else if (event instanceof TaintVulnerabilityRaisedEvent) {
      var projectKey = ((TaintVulnerabilityRaisedEvent) event).getProjectKey();
      refreshTaintVulnerabilitiesForProjectsBoundToProjectKey(facade, projectKey);
    } else if (event instanceof IssueChangedEvent) {
      var issueChangedEvent = (IssueChangedEvent) event;
      var projectKey = issueChangedEvent.getProjectKey();
      refreshTaintVulnerabilitiesForProjectsBoundToProjectKey(facade, projectKey);
    }
  }

  private static void refreshTaintVulnerabilitiesForProjectsBoundToProjectKey(ConnectedEngineFacade facade, String sonarProjectKey) {
    doWithAffectedProjects(facade, sonarProjectKey, p -> {
      var openedFiles = PlatformUtils.collectOpenedFiles(p, f -> true);
      var files = openedFiles.get(p).stream()
        .map(file -> file.getFile())
        .collect(Collectors.toList());
      new TaintIssuesUpdateAfterSyncJob(facade, p, files).schedule();
    });
  }

  private static void doWithAffectedProjects(ConnectedEngineFacade facade, String sonarProjectKey, Consumer<ISonarLintProject> consumer) {
    var possiblyAffectedProjects = facade.getBoundProjects(sonarProjectKey);
    possiblyAffectedProjects.forEach(consumer::accept);
  }

}
