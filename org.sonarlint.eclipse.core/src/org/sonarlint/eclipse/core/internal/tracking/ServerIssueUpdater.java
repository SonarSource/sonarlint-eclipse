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
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.jobs.AsyncServerMarkerUpdaterJob;
import org.sonarlint.eclipse.core.internal.vcs.VcsService;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.ClientTrackedIssueDto;
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.LineWithHashDto;
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.LocalOnlyIssueDto;
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.ServerMatchedIssueDto;
import org.sonarsource.sonarlint.core.clientapi.backend.tracking.TextRangeWithHashDto;
import org.sonarsource.sonarlint.core.serverconnection.DownloadException;
import org.sonarsource.sonarlint.core.serverconnection.IssueStorePaths;
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
  
  private static ClientTrackedIssueDto convertFromTrackable(Trackable issue) {
    var textRange = issue.getTextRange();
    
    // TODO: Get UUID
    return new ClientTrackedIssueDto(null, issue.getServerIssueKey(),
      new TextRangeWithHashDto(textRange.getStartLine(), textRange.getStartLineOffset(), textRange.getEndLine(), textRange.getEndLineOffset(), issue.getTextRangeHash()),
      new LineWithHashDto(textRange.getStartLine(), issue.getLineHash()), issue.getRuleKey(), issue.getMessage());
  }
  
  private static Trackable convertFromServerTrackedIssues(Either<ServerMatchedIssueDto, LocalOnlyIssueDto> serverTrackedIssues) {
    
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
      try {
        var issues = new HashMap<String, List<ClientTrackedIssueDto>>();
        var issueTracker = issueTrackerRegistry.getOrCreate(project);
        
        for (var issuable : issuables) {
          if (issuable instanceof ISonarLintFile) {
            var file = ((ISonarLintFile) issuable);
            
            var serverRelativePath = IssueStorePaths.idePathToServerPath(projectBinding, file.getProjectRelativePath());
            var localIssuesTracked = issueTracker.getFromLocalCache(file);
            issues.put(serverRelativePath, localIssuesTracked.stream().map(ServerIssueUpdater::convertFromTrackable).collect(Collectors.toList()));
          }
        }
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        
        try {
          var response = SonarLintBackendService.get().trackWithServerIssues(project, issues, triggerType.shouldUpdate()).get();
          var trackedIssues = new HashMap<ISonarLintIssuable, Collection<Trackable>>();
          
          response.getIssuesByServerRelativePath().forEach((serverPath, serverTrackedIssues) -> {
            projectBinding.serverPathToIdePath(serverPath).flatMap(project::find).ifPresent(slfile -> {
              var listOfTrackedIssues = serverTrackedIssues.stream().map(ServerIssueUpdater::convertFromServerTrackedIssues).collect(Collectors.toList());
              
              issueTracker.updateCache(slfile, listOfTrackedIssues);
              trackedIssues.put(slfile, listOfTrackedIssues);
            });
          });
            
          if (!trackedIssues.isEmpty()) {
            new AsyncServerMarkerUpdaterJob(project, trackedIssues, docPerFile, triggerType).schedule();
          }
        } catch (InterruptedException | ExecutionException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
        
        return Status.OK_STATUS;
      } catch (Throwable t) {
        // note: without catching Throwable, any exceptions raised in the thread will not be visible
        SonarLintLogger.get().error("Error while fetching and matching server issues", t);
        return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, t.getMessage());
      }
    }

  }
}
