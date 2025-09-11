/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.stream.Collectors;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.SonarLintLanguage;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.internal.backend.SonarLintEclipseHeadlessRpcClient;
import org.sonarlint.eclipse.core.internal.jobs.AnalysisReadyStatusCache;
import org.sonarlint.eclipse.core.internal.jobs.TaintIssuesMarkerUpdateJob;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.ProjectSuggestionDto;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;
import org.sonarlint.eclipse.ui.internal.binding.assist.AbstractAssistCreatingConnectionJob;
import org.sonarlint.eclipse.ui.internal.binding.assist.AssistBindingJob;
import org.sonarlint.eclipse.ui.internal.binding.assist.AssistCreatingAutomaticConnectionJob;
import org.sonarlint.eclipse.ui.internal.binding.assist.AssistCreatingManualConnectionJob;
import org.sonarlint.eclipse.ui.internal.binding.assist.AssistSuggestConnectionJob;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.EditNotificationsWizard;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingProcess;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingWizard;
import org.sonarlint.eclipse.ui.internal.dialog.SuggestConnectionDialog;
import org.sonarlint.eclipse.ui.internal.dialog.SuggestMultipleConnectionSelectionDialog;
import org.sonarlint.eclipse.ui.internal.dialog.SuggestMultipleConnectionsDialog;
import org.sonarlint.eclipse.ui.internal.job.BackendProgressJobScheduler;
import org.sonarlint.eclipse.ui.internal.job.OpenFixSuggestionInEclipseJob;
import org.sonarlint.eclipse.ui.internal.job.OpenIssueInEclipseJob;
import org.sonarlint.eclipse.ui.internal.job.OpenIssueInEclipseJob.OpenIssueContext;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarsource.sonarlint.core.rpc.client.SonarLintCancelChecker;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.binding.BindingSuggestionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.TaintVulnerabilityDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.binding.AssistBindingParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.binding.AssistBindingResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.client.binding.NoBindingSuggestionFoundParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.connection.AssistCreatingConnectionParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.connection.AssistCreatingConnectionResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.client.connection.ConnectionSuggestionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.fix.FixSuggestionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.hotspot.HotspotDetailsDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.IssueDetailsDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.log.LogParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.message.MessageType;
import org.sonarsource.sonarlint.core.rpc.protocol.client.message.ShowSoonUnsupportedMessageParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.progress.ReportProgressParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.progress.StartProgressParams;
import org.sonarsource.sonarlint.core.rpc.protocol.client.smartnotification.ShowSmartNotificationParams;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Either;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Language;

import static org.sonarlint.eclipse.ui.internal.notifications.Notification.newNotification;

public class SonarLintEclipseRpcClient extends SonarLintEclipseHeadlessRpcClient {

  @Override
  public void openUrlInBrowser(URL url) {
    BrowserUtils.openExternalBrowser(url, Display.getDefault());
  }

  @Override
  public void suggestBinding(Map<String, List<BindingSuggestionDto>> suggestionsByConfigScope) {
    Map<ISonarLintProject, List<BindingSuggestionDto>> suggestionsByProject = new HashMap<>();
    suggestionsByConfigScope.forEach((configScopeId, suggestions) -> {
      var projectOpt = SonarLintUtils.tryResolveProject(configScopeId);
      if (projectOpt.isPresent()) {
        // Everything can happen asynchronously on Sloop, therefore there might be a late binding suggestion coming
        // in for a project that is already bound and configured locally and is currently "updated" in Sloop in a
        // different thread!
        var project = projectOpt.get();
        if (project.isOpen() && !ProjectBindingProcess.isProjectBound(project)) {
          suggestionsByProject.put(projectOpt.get(), suggestions);
        }
      }
    });

    // TODO don't ask again?
    if (!suggestionsByProject.isEmpty()) {
      var firstProjectSuggestions = suggestionsByProject.values().iterator().next();
      if (suggestionsByProject.values().stream().allMatch(s -> areAllSameSuggestion(s, firstProjectSuggestions))) {
        suggestSingleBinding(suggestionsByProject.keySet(), firstProjectSuggestions.get(0));
      } else {
        // Suggest binding all projects but let the user choose
        suggestMultipleBindings(suggestionsByProject.keySet());
      }
    }
  }

