/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration.EclipseProjectBinding;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.core.internal.tracking.IssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueUpdater;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;

public class AnalyzeConnectedProjectJob extends AbstractAnalyzeProjectJob<ConnectedAnalysisConfiguration> {

  private final EclipseProjectBinding binding;
  private final Server server;

  public AnalyzeConnectedProjectJob(AnalyzeProjectRequest request, EclipseProjectBinding binding, Server server) {
    super(request);
    this.binding = binding;
    this.server = server;
  }

  @Override
  protected ConnectedAnalysisConfiguration prepareAnalysisConfig(Path projectBaseDir, List<ClientInputFile> inputFiles, Map<String, String> mergedExtraProps) {
    SonarLintLogger.get().debug("Connected mode (using configuration of '" + binding.projectKey() + "' in server '" + binding.serverId() + "')");
    return new ConnectedAnalysisConfiguration(binding.projectKey(), projectBaseDir, getProject().getWorkingDir(), inputFiles, mergedExtraProps);
  }

  @Override
  protected AnalysisResults runAnalysis(ConnectedAnalysisConfiguration analysisConfig, SonarLintIssueListener issueListener, IProgressMonitor monitor) {
    return server.runAnalysis((ConnectedAnalysisConfiguration) analysisConfig, issueListener, monitor);
  }

  @Override
  protected void trackIssues(Map<ISonarLintFile, IDocument> docPerFile, Map<ISonarLintIssuable, List<Issue>> rawIssuesPerResource, TriggerType triggerType,
    IProgressMonitor monitor) {
    if (triggerType.shouldUpdateProjectIssuesSync(rawIssuesPerResource.size())) {
      ServerConfiguration serverConfiguration = server.getConfig();
      ConnectedSonarLintEngine engine = server.getEngine();
      SonarLintLogger.get().debug("Download server issues for project " + getProject().getName());
      engine.downloadServerIssues(serverConfiguration, binding.projectKey());
    }
    super.trackIssues(docPerFile, rawIssuesPerResource, triggerType, monitor);
    if (triggerType.shouldUpdateFileIssuesAsync()) {
      List<ISonarLintIssuable> filesWithAtLeastOneIssue = filesWithAtLeastOneIssue(rawIssuesPerResource);
      if (!filesWithAtLeastOneIssue.isEmpty()) {
        trackServerIssuesAsync(server, filesWithAtLeastOneIssue, docPerFile, triggerType);
      }
    }
  }

  @Override
  protected Collection<Trackable> trackFileIssues(ISonarLintFile file, List<Trackable> trackables, IssueTracker issueTracker, TriggerType triggerType, int totalTrackedFiles) {
    Collection<Trackable> tracked = super.trackFileIssues(file, trackables, issueTracker, triggerType, totalTrackedFiles);
    if (!tracked.isEmpty()) {
      tracked = trackServerIssuesSync(server, file, tracked, triggerType.shouldUpdateFileIssuesSync(totalTrackedFiles));
    }
    return tracked;

  }

  private static List<ISonarLintIssuable> filesWithAtLeastOneIssue(Map<ISonarLintIssuable, List<Issue>> rawIssuesPerResource) {
    return rawIssuesPerResource.entrySet().stream()
      .filter(e -> !e.getValue().isEmpty())
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  private void trackServerIssuesAsync(Server server, Collection<ISonarLintIssuable> resources, Map<ISonarLintFile, IDocument> docPerFile, TriggerType triggerType) {
    ServerConfiguration serverConfiguration = server.getConfig();
    ConnectedSonarLintEngine engine = server.getEngine();
    SonarLintCorePlugin.getInstance().getServerIssueUpdater().updateAsync(serverConfiguration, engine, getProject(), binding,
      resources,
      docPerFile, triggerType);
  }

  private Collection<Trackable> trackServerIssuesSync(Server server, ISonarLintFile file, Collection<Trackable> tracked, boolean updateServerIssues) {
    ServerConfiguration serverConfiguration = server.getConfig();
    ConnectedSonarLintEngine engine = server.getEngine();
    List<ServerIssue> serverIssues;
    if (updateServerIssues) {
      serverIssues = ServerIssueUpdater.fetchServerIssues(serverConfiguration, engine, binding, file);
    } else {
      serverIssues = engine.getServerIssues(binding, file.getProjectRelativePath());
    }
    Collection<Trackable> serverIssuesTrackable = serverIssues.stream().map(ServerIssueTrackable::new).collect(Collectors.toList());
    return IssueTracker.matchAndTrackServerIssues(serverIssuesTrackable, tracked);
  }

}
