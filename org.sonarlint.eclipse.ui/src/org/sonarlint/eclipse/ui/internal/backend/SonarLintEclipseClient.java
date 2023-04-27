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
package org.sonarlint.eclipse.ui.internal.backend;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.internal.backend.SonarLintEclipseHeadlessClient;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.ProjectSelectionDialog;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionModel.ConnectionType;
import org.sonarlint.eclipse.ui.internal.binding.wizard.connection.ServerConnectionWizard;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingProcess;
import org.sonarlint.eclipse.ui.internal.hotspots.HotspotsView;
import org.sonarlint.eclipse.ui.internal.popup.BindingSuggestionPopup;
import org.sonarlint.eclipse.ui.internal.popup.DeveloperNotificationPopup;
import org.sonarlint.eclipse.ui.internal.popup.MessagePopup;
import org.sonarlint.eclipse.ui.internal.popup.SingleBindingSuggestionPopup;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.DisplayUtils;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarsource.sonarlint.core.clientapi.backend.config.binding.BindingSuggestionDto;
import org.sonarsource.sonarlint.core.clientapi.client.OpenUrlInBrowserParams;
import org.sonarsource.sonarlint.core.clientapi.client.SuggestBindingParams;
import org.sonarsource.sonarlint.core.clientapi.client.binding.AssistBindingParams;
import org.sonarsource.sonarlint.core.clientapi.client.binding.AssistBindingResponse;
import org.sonarsource.sonarlint.core.clientapi.client.connection.AssistCreatingConnectionParams;
import org.sonarsource.sonarlint.core.clientapi.client.connection.AssistCreatingConnectionResponse;
import org.sonarsource.sonarlint.core.clientapi.client.host.GetHostInfoResponse;
import org.sonarsource.sonarlint.core.clientapi.client.hotspot.HotspotDetailsDto;
import org.sonarsource.sonarlint.core.clientapi.client.hotspot.ShowHotspotParams;
import org.sonarsource.sonarlint.core.clientapi.client.message.ShowMessageParams;
import org.sonarsource.sonarlint.core.clientapi.client.progress.ReportProgressParams;
import org.sonarsource.sonarlint.core.clientapi.client.progress.StartProgressParams;
import org.sonarsource.sonarlint.core.clientapi.client.smartnotification.ShowSmartNotificationParams;
import org.sonarsource.sonarlint.core.clientapi.client.sync.DidSynchronizeConfigurationScopeParams;
import org.sonarsource.sonarlint.core.commons.TextRange;

public class SonarLintEclipseClient extends SonarLintEclipseHeadlessClient {

  @Override
  public void openUrlInBrowser(OpenUrlInBrowserParams params) {
    BrowserUtils.openExternalBrowser(params.getUrl());
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
  public CompletableFuture<GetHostInfoResponse> getHostInfo() {
    return CompletableFuture.completedFuture(new GetHostInfoResponse(ResourcesPlugin.getWorkspace().getRoot().getLocation().lastSegment()));
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
    return DisplayUtils.bringToFrontAsync()
      .thenComposeAsync(unused -> createConnection(params.getServerUrl()))
      .thenApplyAsync(connection -> new AssistCreatingConnectionResponse(connection.getId()));
  }

  @Override
  public CompletableFuture<AssistBindingResponse> assistBinding(AssistBindingParams params) {
    var connectionId = params.getConnectionId();
    var projectKey = params.getProjectKey();
    return DisplayUtils.bringToFrontAsync()
      .thenComposeAsync(unused -> bindProjectTo(connectionId, projectKey))
      .thenApplyAsync(project -> new AssistBindingResponse(ConfigScopeSynchronizer.getConfigScopeId(project)));
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

  @Nullable
  private static CompletableFuture<IConnectedEngineFacade> createConnection(String serverUrl) {
    var model = new ServerConnectionModel();
    model.setConnectionType(ConnectionType.ONPREMISE);
    model.setServerUrl(serverUrl);
    var wizard = new ServerConnectionWizard(model);
    wizard.setSkipBindingWizard(true);
    return DisplayUtils.asyncExec(() -> {
      var dialog = ServerConnectionWizard.createDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
      dialog.setBlockOnOpen(true);
      dialog.open();
      return wizard.getResultServer();
    });
  }

  private static CompletableFuture<ISonarLintProject> bindProjectTo(String connectionId, String projectKey) {
    return DisplayUtils.asyncExec(() -> ProjectSelectionDialog.pickProject(projectKey, connectionId))
      .thenComposeAsync(pickedProject -> {
        var bindingJob = ProjectBindingProcess.scheduleProjectBinding(connectionId, List.of(pickedProject), projectKey);
        try {
          bindingJob.join();
        } catch (InterruptedException e) {
          SonarLintLogger.get().error("Cannot bind project", e);
          Thread.currentThread().interrupt();
        }
        return CompletableFuture.completedFuture(pickedProject);
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

  // project synchronizations not yet set to be handled by sonarlint-core
  @Override
  public CompletableFuture<Void> startProgress(StartProgressParams params) {
    return null;
  }

  // project synchronizations not yet set to be handled by sonarlint-core
  @Override
  public void reportProgress(ReportProgressParams params) {
  }

  // project synchronizations not yet set to be handled by sonarlint-core
  @Override
  public void didSynchronizeConfigurationScopes(DidSynchronizeConfigurationScopeParams params) {
  }
}
