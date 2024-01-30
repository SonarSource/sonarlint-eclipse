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
package org.sonarlint.eclipse.ui.internal.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.internal.backend.SonarLintEclipseHeadlessClient;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.jobs.TaintIssuesUpdateAfterSyncJob;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;
import org.sonarlint.eclipse.ui.internal.binding.assist.AbstractAssistCreatingConnectionJob;
import org.sonarlint.eclipse.ui.internal.binding.assist.AssistBindingJob;
import org.sonarlint.eclipse.ui.internal.binding.assist.AssistCreatingAutomaticConnectionJob;
import org.sonarlint.eclipse.ui.internal.binding.assist.AssistCreatingManualConnectionJob;
import org.sonarlint.eclipse.ui.internal.hotspots.HotspotsView;
import org.sonarlint.eclipse.ui.internal.job.BackendProgressJobScheduler;
import org.sonarlint.eclipse.ui.internal.job.OpenIssueInEclipseJob;
import org.sonarlint.eclipse.ui.internal.job.OpenIssueInEclipseJob.OpenIssueContext;
import org.sonarlint.eclipse.ui.internal.popup.BindingSuggestionPopup;
import org.sonarlint.eclipse.ui.internal.popup.DeveloperNotificationPopup;
import org.sonarlint.eclipse.ui.internal.popup.MessagePopup;
import org.sonarlint.eclipse.ui.internal.popup.NoBindingSuggestionFoundPopup;
import org.sonarlint.eclipse.ui.internal.popup.SingleBindingSuggestionPopup;
import org.sonarlint.eclipse.ui.internal.popup.SoonUnsupportedPopup;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.DisplayUtils;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarsource.sonarlint.core.clientapi.backend.config.binding.BindingSuggestionDto;
import org.sonarsource.sonarlint.core.clientapi.client.OpenUrlInBrowserParams;
import org.sonarsource.sonarlint.core.clientapi.client.binding.AssistBindingParams;
import org.sonarsource.sonarlint.core.clientapi.client.binding.AssistBindingResponse;
import org.sonarsource.sonarlint.core.clientapi.client.binding.NoBindingSuggestionFoundParams;
import org.sonarsource.sonarlint.core.clientapi.client.binding.SuggestBindingParams;
import org.sonarsource.sonarlint.core.clientapi.client.connection.AssistCreatingConnectionParams;
import org.sonarsource.sonarlint.core.clientapi.client.connection.AssistCreatingConnectionResponse;
import org.sonarsource.sonarlint.core.clientapi.client.event.DidReceiveServerEventParams;
import org.sonarsource.sonarlint.core.clientapi.client.hotspot.HotspotDetailsDto;
import org.sonarsource.sonarlint.core.clientapi.client.hotspot.ShowHotspotParams;
import org.sonarsource.sonarlint.core.clientapi.client.info.GetClientInfoResponse;
import org.sonarsource.sonarlint.core.clientapi.client.issue.ShowIssueParams;
import org.sonarsource.sonarlint.core.clientapi.client.message.ShowMessageParams;
import org.sonarsource.sonarlint.core.clientapi.client.message.ShowSoonUnsupportedMessageParams;
import org.sonarsource.sonarlint.core.clientapi.client.progress.ReportProgressParams;
import org.sonarsource.sonarlint.core.clientapi.client.progress.StartProgressParams;
import org.sonarsource.sonarlint.core.clientapi.client.smartnotification.ShowSmartNotificationParams;
import org.sonarsource.sonarlint.core.clientapi.client.sync.DidSynchronizeConfigurationScopeParams;
import org.sonarsource.sonarlint.core.commons.TextRange;
import org.sonarsource.sonarlint.core.serverapi.push.IssueChangedEvent;
import org.sonarsource.sonarlint.core.serverapi.push.TaintVulnerabilityClosedEvent;
import org.sonarsource.sonarlint.core.serverapi.push.TaintVulnerabilityRaisedEvent;

public class SonarLintEclipseClient extends SonarLintEclipseHeadlessClient {

  @Override
  public void openUrlInBrowser(OpenUrlInBrowserParams params) {
    BrowserUtils.openExternalBrowser(params.getUrl(), Display.getDefault());
  }

