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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.StoragePathManager;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.nodejs.NodeJsService;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.core.internal.utils.DurationUtils;
import org.sonarlint.eclipse.core.internal.utils.JavaRuntimeUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.internal.vcs.VcsService;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.rpc.client.SloopLauncher;
import org.sonarsource.sonarlint.core.rpc.client.SonarLintRpcClientDelegate;
import org.sonarsource.sonarlint.core.rpc.protocol.SonarLintRpcServer;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesAndTrackParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.binding.GetSharedConnectedModeConfigFileParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.binding.GetSharedConnectedModeConfigFileResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.branch.DidVcsRepositoryChangeParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.config.DidChangeCredentialsParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.projects.GetAllProjectsParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.connection.projects.SonarProjectDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.ClientConstantInfoDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.FeatureFlagsDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.HttpConfigurationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.InitializeParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.JsTsRequirementsDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.LanguageSpecificRequirements;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.SonarCloudAlternativeEnvironmentDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.SonarQubeCloudRegionDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.SslConfigurationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.TelemetryClientConstantAttributesDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.AddIssueCommentParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.ChangeIssueStatusParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.CheckAnticipatedStatusChangeSupportedParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.CheckAnticipatedStatusChangeSupportedResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.GetEffectiveIssueDetailsParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.GetEffectiveIssueDetailsResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.ReopenIssueParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.ReopenIssueResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.issue.ResolutionStatus;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.newcode.GetNewCodeDefinitionParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.newcode.GetNewCodeDefinitionResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.GetStandaloneRuleDescriptionParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.GetStandaloneRuleDescriptionResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.rules.ListAllStandaloneRulesDefinitionsResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.ListAllParams;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.tracking.ListAllResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.common.Language;
import org.sonarsource.sonarlint.core.rpc.protocol.common.SonarCloudRegion;

import static java.util.Objects.requireNonNull;
import static org.sonarlint.eclipse.core.internal.utils.StringUtils.defaultString;

public class SonarLintBackendService {
  private static final SonarLintBackendService INSTANCE = new SonarLintBackendService();

  // Properties / options passed to SonarLint Core
  private static final String SONARLINT_DEBUG_RPC = "sonarlint.debug.rpc";
  private static final String SONARLINT_DEBUG_ACTIVE_RULES = "sonarlint.debug.active.rules";
  private static final String SONARLINT_JVM_OPTS = "SONARLINT_JVM_OPTS";

  @Nullable
  private static ConfigScopeSynchronizer configScopeSynchronizer;
  @Nullable
  private static ConnectionSynchronizer connectionSynchronizer;
  @Nullable
  private static FileSystemSynchronizer fileSystemSynchronizer;
  @Nullable
  private SonarLintRpcServer backend;

  private Job initJob;

  private SloopLauncher sloopLauncher;
  private HttpConfigurationDto httpConfiguration;

  public static SonarLintBackendService get() {
    return INSTANCE;
  }

