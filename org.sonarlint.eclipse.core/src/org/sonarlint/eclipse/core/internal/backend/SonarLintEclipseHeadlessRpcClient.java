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
package org.sonarlint.eclipse.core.internal.backend;

import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.AnalysisRequirementNotifications;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.jobs.IssuesMarkerUpdateJob;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.internal.vcs.VcsService;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.client.ConfigScopeNotFoundException;
import org.sonarsource.sonarlint.core.rpc.client.ConnectionNotFoundException;
import org.sonarsource.sonarlint.core.rpc.client.SonarLintCancelChecker;
import org.sonarsource.sonarlint.core.rpc.client.SonarLintRpcClientDelegate;
import org.sonarsource.sonarlint.core.rpc.protocol.client.event.DidReceiveServerHotspotEvent;
import org.sonarsource.sonarlint.core.rpc.protocol.client.http.GetProxyPasswordAuthenticationResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.client.http.ProxyDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.http.X509CertificateDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.issue.RaisedIssueDto;
import org.sonarsource.sonarlint.core.rpc.protocol.client.plugin.DidSkipLoadingPluginParams.SkipReason;
import org.sonarsource.sonarlint.core.rpc.protocol.client.telemetry.TelemetryClientLiveAttributesResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.common.ClientFileDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Either;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Language;
import org.sonarsource.sonarlint.core.rpc.protocol.common.TokenDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.UsernamePasswordDto;

/**
 * Headless part of the client
 *
 */
public abstract class SonarLintEclipseHeadlessRpcClient implements SonarLintRpcClientDelegate {

  @Override
  public Path getBaseDir(String configurationScopeId) throws ConfigScopeNotFoundException {
    var project = SonarLintUtils.resolveProject(configurationScopeId);
    var projectLocation = project.getResource().getLocation();
    // In some unfrequent cases the project may be virtual and don't have physical location
    // so fallback to use analysis work dir that was created before
    var analysisWorkDir = project.getWorkingDir().resolve("sonarlint");
    return projectLocation != null ? projectLocation.toFile().toPath() : analysisWorkDir;
  }

  @Override
  public List<ClientFileDto> listFiles(String configScopeId) throws ConfigScopeNotFoundException {
    var project = SonarLintUtils.resolveProject(configScopeId);

    var files = new ArrayList<>(project.files().stream()
      .map(slFile -> FileSystemSynchronizer.toFileDto(slFile, new NullProgressMonitor()))
      .filter(Objects::nonNull)
      .collect(Collectors.toList()));

    // If the project is in a hierarchy, also provide the ".sonarlint/*.json" files from the root project if possible.
    // If there are different hierarchical systems in place (e.g. Maven / Gradle) we provide all of them!
    var rootProjects = new HashSet<ISonarLintProject>();
    for (var projectHistoryProvider : SonarLintExtensionTracker.getInstance().getProjectHierarchyProviders()) {
      if (projectHistoryProvider.partOfHierarchy(project)) {
        var rootProject = projectHistoryProvider.getRootProject(project);
        if (rootProject != null && !project.equals(rootProject)) {
          rootProjects.add(rootProject);
        }
      }
    }

    // For root project in root projects add the files to "files"
    for (var rootProject : rootProjects) {
      FileSystemSynchronizer.getSonarLintJsonFiles(rootProject).stream()
        .forEach(slFile -> {
          var dto = FileSystemSynchronizer.toFileDto(slFile, null);
          if (dto != null) {
            files.add(FileSystemSynchronizer.toSubProjectFileDto(project, dto));
          }
        });
    }

    return files;
  }

  protected ConnectionFacade resolveConnection(String connectionId) throws ConnectionNotFoundException {
    var connectionOpt = SonarLintCorePlugin.getConnectionManager().findById(connectionId);
    if (connectionOpt.isEmpty()) {
      SonarLintLogger.get().debug("Unable to resolve connection: " + connectionId);
      throw new ConnectionNotFoundException();
    }
    return connectionOpt.get();
  }

  /** There is no UsernamePasswordDto available in SonarQube for Eclipse anymore, only token authentication! */
  @Override
  public Either<TokenDto, UsernamePasswordDto> getCredentials(String connectionId) throws ConnectionNotFoundException {
    var connection = resolveConnection(connectionId);
    return Either.forLeft(connection.getCredentials());
  }

  @Override
  public List<ProxyDto> selectProxies(URI uri) {
    var proxyService = SonarLintCorePlugin.getInstance().getProxyService();
    if (proxyService == null) {
      return List.of();
    }
    var proxyDataForHost = proxyService.select(uri);
    return Arrays.asList(proxyDataForHost).stream()
      .map(proxyData -> {
        var proxyType = IProxyData.SOCKS_PROXY_TYPE.equals(proxyData.getType()) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        return new ProxyDto(proxyType, proxyData.getHost(), proxyData.getPort());
      }).collect(Collectors.toList());
  }