  @Override
  public void suggestBinding(SuggestBindingParams params) {
    Map<ISonarLintProject, List<BindingSuggestionDto>> suggestionsByProject = new HashMap<>();
    params.getSuggestions().forEach((configScopeId, suggestions) -> {
      var projectOpt = resolveProject(configScopeId);
      if (projectOpt.isPresent() && projectOpt.get().isOpen()) {
        suggestionsByProject.put(projectOpt.get(), suggestions);
      }
    });

    if (!suggestionsByProject.isEmpty()) {
      var firstProjectSuggestions = suggestionsByProject.values().iterator().next();
      if (suggestionsByProject.values().stream().allMatch(s -> areAllSameSuggestion(s, firstProjectSuggestions))) {
        // Suggest binding all projects to the suggestion
        Display.getDefault().asyncExec(() -> {
          var popup = new SingleBindingSuggestionPopup(List.copyOf(suggestionsByProject.keySet()), firstProjectSuggestions.get(0));
          popup.open();
        });
      } else {
        // Suggest binding all projects but let the user choose
        Display.getDefault().asyncExec(() -> {
          var popup = new BindingSuggestionPopup(List.copyOf(suggestionsByProject.keySet()));
          popup.open();
        });
      }
    }
  }

  private static boolean areAllSameSuggestion(List<BindingSuggestionDto> otherSuggestions, List<BindingSuggestionDto> firstProjectSuggestions) {
    if (otherSuggestions.size() != 1 || firstProjectSuggestions.size() != 1) {
      return false;
    }
    var suggestionDto = otherSuggestions.get(0);
    var firstSuggestion = firstProjectSuggestions.get(0);
    return Objects.equals(suggestionDto.getConnectionId(), firstSuggestion.getConnectionId())
      && Objects.equals(suggestionDto.getSonarProjectKey(), firstSuggestion.getSonarProjectKey());
  }

  @Override
  public void showMessage(ShowMessageParams params) {
    showPopup(params.getType(), params.getText());
  }

  @Override
  public CompletableFuture<GetClientInfoResponse> getClientInfo() {
    return CompletableFuture.completedFuture(new GetClientInfoResponse(ResourcesPlugin.getWorkspace().getRoot().getLocation().lastSegment()));
  }

  @Override
  public void showHotspot(ShowHotspotParams params) {
    var projectOpt = resolveProject(params.getConfigurationScopeId());
    if (projectOpt.isEmpty()) {
      showPopup("ERROR", "Cannot find project with scope '" + params.getConfigurationScopeId() + "', not in sync'");
      return;
    }
    var project = projectOpt.get();
    var hotspotFilePath = params.getHotspotDetails().getFilePath();
    var hotspotFile = findHotspotFile(hotspotFilePath, project);
    if (hotspotFile.isEmpty()) {
      showPopup("ERROR", "Unable to find file '" + hotspotFilePath + "' in '" + project.getName() + "'");
      return;
    }
    show(hotspotFile.get(), params.getHotspotDetails());
  }

  @Override
  public CompletableFuture<AssistCreatingConnectionResponse> assistCreatingConnection(AssistCreatingConnectionParams params) {
    return CompletableFuture.supplyAsync(() -> {
      var baseUrl = params.getServerUrl();
      
      try {
        AbstractAssistCreatingConnectionJob job;
        
        SonarLintLogger.get().debug("Assist creating a new connection...");
        if (params.getTokenName() != null && params.getTokenValue() != null) {
          job = new AssistCreatingAutomaticConnectionJob(baseUrl, params.getTokenValue());
        } else {
          job = new AssistCreatingManualConnectionJob(baseUrl);
        }
        
        job.schedule();
        job.join();
        if (job.getResult().isOK()) {
          // Provide the backend with all projects currently opened
          SonarLintLogger.get().debug("Successfully created connection '" + job.getConnectionId() + "'");
          return new AssistCreatingConnectionResponse(job.getConnectionId(),
            ProjectsProviderUtils.allConfigurationScopeIds());
        } else if (job.getResult().matches(IStatus.CANCEL)) {
          SonarLintLogger.get().debug("Assist creating connection was cancelled.");
        }
        throw new IllegalStateException(job.getResult().getMessage(), job.getResult().getException());
      } catch (InterruptedException e) {
        SonarLintLogger.get().debug("Assist creating connection was interrupted.");
        Thread.currentThread().interrupt();
        throw new CancellationException("Interrupted!");
      }
    });
  }
  
  @Override
  public void noBindingSuggestionFound(NoBindingSuggestionFoundParams params) {
    NoBindingSuggestionFoundPopup.displayPopupIfNotIgnored(params.getProjectKey());
  }