  public synchronized void init(SonarLintRpcClientDelegate client) {
    if (sloopLauncher != null) {
      throw new IllegalStateException("Backend is already initialized");
    }
    sloopLauncher = new SloopLauncher(client);

    httpConfiguration = new HttpConfigurationDto(
      new SslConfigurationDto(getPathProperty("sonarlint.ssl.trustStorePath"),
        System.getProperty("sonarlint.ssl.trustStorePassword"),
        System.getProperty("sonarlint.ssl.trustStoreType"),
        getPathProperty("sonarlint.ssl.keyStorePath"),
        System.getProperty("sonarlint.ssl.keyStorePassword"),
        System.getProperty("sonarlint.ssl.keyStoreType")),
      DurationUtils.getTimeoutProperty("sonarlint.http.connectTimeout"),
      DurationUtils.getTimeoutProperty("sonarlint.http.socketTimeout"),
      DurationUtils.getTimeoutProperty("sonarlint.http.connectionRequestTimeout"),
      DurationUtils.getTimeoutProperty("sonarlint.http.responseTimeout"));

    initJob = new Job("Backend initialization") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        SonarLintLogger.get().debug("Initializing SonarLint backend...");
        try {
          var sloopJarUrls = SonarLintCorePlugin.getInstance().getBundle().findEntries("/sloop/lib", "sonarlint-core-*", false);
          if (!sloopJarUrls.hasMoreElements()) {
            throw new IllegalStateException("Unable to locate the Sloop installation");
          }
          var sloopJarUrl = FileLocator.toFileURL(sloopJarUrls.nextElement());
          var sloopJarPath = new File(sloopJarUrl.getFile()).toPath();
          SonarLintLogger.get().debug("SonarLint Core Jar archive located at " + sloopJarPath);
          var sloopBasedir = sloopJarPath.getParent().getParent();
          SonarLintLogger.get().debug("Sloop located in " + sloopBasedir);

          var javaRuntimeInformation = JavaRuntimeUtils.getJavaRuntime();
          var javaRuntimePath = javaRuntimeInformation.getPath();
          switch (javaRuntimeInformation.getProvider()) {
            case SELF_MANAGED:
              SonarLintLogger.get().info("Using self-managed Java installation");
              fixExecutablePermissions();
              break;
            case ECLIPSE_MANAGED:
              SonarLintLogger.get().info("Using Java installation of Eclipse");
              break;
            case SONARLINT_BUNDLED:
              SonarLintLogger.get().info("Using Java installation of SonarLint");
          }

          var sloop = sloopLauncher.start(sloopBasedir, javaRuntimePath, passSloopJvmOpts());
          sloop.onExit().thenAccept(SonarLintBackendService::onSloopExit);
          backend = sloop.getRpcServer();

          var embeddedPluginPaths = PluginPathHelper.getEmbeddedPluginPaths();
          embeddedPluginPaths.stream().forEach(p -> SonarLintLogger.get().debug("  - " + p));

          Map<String, Path> embeddedPlugins = new HashMap<>();
          embeddedPlugins.put("javascript", requireNonNull(PluginPathHelper.findEmbeddedJsPlugin(), "JS/TS plugin not found"));
          embeddedPlugins.put("web", requireNonNull(PluginPathHelper.findEmbeddedHtmlPlugin(), "HTML plugin not found"));
          embeddedPlugins.put("xml", requireNonNull(PluginPathHelper.findEmbeddedXmlPlugin(), "XML plugin not found"));
          embeddedPlugins.put("text", requireNonNull(PluginPathHelper.findEmbeddedSecretsPlugin(), "Secrets plugin not found"));
          embeddedPlugins.put("cpp", requireNonNull(PluginPathHelper.findEmbeddedCFamilyPlugin(), "CFamily plugin not found"));

          var sqConnections = ConnectionSynchronizer.buildSqConnectionDtos();
          var scConnections = ConnectionSynchronizer.buildScConnectionDtos();

          // Check if telemetry was disabled via system properties (e.g. in unit / integration tests)
          var telemetryEnabled = !Boolean.parseBoolean(System.getProperty("sonarlint.telemetry.disabled", "false"));

          // Getting this information is expensive, therefore only do it once and re-use the values!
          var plugInVersion = SonarLintUtils.getPluginVersion();
          var ideVersion = SonarLintTelemetry.ideVersionForTelemetry();

          backend.initialize(new InitializeParams(
            new ClientConstantInfoDto(getIdeName(), "SonarQube for IDE (SonarLint) - Eclipse " + plugInVersion + " - " + ideVersion),
            new TelemetryClientConstantAttributesDto("eclipse", "SonarLint Eclipse", plugInVersion, ideVersion, Map.of()),
            httpConfiguration,
            getSonarCloudAlternativeEnvironment(),
            new FeatureFlagsDto(true, true, true, true, false, true, true, true, telemetryEnabled, true, true),
            StoragePathManager.getStorageDir(),
            StoragePathManager.getDefaultWorkDir(),
            Set.copyOf(embeddedPluginPaths),
            embeddedPlugins,
            SonarLintUtils.getStandaloneEnabledLanguages().stream().map(l -> Language.valueOf(l.name())).collect(Collectors.toSet()),
            SonarLintUtils.getConnectedEnabledLanguages().stream().map(l -> Language.valueOf(l.name())).collect(Collectors.toSet()),
            null,
            sqConnections,
            scConnections,
            null,
            SonarLintGlobalConfiguration.buildStandaloneRulesConfigDto(),
            SonarLintGlobalConfiguration.issuesOnlyNewCode(),
            new LanguageSpecificRequirements(new JsTsRequirementsDto(NodeJsService.getNodeJsPath(), null), null),
            false,
            null)).join();
        } catch (IOException e) {
          throw new IllegalStateException("Unable to initialize the SonarLint Backend", e);
        }
        connectionSynchronizer = new ConnectionSynchronizer(backend);
        SonarLintCorePlugin.getConnectionManager().addConnectionManagerListener(connectionSynchronizer);

        configScopeSynchronizer = new ConfigScopeSynchronizer(backend);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(configScopeSynchronizer);
        configScopeSynchronizer.init();

        fileSystemSynchronizer = new FileSystemSynchronizer(backend);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(fileSystemSynchronizer, IResourceChangeEvent.POST_CHANGE);

        VcsService.installBranchChangeListener();

        SonarLintRpcClientSupportSynchronizer.setSloopAvailability(true);

        return Status.OK_STATUS;
      }

