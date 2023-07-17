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
package org.sonarlint.eclipse.core.internal.tracking;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.jobs.AsyncServerMarkerUpdaterJob;
import org.sonarlint.eclipse.core.internal.vcs.VcsService;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.serverconnection.DownloadException;
import org.sonarsource.sonarlint.core.serverconnection.ProjectBinding;
import org.sonarsource.sonarlint.core.serverconnection.issues.ServerIssue;

public class ServerIssueUpdater {

  public static final String PATH_SEPARATOR_PATTERN = Pattern.quote(File.separator);

  private final IssueTrackerRegistry issueTrackerRegistry;

  public ServerIssueUpdater(IssueTrackerRegistry issueTrackerRegistry) {
    this.issueTrackerRegistry = issueTrackerRegistry;
  }

  public void updateAsync(ConnectedEngineFacade engineFacade,
    ISonarLintProject project,
    ProjectBinding projectBinding, Collection<ISonarLintIssuable> issuables, Map<ISonarLintFile, IDocument> docPerFile, TriggerType triggerType) {
    new IssueUpdateJob(engineFacade, project, projectBinding, issuables, docPerFile, triggerType).schedule();
  }

  private class IssueUpdateJob extends Job {
    private final ProjectBinding projectBinding;
    private final Collection<ISonarLintIssuable> issuables;
    private final ISonarLintProject project;
    private final Map<ISonarLintFile, IDocument> docPerFile;
    private final TriggerType triggerType;
    private final ConnectedEngineFacade engineFacade;

    private IssueUpdateJob(ConnectedEngineFacade engineFacade,
      ISonarLintProject project,
      ProjectBinding projectBinding, Collection<ISonarLintIssuable> issuables, Map<ISonarLintFile, IDocument> docPerFile,
      TriggerType triggerType) {
      super("Fetch server issues for " + project.getName());
      this.engineFacade = engineFacade;
      this.docPerFile = docPerFile;
      this.triggerType = triggerType;
      setPriority(DECORATE);
      this.project = project;
      this.projectBinding = projectBinding;
      this.issuables = issuables;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      var trackedIssues = new HashMap<ISonarLintIssuable, Collection<Trackable>>();
      try {
        for (var issuable : issuables) {
          if (monitor.isCanceled()) {
            return Status.CANCEL_STATUS;
          }
          VcsService.getServerBranch(project).ifPresent(b -> {
            if (issuable instanceof ISonarLintFile) {
              var file = ((ISonarLintFile) issuable);
              var issueTracker = issueTrackerRegistry.getOrCreate(project);
              var serverIssues = fetchServerIssues(engineFacade, projectBinding, b, (ISonarLintFile) issuable, monitor);
              Collection<Trackable> serverIssuesTrackable = serverIssues.stream().map(ServerIssueTrackable::new).collect(Collectors.toList());
              var tracked = issueTracker.matchAndTrackServerIssues(file, serverIssuesTrackable);
              issueTracker.updateCache(file, tracked);
              trackedIssues.put(issuable, tracked);
            }
          });
        }
        if (!trackedIssues.isEmpty()) {
          new AsyncServerMarkerUpdaterJob(project, trackedIssues, docPerFile, triggerType).schedule();
        }
        return Status.OK_STATUS;
      } catch (Throwable t) {
        // note: without catching Throwable, any exceptions raised in the thread will not be visible
        SonarLintLogger.get().error("Error while fetching and matching server issues", t);
        return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, t.getMessage());
      }
    }

  }

  public static List<ServerIssue> fetchServerIssues(ConnectedEngineFacade engineFacade,
    ProjectBinding projectBinding,
    String branchName,
    ISonarLintFile file, IProgressMonitor monitor) {
    var filePath = file.getProjectRelativePath();

    try {
      SonarLintLogger.get().debug("Download server issues for " + file.getName());
      return engineFacade.downloadAllServerIssuesForFile(projectBinding, branchName, filePath, monitor);
    } catch (DownloadException e) {
      SonarLintLogger.get().info(e.getMessage());
      return engineFacade.getServerIssues(projectBinding, branchName, filePath);
    }
  }

}