  @Override
  public CompletableFuture<AssistBindingResponse> assistBinding(AssistBindingParams params) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        SonarLintLogger.get().debug("Assist creating a new binding...");
        var job = new AssistBindingJob(params.getConnectionId(), params.getProjectKey());
        job.schedule();
        job.join();
        if (job.getResult().isOK()) {
          SonarLintLogger.get().debug("Successfully created binding");
          return new AssistBindingResponse(ConfigScopeSynchronizer.getConfigScopeId(job.getProject()));
        }
        throw new IllegalStateException(job.getResult().getMessage(), job.getResult().getException());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new CancellationException("Interrupted!");
      }
    });
  }

  private static Optional<ISonarLintFile> findHotspotFile(String hotspotFilePath, ISonarLintProject project) {
    return SonarLintCorePlugin.getServersManager().resolveBinding(project)
      .flatMap(binding -> binding.getProjectBinding().serverPathToIdePath(hotspotFilePath))
      .flatMap(project::find);
  }

  private static void show(ISonarLintFile file, HotspotDetailsDto hotspot) {
    Display.getDefault().asyncExec(() -> {
      DisplayUtils.bringToFront();
      var doc = getDocumentFromEditorOrFile(file);
      var marker = createMarker(file, hotspot, doc);
      try {
        var view = (HotspotsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(HotspotsView.ID);
        view.openHotspot(hotspot, marker);
      } catch (Exception e) {
        SonarLintLogger.get().error("Unable to open Hotspots View", e);
      }
    });
  }

  @Nullable
  private static IMarker createMarker(ISonarLintFile file, HotspotDetailsDto hotspot, IDocument doc) {
    IMarker marker = null;
    try {
      marker = file.getResource().createMarker(SonarLintCorePlugin.MARKER_HOTSPOT_ID);
      marker.setAttribute(IMarker.MESSAGE, hotspot.getMessage());
      marker.setAttribute(IMarker.LINE_NUMBER, hotspot.getTextRange().getStartLine());
      var position = MarkerUtils.getPosition(doc,
        new TextRange(hotspot.getTextRange().getStartLine(), hotspot.getTextRange().getStartLineOffset(), hotspot.getTextRange().getEndLine(),
          hotspot.getTextRange().getEndLineOffset()));
      if (position != null && Objects.equals(hotspot.getCodeSnippet(), doc.get(position.getOffset(), position.getLength()))) {
        marker.setAttribute(IMarker.CHAR_START, position.getOffset());
        marker.setAttribute(IMarker.CHAR_END, position.getOffset() + position.getLength());
      }
    } catch (Exception e) {
      SonarLintLogger.get().debug("Unable to create hotspot marker", e);
    }
    return marker;
  }

  private static IDocument getDocumentFromEditorOrFile(ISonarLintFile file) {
    IDocument doc;
    var editorPart = PlatformUtils.findEditor(file);
    if (editorPart instanceof ITextEditor) {
      doc = ((ITextEditor) editorPart).getDocumentProvider().getDocument(editorPart.getEditorInput());
    } else {
      doc = file.getDocument();
    }
    return doc;
  }

  private static void showPopup(String type, String message) {
    Display.getDefault().asyncExec(() -> {
      var popup = new MessagePopup(type, message);
      popup.open();
    });
  }

  @Override
  public void showSmartNotification(ShowSmartNotificationParams params) {
    var connectionOpt = SonarLintCorePlugin.getServersManager().findById(params.getConnectionId());
    if (connectionOpt.isEmpty()) {
      return;
    }

    Display.getDefault().asyncExec(() -> new DeveloperNotificationPopup(connectionOpt.get(), params, connectionOpt.get().isSonarCloud()).open());
  }

  /** Start IDE progress bar for backend jobs running out of IDE scope */
  @Override
  public CompletableFuture<Void> startProgress(StartProgressParams params) {
    return BackendProgressJobScheduler.get().startProgress(params);
  }

  /** Update / finish IDE progress bar for backend jobs running out of IDE scope */
  @Override
  public void reportProgress(ReportProgressParams params) {
    if (params.getNotification().isLeft()) {
      BackendProgressJobScheduler.get().update(params.getTaskId(), params.getNotification().getLeft());
    } else {
      BackendProgressJobScheduler.get().complete(params.getTaskId());
    }
  }

  /**
   *  After synchronization of the backend, the taint vulnerabilities view should be updated with the remote findings
   *  and also an analysis of the opened files should be triggered.
   */
  @Override
  public void didSynchronizeConfigurationScopes(DidSynchronizeConfigurationScopeParams params) {
    params.getConfigurationScopeIds().stream()
      .map(id -> resolveProject(id))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toSet())
      .forEach(project -> {
        var openedFiles = PlatformUtils.collectOpenedFiles(project, f -> true);
        if (!openedFiles.isEmpty() && openedFiles.containsKey(project)) {
          var bindingOpt = SonarLintCorePlugin.getServersManager().resolveBinding(project);
          if (bindingOpt.isPresent()) {
            var connection = (ConnectedEngineFacade) bindingOpt.get().getEngineFacade();

            // present taint vulnerabilities without re-fetching them from the server
            var files = openedFiles.get(project).stream()
              .map(file -> file.getFile())
              .collect(Collectors.toList());
            new TaintIssuesUpdateAfterSyncJob(connection, project, files).schedule();

            // Also schedule analyze of opened files based on synced information, we can ignore unavailable languages
            // as this project is actually bound to SonarQube / SonarCloud!
            AnalysisJobsScheduler.scheduleAnalysisOfOpenFiles(project, TriggerType.BINDING_CHANGE,
              f -> SonarLintUtils.isBoundToConnection(f, connection), false);
          }
        }
      });
  }

  @Override
  public void showSoonUnsupportedMessage(ShowSoonUnsupportedMessageParams params) {
    var connectionVersionCombination = params.getDoNotShowAgainId();
    if (SonarLintGlobalConfiguration.alreadySoonUnsupportedConnection(connectionVersionCombination)) {
      return;
    }

    Display.getDefault().syncExec(() -> {
      var popup = new SoonUnsupportedPopup(params.getDoNotShowAgainId(), params.getText());
      popup.setFadingEnabled(false);
      popup.setDelayClose(0L);
      popup.open();
    });
  }

  /**
   *  Before this method is even invoked, some magic already interacts with Eclipse: When there is no connection to
   *  SonarQube, it will automatically launch the "Connect to SonarQube or SonarCloud" wizard. Some magic will also ask
   *  the user to find the correct project if there is currently no binding to the SonarQube project.
   *  -> both cases must not be tested afterwards
   */
  @Override
  public void showIssue(ShowIssueParams params) {
    var configScopeId = params.getConfigScopeId();

    // We were just asked to find the correct project, this cannot happen -> Only log the information
    var projectOpt = resolveProject(configScopeId);
    if (projectOpt.isEmpty()) {
      SonarLintLogger.get().error("Open in IDE: The project '" + configScopeId
        + "' was removed in the middle of running the action on '" + params + "'");
      return;
    }
    var project = projectOpt.get();

    // We were just asked to connect and create a binding, this cannot happen -> Only log the information
    var bindingOpt = SonarLintCorePlugin.getServersManager().resolveBinding(project);
    if (bindingOpt.isEmpty()) {
      SonarLintLogger.get().error("Open in IDE: The project '" + configScopeId
        + "' removed its binding in the middle of running the action on '" + params + "'");
      return;
    }

    // Handle expensive checks and actual logic in separate job to not block the thread
    new OpenIssueInEclipseJob(new OpenIssueContext("Open in IDE", params, project, bindingOpt.get()))
      .schedule();
  }

  @Override
  public void didReceiveServerEvent(DidReceiveServerEventParams params) {
    var event = params.getServerEvent();
    var connectionId = params.getConnectionId();
    var facadeOpt = SonarLintCorePlugin.getServersManager().findById(connectionId);
    facadeOpt.ifPresent(facade -> {
      // FIXME Very inefficient implementation. Should be acceptable as we don't expect to have too many taint vulnerabilities
      if (event instanceof TaintVulnerabilityClosedEvent) {
        var projectKey = ((TaintVulnerabilityClosedEvent) event).getProjectKey();
        refreshTaintVulnerabilitiesForProjectsBoundToProjectKey((ConnectedEngineFacade) facade, projectKey);
      } else if (event instanceof TaintVulnerabilityRaisedEvent) {
        var projectKey = ((TaintVulnerabilityRaisedEvent) event).getProjectKey();
        refreshTaintVulnerabilitiesForProjectsBoundToProjectKey((ConnectedEngineFacade) facade, projectKey);
      } else if (event instanceof IssueChangedEvent) {
        var issueChangedEvent = (IssueChangedEvent) event;
        var projectKey = issueChangedEvent.getProjectKey();
        refreshTaintVulnerabilitiesForProjectsBoundToProjectKey((ConnectedEngineFacade) facade, projectKey);
      }
    });
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