      /**
       * When running tests, Tycho does not execute p2 directives, so we have to manually fix the permissions of the sloop executables.
       */
      private void fixExecutablePermissions() throws IOException {
        fixExecutablePermissions("/sloop/jre/bin", "java");
        fixExecutablePermissions("/sloop/jre/lib", "jspawnhelper");
        fixExecutablePermissions("/sloop/jre/lib", "jexec");
      }

      private void fixExecutablePermissions(String dir, String file) throws IOException {
        var sloopShellScriptUrls = SonarLintCorePlugin.getInstance().getBundle().findEntries(dir, file, false);
        if (sloopShellScriptUrls != null && sloopShellScriptUrls.hasMoreElements()) {
          var jreBin = FileLocator.toFileURL(sloopShellScriptUrls.nextElement());
          var jreBinPath = new File(jreBin.getFile()).toPath();
          var existingPerm = Files.getPosixFilePermissions(jreBinPath);
          if (!existingPerm.contains(PosixFilePermission.OWNER_EXECUTE)) {
            existingPerm.add(PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(jreBinPath, existingPerm);
          }
        }
      }

    };
    initJob.schedule();

  }

  /**
   *  This handles system properties that are used inside Sloop by passing them as JVM options. In addition to debug
   *  properties there is a environment variable "SONARLINT_JVM_OPTS" that could also be provided as a system property
   *  for easier configuration of the IDE.
   */
  @Nullable
  private static String passSloopJvmOpts() {
    var properties = "";

    // i) To debug the JSON RPC connection
    var debugRpc = System.getProperty(SONARLINT_DEBUG_RPC);
    if (debugRpc != null) {
      properties += "-D" + SONARLINT_DEBUG_RPC + "=" + debugRpc;
    }

    // ii) To debug the active rules
    var debugActiveRules = System.getProperty(SONARLINT_DEBUG_ACTIVE_RULES);
    if (debugActiveRules != null) {
      properties += " -D" + SONARLINT_DEBUG_ACTIVE_RULES + "=" + debugActiveRules;
    }

    // iii) JVM options that are already checked by "SloopLauncher#createCommand(...)" when coming from environment
    // variables. But it is easier to pass system properties to the IDE and therefore they are checked here as well
    var jvmOpts = System.getProperty(SONARLINT_JVM_OPTS);
    if (jvmOpts != null) {
      properties += " " + jvmOpts;
    }

    properties = properties.trim();
    return properties.isEmpty() ? null : properties;
  }

  @Nullable
  private static SonarCloudAlternativeEnvironmentDto getSonarCloudAlternativeEnvironment() {
    var sonarQubeCloudEuUrl = System.getProperty("sonarlint.internal.sonarcloud.url");
    var sonarQubeCloudEuApiUrl = System.getProperty("sonarlint.internal.sonarcloud.api.url");
    var sonarQubeCloudEuWebSocketUrl = System.getProperty("sonarlint.internal.sonarcloud.websocket.url");
    var sonarQubeCloudUsUrl = System.getProperty("sonarlint.internal.sonarcloud.us.url");
    var sonarQubeCloudUsApiUrl = System.getProperty("sonarlint.internal.sonarcloud.us.api.url");
    var sonarQubeCloudUsWebSocketUrl = System.getProperty("sonarlint.internal.sonarcloud.us.websocket.url");

    return new SonarCloudAlternativeEnvironmentDto(
      Map.of(
        SonarCloudRegion.EU,
        new SonarQubeCloudRegionDto(
          sonarQubeCloudEuUrl == null ? null : URI.create(sonarQubeCloudEuUrl),
          sonarQubeCloudEuApiUrl == null ? null : URI.create(sonarQubeCloudEuApiUrl),
          sonarQubeCloudEuWebSocketUrl == null ? null : URI.create(sonarQubeCloudEuWebSocketUrl)),
        SonarCloudRegion.US,
        new SonarQubeCloudRegionDto(
          sonarQubeCloudUsUrl == null ? null : URI.create(sonarQubeCloudUsUrl),
          sonarQubeCloudUsApiUrl == null ? null : URI.create(sonarQubeCloudUsApiUrl),
          sonarQubeCloudUsWebSocketUrl == null ? null : URI.create(sonarQubeCloudUsWebSocketUrl))));
  }

  @Nullable
  private static Path getPathProperty(String propertyName) {
    var property = System.getProperty(propertyName);
    return property == null ? null : Paths.get(property);
  }

  private static void onSloopExit(int exitCode) {
    if (exitCode != 0) {
      // When the exit code is 0 we accept it as a normal shutdown of the RPC server.
      SonarLintRpcClientSupportSynchronizer.setSloopAvailability(false);

      // INFO: With SLE-812 improve this to restart the backend / offer users the possibility to to so!
    }
  }

  /** This way the HTTP configuration regarding SSL can also be used inside the plug-in and not only in SLCORE */
  public HttpConfigurationDto getHttpConfiguration() {
    return requireNonNull(httpConfiguration, "SonarLint backend service needs to be initialized first");
  }

  /**
   * Inform the backend that VCS changed in a way that may affect the matched SonarProject branch
   */
  public void didVcsRepositoryChange(ISonarLintProject project) {
    getBackend()
      .getSonarProjectBranchService()
      .didVcsRepositoryChange(new DidVcsRepositoryChangeParams(ConfigScopeSynchronizer.getConfigScopeId(project)));
  }

  public void credentialsChanged(ConnectionFacade connection) {
    getBackend().getConnectionService().didChangeCredentials(new DidChangeCredentialsParams(connection.getId()));
  }

  public SonarLintRpcServer getBackend() {
    try {
      requireNonNull(initJob, "SonarLintBackendService has not been initialized").join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      Platform.getLog(SonarLintBackendService.class).error("Interrupted!", e);
    }
    return requireNonNull(backend, "SonarLintBackendService has not been initialized");
  }

  private static String getIdeName() {
    var ideName = "Eclipse";
    var product = Platform.getProduct();
    if (product != null) {
      ideName = defaultString(product.getName(), "Eclipse");
    }
    return ideName;
  }

  /** Get all the rules available in standalone mode */
  public CompletableFuture<ListAllStandaloneRulesDefinitionsResponse> getStandaloneRules() {
    return getBackend().getRulesService().listAllStandaloneRulesDefinitions();
  }

  /** Get the rules details (global configuration) */
  public GetStandaloneRuleDescriptionResponse getStandaloneRuleDetails(String ruleKey) throws InterruptedException, ExecutionException {
    return getBackend()
      .getRulesService()
      .getStandaloneRuleDetails(new GetStandaloneRuleDescriptionParams(ruleKey))
      .get();
  }

  /** Get the issue details (project configuration, maybe connected mode) */
  public GetEffectiveIssueDetailsResponse getEffectiveIssueDetails(ISonarLintProject project, UUID issueId)
    throws InterruptedException, ExecutionException {
    return getBackend()
      .getIssueService()
      .getEffectiveIssueDetails(new GetEffectiveIssueDetailsParams(ConfigScopeSynchronizer.getConfigScopeId(project), issueId))
      .get();
  }

  public synchronized void stop() {
    VcsService.removeBranchChangeListener();
    if (fileSystemSynchronizer != null) {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(fileSystemSynchronizer);
      fileSystemSynchronizer = null;
    }
    if (configScopeSynchronizer != null) {
      ResourcesPlugin.getWorkspace().removeResourceChangeListener(configScopeSynchronizer);
      configScopeSynchronizer = null;
    }
    if (connectionSynchronizer != null) {
      SonarLintCorePlugin.getConnectionManager().removeConnectionManagerListener(connectionSynchronizer);
      connectionSynchronizer = null;
    }
    if (backend != null) {
      try {
        backend.shutdown().get(10, TimeUnit.SECONDS);
      } catch (InterruptedException | ExecutionException | TimeoutException err) {
        SonarLintLogger.get().error("Unable to shutdown the SonarLint Core RPC server", err);
      }
    }
    backend = null;
  }

  /**
   *  INFO: For anticipated issues the `serverIssueKey` parameter has to be replaced with the string representation of
   *        the `TrackedIssue.id` field. This is due to SLCORE using the same method for server and anticipated issues,
   *        therefore the naming confusion!
   */
  public CompletableFuture<Void> changeIssueStatus(ISonarLintProject project, String serverIssueKey, ResolutionStatus newStatus, boolean isTaint) {
    return getBackend()
      .getIssueService()
      .changeStatus(new ChangeIssueStatusParams(ConfigScopeSynchronizer.getConfigScopeId(project), serverIssueKey, newStatus, isTaint));
  }

  /**
   *  INFO: For anticipated issues the `serverIssueKey` parameter has to be replaced with the string representation of
   *        the `TrackedIssue.id` field. This is due to SLCORE using the same method for server and anticipated issues,
   *        therefore the naming confusion!
   */
  public CompletableFuture<Void> addIssueComment(ISonarLintProject project, String serverIssueKey, String text) {
    return getBackend()
      .getIssueService()
      .addComment(new AddIssueCommentParams(ConfigScopeSynchronizer.getConfigScopeId(project), serverIssueKey, text));
  }

  public CompletableFuture<GetNewCodeDefinitionResponse> getNewCodeDefinition(ISonarLintProject project) {
    return getBackend().getNewCodeService().getNewCodeDefinition(new GetNewCodeDefinitionParams(ConfigScopeSynchronizer.getConfigScopeId(project)));
  }

  public CompletableFuture<ReopenIssueResponse> reopenIssue(ISonarLintProject project, String issueKey, Boolean isTaintVulnerability) {
    return getBackend()
      .getIssueService()
      .reopenIssue(new ReopenIssueParams(ConfigScopeSynchronizer.getConfigScopeId(project), issueKey, isTaintVulnerability));
  }

  public CompletableFuture<CheckAnticipatedStatusChangeSupportedResponse> checkAnticipatedStatusChangeSupported(ISonarLintProject project) {
    return getBackend()
      .getIssueService()
      .checkAnticipatedStatusChangeSupported(new CheckAnticipatedStatusChangeSupportedParams(ConfigScopeSynchronizer.getConfigScopeId(project)));
  }

  public CompletableFuture<List<SonarProjectDto>> getAllProjects(ConnectionFacade connectionFacade) {
    return getBackend().getConnectionService().getAllProjects(new GetAllProjectsParams(connectionFacade.toTransientDto())).thenApply(r -> r.getSonarProjects());
  }

  public CompletableFuture<ListAllResponse> listAllTaintVulnerabilities(ISonarLintProject project) {
    return getBackend().getTaintVulnerabilityTrackingService().listAll(new ListAllParams(ConfigScopeSynchronizer.getConfigScopeId(project)));
  }

  public CompletableFuture<GetSharedConnectedModeConfigFileResponse> getSharedConnectedModeConfigFileContents(ISonarLintProject project) {
    return getBackend()
      .getBindingService()
      .getSharedConnectedModeConfigFileContents(new GetSharedConnectedModeConfigFileParams(ConfigScopeSynchronizer.getConfigScopeId(project)));
  }

  public CompletableFuture<AnalyzeFilesResponse> analyzeFilesAndTrack(ISonarLintProject project, UUID analysisId, List<URI> fileURIs,
    Map<String, String> extraProps, boolean shouldFetchServerIssues, long startTime) {
    return getBackend().getAnalysisService().analyzeFilesAndTrack(
      new AnalyzeFilesAndTrackParams(ConfigScopeSynchronizer.getConfigScopeId(project), analysisId, fileURIs, extraProps, shouldFetchServerIssues, startTime));
  }
}