  private static void suggestSingleBinding(Set<ISonarLintProject> projectsToBind, BindingSuggestionDto suggestion) {
    String message;
    if (projectsToBind.size() == 1) {
      message = "Bind project " + projectsToBind.iterator().next().getName() +
        " to '" + suggestion.getSonarProjectName() + "' on '" + suggestion.getConnectionId() + "'?";
    } else {
      message = "Bind " + projectsToBind.size() +
        " projects to '" + suggestion.getSonarProjectName() + "' on '" + suggestion.getConnectionId() + "'?";
    }
    newNotification()
      .setTitle("SonarQube - Binding Suggestion")
      .setBody(message)
      .addActionWithTooltip("Bind", "Accept suggested binding", s -> {
        ProjectBindingProcess.bindProjects(suggestion.getConnectionId(), List.copyOf(projectsToBind), suggestion.getSonarProjectKey());
        if (SonarLintTelemetry.isEnabled()) {
          if (suggestion.isFromSharedConfiguration()) {
            SonarLintTelemetry.addedImportedBindings();
          } else {
            SonarLintTelemetry.addedAutomaticBindings();
          }
        }
      })
      .addActionWithTooltip("Select another", "Select another binding", shell -> ProjectBindingWizard.createDialog(shell, projectsToBind).open())
      .addLearnMoreLink(SonarLintDocumentation.CONNECTED_MODE_SETUP_LINK)
      .addDoNotAskAgainAction(projectsToBind, projectConfig -> projectConfig.setBindingSuggestionsDisabled(true))
      .show();
  }