  @Override
  public GetProxyPasswordAuthenticationResponse getProxyPasswordAuthentication(String host, int port, String protocol, String prompt, String scheme, URL targetHost) {
    var proxyService = SonarLintCorePlugin.getInstance().getProxyService();
    if (proxyService == null) {
      return new GetProxyPasswordAuthenticationResponse(null, null);
    }
    IProxyData[] proxyDataForHost;
    try {
      proxyDataForHost = proxyService.select(targetHost.toURI());
      return Arrays.asList(proxyDataForHost).stream()
        .findFirst()
        .map(proxyData -> new GetProxyPasswordAuthenticationResponse(proxyData.getUserId(), proxyData.getPassword()))
        .orElse(new GetProxyPasswordAuthenticationResponse(null, null));
    } catch (URISyntaxException e) {
      SonarLintLogger.get().error("Invalid URL: " + targetHost);
    }
    return new GetProxyPasswordAuthenticationResponse(null, null);
  }

  @Override
  public boolean checkServerTrusted(List<X509CertificateDto> chain, String authType) {
    return false;
  }

  @Override
  public void didReceiveServerHotspotEvent(DidReceiveServerHotspotEvent params) {
    // No Hotspots in SLE
  }

  @Override
  public void didSynchronizeConfigurationScopes(Set<String> configurationScopeIds) {
    // After a sync happened on backend side, we can refresh the project list
    var allAffectedConnections = configurationScopeIds.stream()
      .map(SonarLintUtils::tryResolveProject)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .distinct()
      .map(SonarLintCorePlugin.getConnectionManager()::resolveBinding)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .map(t -> t.getConnectionFacade())
      .distinct();
    allAffectedConnections.forEach(c -> c.getAndCacheAllSonarProjects());
  }

  @Override
  public boolean matchProjectBranch(String configurationScopeId, String branchNameToMatch, SonarLintCancelChecker cancelChecker)
    throws ConfigScopeNotFoundException {
    var project = SonarLintUtils.resolveProject(configurationScopeId);
    return branchNameToMatch.equals(VcsService.matchSonarProjectBranch(project, branchNameToMatch, Set.of(branchNameToMatch)));
  }

  @Override
  public String matchSonarProjectBranch(String configurationScopeId, String mainBranchName, Set<String> allBranchesNames, SonarLintCancelChecker cancelChecker)
    throws ConfigScopeNotFoundException {
    var project = SonarLintUtils.resolveProject(configurationScopeId);
    return VcsService.matchSonarProjectBranch(project, mainBranchName, allBranchesNames);
  }

  @Override
  public void didChangeMatchedSonarProjectBranch(String configScopeId, String newMatchedBranchName) {
    SonarLintUtils.tryResolveProject(configScopeId)
      .ifPresent(project -> VcsService.updateCachedMatchedSonarProjectBranch(project, newMatchedBranchName));
  }

  @Override
  public TelemetryClientLiveAttributesResponse getTelemetryLiveAttributes() {
    return new TelemetryClientLiveAttributesResponse(Map.of());
  }

  @Override
  public void didDetectSecret(String configScopeId) {
    ISonarLintProject project;
    try {
      project = SonarLintUtils.resolveProject(configScopeId);
    } catch (ConfigScopeNotFoundException err) {
      return;
    }
    SonarLintNotifications.get().showNotificationIfFirstSecretDetected(project);
  }

  @Override
  public void raiseIssues(String configurationScopeId, Map<URI, List<RaisedIssueDto>> issuesByFileUri, boolean isIntermediatePublication, @Nullable UUID analysisId) {
    ISonarLintProject project;
    try {
      project = SonarLintUtils.resolveProject(configurationScopeId);
    } catch (ConfigScopeNotFoundException err) {
      return;
    }

    if (isIntermediatePublication) {
      return;
    }

    // Due to the AnalysisTracker using a ConcurrentHashMap, we have to explicitly check that the key ("analysisId") is
    // not null before trying to get the value associated to this key.
    var currentAnalysis = analysisId == null ? null : RunningAnalysesTracker.get().getById(analysisId);
    if (currentAnalysis != null) {
      // For all the file URIs that might not be present in "issuesByFileUri" (maybe due to issue removed, no issues
      // present before or afterwards), we still have to include them! Otherwise situations like server-sent events for
      // Quality Profile changed (deactivated rules) doesn't work anymore!
      var fileURIs = currentAnalysis.getFileURIs();
      for (var fileURI : fileURIs) {
        issuesByFileUri.computeIfAbsent(fileURI, k -> Collections.<RaisedIssueDto>emptyList());
      }

      RunningAnalysesTracker.get().finish(currentAnalysis);
    }

    // When no analysisId provided or no analysis can be found we assume that this is some form of update triggered by
    // SonarLint Core. In this case we handle them as "on-the-fly" markers as "report" markers cannot be updated, they
    // show an immutable state.
    final var issuesAreOnTheFly = currentAnalysis == null || currentAnalysis.getTriggerType().isOnTheFly();

    new IssuesMarkerUpdateJob(project, issuesByFileUri, issuesAreOnTheFly).schedule();
  }

  @Override
  public void didSkipLoadingPlugin(String configurationScopeId, Language language, SkipReason reason, String minVersion, @Nullable String currentVersion) {
    SonarLintUtils.tryResolveProject(configurationScopeId)
      .ifPresent(project -> AnalysisRequirementNotifications.notifyOnceForSkippedPlugins(language, reason, minVersion, currentVersion));
  }

}