  private static void suggestMultipleBindings(Set<ISonarLintProject> projectsToBind) {
    String message;
    if (projectsToBind.size() == 1) {
      message = "Bind project " + projectsToBind.iterator().next().getName() + "'?";
    } else {
      message = "Bind " + projectsToBind.size() + " projects?";
    }
    newNotification()
      .setTitle("SonarQube - Binding Suggestion")
      .addActionWithTooltip("Bind", "Select binding", shell -> ProjectBindingWizard.createDialog(shell, projectsToBind).open())
      .addLink("Learn more", SonarLintDocumentation.CONNECTED_MODE_SETUP_LINK)
      .addDoNotAskAgainAction(projectsToBind, projectConfig -> projectConfig.setBindingSuggestionsDisabled(true))
      .setBody(message)
      .show();

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
  public void showMessage(MessageType type, String text) {
    newNotification()
      .setTitle("SonarQube")
      .setBody(text)
      .show();
  }

  @Override
  public String getClientLiveDescription() {
    return ResourcesPlugin.getWorkspace().getRoot().getLocation().lastSegment();
  }

  @Override
  public void showHotspot(String configurationScopeId, HotspotDetailsDto hotspotDetails) {
    SonarLintUtils.tryResolveProject(configurationScopeId)
      .ifPresent(project -> new ShowHotspotJob(project, hotspotDetails).schedule());
  }

  @Override
  public AssistCreatingConnectionResponse assistCreatingConnection(AssistCreatingConnectionParams params, SonarLintCancelChecker cancelChecker) throws CancellationException {
    try {
      AbstractAssistCreatingConnectionJob job;

      // We want to check if is a request for a SonarQube server or SonarCloud. Based on the parameter "left" is
      // SonarQube (denoted by a URL) and "right" is SonarCloud (denoted by a Organization)
      Either<String, String> serverUrlOrOrganization;
      var connectionParams = params.getConnectionParams();
      if (connectionParams.isLeft()) {
        serverUrlOrOrganization = Either.forLeft(connectionParams.getLeft().getServerUrl());
      } else {
        serverUrlOrOrganization = Either.forRight(connectionParams.getRight().getOrganizationKey());
      }

      var region = connectionParams.isRight() ? connectionParams.getRight().getRegion().name() : null;

      SonarLintLogger.get().debug("Assist creating a new connection...");
      if (params.getTokenName() != null && params.getTokenValue() != null) {
        job = new AssistCreatingAutomaticConnectionJob(serverUrlOrOrganization, params.getTokenValue(), region);
      } else {
        job = new AssistCreatingManualConnectionJob(serverUrlOrOrganization, region);
      }

      job.schedule();
      job.join();
      if (job.getResult().isOK()) {
        SonarLintLogger.get().debug("Successfully created connection '" + job.getConnectionId() + "'");
        return new AssistCreatingConnectionResponse(job.getConnectionId());
      } else if (job.getResult().matches(IStatus.CANCEL)) {
        SonarLintLogger.get().debug("Assist creating connection was cancelled.");
      }
      throw new IllegalStateException(job.getResult().getMessage(), job.getResult().getException());
    } catch (InterruptedException e) {
      SonarLintLogger.get().debug("Assist creating connection was interrupted.");
      throw new CancellationException("Interrupted!");
    }
  }

  @Override
  public void noBindingSuggestionFound(NoBindingSuggestionFoundParams params) {
    var isSonarCloud = params.isSonarCloud();
    newNotification()
      .setTitle("SonarQube " + (isSonarCloud ? "Cloud" : "Server") + " - No matching open project found")
      .setIcon(isSonarCloud ? SonarLintImages.SONARCLOUD_SERVER_ICON_IMG : SonarLintImages.SONARQUBE_SERVER_ICON_IMG)
      .setBody("The SonarQube "
        + (isSonarCloud ? "Cloud" : "Server")
        + " project '" + params.getProjectKey() + "' cannot be matched to any project in the workspace. "
        + "Please open your project, or bind it manually, and try again.")
      .addLink("Open Troubleshooting documentation", SonarLintDocumentation.TROUBLESHOOTING_LINK)
      .show();
  }

  @Override
  public AssistBindingResponse assistBinding(AssistBindingParams params, SonarLintCancelChecker cancelChecker) throws CancellationException {
    try {
      SonarLintLogger.get().debug("Assist creating a new binding...");
      var job = new AssistBindingJob(params.getConnectionId(), params.getProjectKey());
      job.schedule();
      job.join();
      if (job.getResult().isOK()) {
        SonarLintLogger.get().debug("Successfully created binding");

        if (SonarLintTelemetry.isEnabled()) {
          if (params.isFromSharedConfiguration()) {
            SonarLintTelemetry.addedImportedBindings();
          } else {
            SonarLintTelemetry.addedAutomaticBindings();
          }
        }

        return new AssistBindingResponse(ConfigScopeSynchronizer.getConfigScopeId(job.getProject()));
      }
      throw new IllegalStateException(job.getResult().getMessage(), job.getResult().getException());
    } catch (InterruptedException e) {
      throw new CancellationException("Interrupted!");
    }
  }

  @Override
  public void showSmartNotification(ShowSmartNotificationParams params) {
    var connectionOpt = SonarLintCorePlugin.getConnectionManager().findById(params.getConnectionId());
    if (connectionOpt.isEmpty()) {
      return;
    }
    var connection = connectionOpt.get();
    var isSonarCloud = connection.isSonarCloud();
    var product = isSonarCloud ? "SonarQube Cloud" : "SonarQube Server";

    newNotification()
      .setTitle(product + " Notification")
      .setIcon(isSonarCloud ? SonarLintImages.SONARCLOUD_SERVER_ICON_IMG : SonarLintImages.SONARQUBE_SERVER_ICON_IMG)
      .setBody(params.getText())
      .addLink("Open in " + product, params.getLink(), () -> SonarLintTelemetry.devNotificationsClicked(params.getCategory()))
      .addAction("Configure", shell -> EditNotificationsWizard.createDialog(shell, connection).open())
      .show();
  }

  /** Start IDE progress bar for backend jobs running out of IDE scope */
  @Override
  public void startProgress(StartProgressParams params) throws UnsupportedOperationException {
    BackendProgressJobScheduler.get().startProgress(params);
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
  public void didSynchronizeConfigurationScopes(Set<String> configurationScopeIds) {
    super.didSynchronizeConfigurationScopes(configurationScopeIds);
    configurationScopeIds.stream()
      .map(SonarLintUtils::tryResolveProject)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .distinct()
      .forEach(project -> {
        var openedFiles = PlatformUtils.collectOpenedFiles(project, f -> true);
        if (!openedFiles.isEmpty() && openedFiles.containsKey(project)) {
          var bindingOpt = SonarLintCorePlugin.getConnectionManager().resolveBinding(project);
          if (bindingOpt.isPresent()) {
            var connection = bindingOpt.get().getConnectionFacade();

            // present taint vulnerabilities without re-fetching them from the server
            var files = openedFiles.get(project).stream()
              .map(file -> file.getFile())
              .collect(Collectors.toList());
            new TaintIssuesMarkerUpdateJob(connection, project, files).schedule();

            // also schedule analyze of opened files based on synced information
            AnalysisJobsScheduler.scheduleAnalysisOfOpenFiles(project, TriggerType.BINDING_CHANGE,
              f -> SonarLintUtils.isBoundToConnection(f, connection));
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

    newNotification()
      .setTitle("SonarQube Server - Soon unsupported version")
      .setIcon(SonarLintImages.SONARQUBE_SERVER_ICON_IMG)
      .setBody(params.getText())
      .addAction("Don't show again", s -> SonarLintGlobalConfiguration.addSoonUnsupportedConnection(params.getDoNotShowAgainId()))
      .addLink("Learn more", SonarLintDocumentation.VERSION_SUPPORT_POLICY)
      .show();
  }

  /**
   *  Before this method is even invoked, some magic already interacts with Eclipse: When there is no connection to
   *  SonarQube, it will automatically launch the "Connect to SonarQube or SonarCloud" wizard. Some magic will also ask
   *  the user to find the correct project if there is currently no binding to the SonarQube project.
   *  -> both cases must not be tested afterwards
   *
   *  Both cases to get the project and binding should not be failing as this was just set up if not already done
   *  beforehand! Therefore we just log it ^^
   */
  @Override
  public void showIssue(String configScopeId, IssueDetailsDto issueDetails) {
    var projectOpt = SonarLintUtils.tryResolveProject(configScopeId);
    if (projectOpt.isEmpty()) {
      SonarLintLogger.get().error("Open in IDE: The project '" + configScopeId + "' is not found in the workspace");
      return;
    }
    var project = projectOpt.get();

    var bindingOpt = SonarLintCorePlugin.getConnectionManager().resolveBinding(project);
    if (bindingOpt.isEmpty()) {
      SonarLintLogger.get().error("Open in IDE: The project '" + configScopeId
        + "' removed its binding in the middle of running the showIssue action on '" + issueDetails.getIssueKey() + "'");
      return;
    }

    // Handle expensive checks and actual logic in separate job to not block the thread
    new OpenIssueInEclipseJob(new OpenIssueContext("Open in IDE", issueDetails, project, bindingOpt.get()))
      .schedule(500);
  }

  /**
   *  The behavior before this is invoked regarding setting up the Connected Mode (including binding) or finding the
   *  correct project in the workspace is exactly the same as for the "showIssue(...)" method!
   */
  @Override
  public void showFixSuggestion(String configScopeId, String issueKey, FixSuggestionDto fixSuggestion) {
    var projectOpt = SonarLintUtils.tryResolveProject(configScopeId);
    if (projectOpt.isEmpty()) {
      SonarLintLogger.get().error("Open fix suggestion in the IDE: The project '" + configScopeId
        + "' is not found in the workspace");
      return;
    }
    var project = projectOpt.get();

    // Handle expensive checks and actual logic in separate job to not block the thread
    new OpenFixSuggestionInEclipseJob(fixSuggestion, project).schedule();
  }

  @Override
  public void didChangeTaintVulnerabilities(String configurationScopeId, Set<UUID> closedTaintVulnerabilityIds, List<TaintVulnerabilityDto> addedTaintVulnerabilities,
    List<TaintVulnerabilityDto> updatedTaintVulnerabilities) {
    var projectOpt = SonarLintUtils.tryResolveProject(configurationScopeId);
    if (projectOpt.isEmpty()) {
      return;
    }
    var project = projectOpt.get();
    var bindingOpt = SonarLintCorePlugin.getConnectionManager().resolveBinding(project);
    if (bindingOpt.isPresent()) {
      // INFO: It can be that there is no file of that project currently opened, in that case return directly!
      var openedFiles = PlatformUtils.collectOpenedFiles(project, f -> true);
      var projectFiles = openedFiles.get(project);
      if (projectFiles == null || projectFiles.isEmpty()) {
        return;
      }

      var files = projectFiles.stream()
        .map(file -> file.getFile())
        .collect(Collectors.toList());

      new TaintIssuesMarkerUpdateJob(bindingOpt.get().getConnectionFacade(), project, files).schedule();
    }
  }

  @Override
  public void log(LogParams params) {
    var message = params.getMessage();
    var stackTrace = params.getStackTrace() != null
      ? ("\n\n" + params.getStackTrace())
      : "";

    // The tracing coming from SLCORE should not be confused with the SonarLintLogger#traceIdeMessage(String) message!
    // This is only to be used for IDE-specific logging (e.g. adaptations, interaction with extension points, ...)
    switch (params.getLevel()) {
      case TRACE:
      case DEBUG:
        SonarLintLogger.get().debug(message + stackTrace);
        break;
      case ERROR:
        SonarLintLogger.get().error(message + stackTrace);
        break;
      default:
        SonarLintLogger.get().info(message + stackTrace);
    }
  }

  @Override
  public void didChangeAnalysisReadiness(Set<String> configurationScopeIds, boolean areReadyForAnalysis) {
    var projects = configurationScopeIds.stream()
      .map(SonarLintUtils::tryResolveProject)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());

    // In order for use to simplify listening to changes in ITs we log the information per configuration scope id!
    for (var project : projects) {
      SonarLintLogger.get().debug("Project at '" + project.getName()
        + "' changed ready status for analysis to: " + areReadyForAnalysis);
    }

    configurationScopeIds.stream()
      .forEach(configurationScopeId -> AnalysisReadyStatusCache.changeAnalysisReadiness(configurationScopeId, areReadyForAnalysis));
    if (areReadyForAnalysis) {
      AnalysisJobsScheduler.scheduleAnalysisOfOpenFiles(projects, TriggerType.ANALYSIS_READY);
    }
  }

  /**
   *  This handles the connection suggestions of the shared Connected Mode. As Eclipse can have all kinds of different
   *  combinations we want to reduce the number of notifications to the user and therefore aggregate as much
   *  information as possible.
   */
  @Override
  public void suggestConnection(Map<String, List<ConnectionSuggestionDto>> suggestionsByConfigScope) {
    if (SonarLintGlobalConfiguration.noConnectionSuggestions()) {
      return;
    }

    var organizationBasedConnections = new HashMap<String, HashMap<String, List<ProjectSuggestionDto>>>();
    var regionsByOrganization = new HashMap<String, String>();
    var serverUrlBasedConnections = new HashMap<String, HashMap<String, List<ProjectSuggestionDto>>>();
    var multipleSuggestions = new HashMap<ISonarLintProject, List<ConnectionSuggestionDto>>();

    for (var entry : suggestionsByConfigScope.entrySet()) {
      var configScopeId = entry.getKey();
      var projectOpt = SonarLintUtils.tryResolveProject(configScopeId);
      if (projectOpt.isEmpty()) {
        SonarLintLogger.get().debug("No project can be found for '" + configScopeId + "' for a Connection Suggestion!");
        continue;
      }
      var project = projectOpt.get();

      var suggestions = entry.getValue();
      if (suggestions.size() > 1) {
        multipleSuggestions.put(project, suggestions);
        continue;
      }

      var isFromSharedConfiguration = suggestions.get(0).isFromSharedConfiguration();
      var eitherSuggestion = suggestions.get(0).getConnectionSuggestion();
      if (eitherSuggestion.isLeft()) {
        var suggestion = eitherSuggestion.getLeft();
        var serverUrl = suggestion.getServerUrl();
        var projectKey = suggestion.getProjectKey();

        serverUrlBasedConnections.putIfAbsent(serverUrl, new HashMap<>());

        var sonarProjects = serverUrlBasedConnections.get(serverUrl);
        sonarProjects.putIfAbsent(projectKey, new ArrayList<>());

        var projects = sonarProjects.get(projectKey);
        projects.add(new ProjectSuggestionDto(project, isFromSharedConfiguration));
      } else {
        var suggestion = eitherSuggestion.getRight();
        var organization = suggestion.getOrganization();
        var region = suggestion.getRegion().name();
        var projectKey = suggestion.getProjectKey();

        organizationBasedConnections.putIfAbsent(organization, new HashMap<>());
        regionsByOrganization.putIfAbsent(organization, region);

        var sonarProjects = organizationBasedConnections.get(organization);
        sonarProjects.putIfAbsent(projectKey, new ArrayList<>());

        var projects = sonarProjects.get(projectKey);
        projects.add(new ProjectSuggestionDto(project, isFromSharedConfiguration));
      }
    }

    Display.getDefault().asyncExec(() -> {
      // for all SonarCloud organizations display one notification each (so only one connection creation will be done
      // per organization)
      for (var entry : organizationBasedConnections.entrySet()) {
        suggestConnection(Either.forRight(entry.getKey()), entry.getValue(), regionsByOrganization.get(entry.getKey()));
      }

      // for all SonarQube URLs display one notification each (so only one connection creation will be done per URL)
      for (var entry : serverUrlBasedConnections.entrySet()) {
        suggestConnection(Either.forLeft(entry.getKey()), entry.getValue(), null);
      }

      // for all (complicated) leftover projects display a specific notification each
      for (var entry : multipleSuggestions.entrySet()) {
        suggestMultipleConnections(entry.getKey(), entry.getValue());
      }
    });
  }

  private static void suggestConnection(Either<String, String> serverUrlOrOrganization, Map<String, List<ProjectSuggestionDto>> projectMapping, @Nullable String sonarCloudRegion) {
    String prefix;
    if (serverUrlOrOrganization.isLeft()) {
      prefix = "For the SonarQube Server '" + serverUrlOrOrganization.getLeft();
    } else {
      prefix = "For the SonarQube Cloud organization '" + serverUrlOrOrganization.getRight();
    }

    String message;
    if (projectMapping.keySet().size() > 1) {
      message = prefix + "' there are multiple projects that can be connected to local projects. Click 'More "
        + "Information' to see them all. Do you want to connect and bind the project?";
    } else {
      var projectKey = projectMapping.keySet().toArray()[0];
      var mappedProjects = projectMapping.get(projectKey);
      if (mappedProjects.size() > 1) {
        message = prefix + "' the project '" + projectKey + "' can be connected to multiple local projects. Click 'More "
          + "Information' to see them all. Do you want to connect and bind the project?";
      } else {
        message = prefix + "' the project '" + projectKey
          + "' can be connected to the local project '" + mappedProjects.get(0).getProject().getName()
          + "'. Do you want to connect and bind the project?";
      }
    }

    var notification = newNotification()
      .setTitle("Connection Suggestion to " + (serverUrlOrOrganization.isLeft() ? "SonarQube Server" : "SonarQube Cloud"))
      .setIcon(serverUrlOrOrganization.isLeft() ? SonarLintImages.SONARQUBE_SERVER_ICON_IMG : SonarLintImages.SONARCLOUD_SERVER_ICON_IMG)
      .setBody(message)
      .addActionWithTooltip("Connect", "Connect to " + (serverUrlOrOrganization.isLeft() ? "server" : "organization"),
        s -> {
          var job = new AssistSuggestConnectionJob(serverUrlOrOrganization, projectMapping, sonarCloudRegion);
          job.schedule();
        });

    var projectKeys = projectMapping.keySet().toArray();
    if (projectKeys.length > 1 || projectMapping.get(projectKeys[0]).size() > 1) {
      notification.addAction("More information", shell -> new SuggestConnectionDialog(shell, serverUrlOrOrganization, projectMapping).open());
    }

    notification.addDoNotAskAgainAction(SonarLintGlobalConfiguration::setNoConnectionSuggestions)
      .show();
  }

  private static void suggestMultipleConnections(ISonarLintProject project, List<ConnectionSuggestionDto> suggestions) {
    newNotification()
      .setTitle("SonarQube - Multiple Connection Suggestions found")
      .setBody("The local project '" + project.getName() + "' can be connected based on different suggestions. Click "
        + "'More Information' to see them all. Do you want to choose which suggestion to use and then connect and bind "
        + "the project?")
      .addActionWithTooltip("Choose suggestion", "Based on suggestion", shell -> {
        // ask the user to select the suggestion they want to use
        var dialog = new SuggestMultipleConnectionSelectionDialog(shell, project, suggestions);
        dialog.open();
        var selection = (String) dialog.getFirstResult();
        if (selection == null) {
          return;
        }

        // from the dialog response we have to get back the actual connection suggestion
        var suggestion = dialog.getSuggestionFromElement(selection);

        var isFromSharedConnectedMode = suggestion.isFromSharedConfiguration();
        var isSonarQube = suggestion.getConnectionSuggestion().isLeft();
        var projectKey = isSonarQube
          ? suggestion.getConnectionSuggestion().getLeft().getProjectKey()
          : suggestion.getConnectionSuggestion().getRight().getProjectKey();
        var region = isSonarQube ? null : suggestion.getConnectionSuggestion().getRight().getRegion().name();

        Either<String, String> serverUrlOrOrganization;
        if (isSonarQube) {
          serverUrlOrOrganization = Either.forLeft(suggestion.getConnectionSuggestion().getLeft().getServerUrl());
        } else {
          serverUrlOrOrganization = Either.forRight(suggestion.getConnectionSuggestion().getRight().getOrganization());
        }

        var projectMapping = new HashMap<String, List<ProjectSuggestionDto>>();
        projectMapping.put(projectKey, List.of(new ProjectSuggestionDto(project, isFromSharedConnectedMode)));

        var job = new AssistSuggestConnectionJob(serverUrlOrOrganization, projectMapping, region);
        job.schedule();
      })
      .addAction("More information", shell -> new SuggestMultipleConnectionsDialog(shell, project, suggestions).open())
      .addDoNotAskAgainAction(SonarLintGlobalConfiguration::setNoConnectionSuggestions)
      .show();
  }

  @Override
  public void promoteExtraEnabledLanguagesInConnectedMode(String configurationScopeId, Set<Language> languagesToPromote) {
    var projectOpt = SonarLintUtils.tryResolveProject(configurationScopeId);
    if (projectOpt.isEmpty()) {
      return;
    }
    if (SonarLintGlobalConfiguration.ignoreMissingFeatureNotifications()) {
      return;
    }
    var languages = languagesToPromote.stream().map(language -> SonarLintLanguage.valueOf(language.name())).collect(Collectors.toList());

    String message;
    if (languages.size() == 1) {
      message = "You tried to analyze a " + org.sonarsource.sonarlint.core.client.utils.Language.valueOf(languages.get(0).name()).getLabel()
        + " file. This language analysis is only available in Connected Mode.";
    } else {
      var languagesList = String.join(" / ",
        languages.stream().map(l -> org.sonarsource.sonarlint.core.client.utils.Language.valueOf(l.name()).getLabel()).collect(Collectors.toList()));
      message = "You tried to analyze " + languagesList
        + " files. These language analyses are only available in Connected Mode.";
    }
    var notification = newNotification()
      .setTitle("SonarQube for Eclipse - Language" + (languages.size() > 1 ? "s" : "") + " could not be analyzed")
      .setBody(message)
      .addLearnMoreLink(SonarLintDocumentation.RULES);
    if (SonarLintCorePlugin.getConnectionManager().checkForSonarCloud()) {
      notification.addAction("Bind to SonarQube Cloud", shell -> ProjectBindingWizard.createDialog(shell, Set.of(projectOpt.get())));
    } else {
      notification.addLink("Try SonarQube Cloud for free", SonarLintDocumentation.SONARCLOUD_FREE_SIGNUP_LINK);
    }
    notification.addDoNotShowAgainAction(SonarLintGlobalConfiguration::setIgnoreMissingFeatureNotifications)
      .show();
  }

  @Override
  public void invalidToken(String connectionId) {
    var connectionFacadeOpt = SonarLintCorePlugin.getConnectionManager().findById(connectionId);
    if (connectionFacadeOpt.isEmpty()) {
      return;
    }
    var facade = connectionFacadeOpt.get();
    var isSonarCloud = facade.isSonarCloud();
    var product = isSonarCloud ? "SonarQube Cloud" : "SonarQube Server";
    newNotification()
      .setTitle(product + " - Invalid token for connection")
      .setIcon(isSonarCloud ? SonarLintImages.SONARCLOUD_SERVER_ICON_IMG : SonarLintImages.SONARQUBE_SERVER_ICON_IMG)
      .setBody("The token for the connection '" + facade.getId() + "' (" + facade.getHost() + ") is invalid. Please "
        + "change it to continue working in Connected Mode.")
      .addAction("Edit Connection", shell -> ServerConnectionWizard.createDialog(shell, facade).open())
      .show();
  }
}
