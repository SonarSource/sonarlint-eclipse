/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IAnalysisConfigurator;
import org.sonarlint.eclipse.core.analysis.IFileLanguageProvider;
import org.sonarlint.eclipse.core.analysis.IPostAnalysisContext;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.core.internal.tracking.IssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.RawIssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueUpdater;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;
import org.sonarsource.sonarlint.core.client.api.exceptions.CanceledException;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.util.FileUtils;

import static java.text.MessageFormat.format;
import static org.sonarlint.eclipse.core.internal.utils.StringUtils.trimToNull;

public class AnalyzeProjectJob extends AbstractSonarProjectJob {
  private final List<SonarLintProperty> extraProps;
  private final Map<ISonarLintFile, IDocument> filesToAnalyze;
  private final Collection<ISonarLintFile> excludedFiles;
  private final Collection<RuleKey> excludedRules;
  private final TriggerType triggerType;

  public AnalyzeProjectJob(AnalyzeProjectRequest request) {
    super(jobTitle(request), request.getProject());
    this.extraProps = PreferencesUtils.getExtraPropertiesForLocalAnalysis(request.getProject());
    this.filesToAnalyze = request.getFilesToAnalyze()
      .stream()
      .collect(HashMap::new, (m, fWithDoc) -> m.put(fWithDoc.getFile(), fWithDoc.getDocument()), HashMap::putAll);
    this.excludedFiles = request.getExcludedFiles();
    this.excludedRules = PreferencesUtils.getExcludedRules();
    this.triggerType = request.getTriggerType();
  }

  private static String jobTitle(AnalyzeProjectRequest request) {
    if (request.getFilesToAnalyze().size() == 1) {
      return "SonarLint analysis of file " + request.getFilesToAnalyze().iterator().next().getFile().getName();
    }
    return format("SonarLint analysis of project {0} ({1} files to analyze, {2} excluded)", request.getProject().getName(), request.getFilesToAnalyze().size(),
      request.getExcludedFiles().size());
  }

  @Override
  protected IStatus doRun(final IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    long startTime = System.currentTimeMillis();
    SonarLintLogger.get().debug("Trigger: " + triggerType.name());
    SonarLintLogger.get().debug("Clear markers on " + excludedFiles.size() + " excluded files");
    excludedFiles.forEach(SonarLintMarkerUpdater::clearMarkers);

    if (filesToAnalyze.isEmpty()) {
      return Status.OK_STATUS;
    }

    // Analyze
    SonarLintLogger.get().info(this.getName() + "...");
    Path analysisWorkDir = null;
    try {
      // Configure
      Map<String, String> mergedExtraProps = new LinkedHashMap<>();
      Collection<ProjectConfigurator> usedDeprecatedConfigurators = configureDeprecated(getProject(), filesToAnalyze.keySet(), mergedExtraProps, monitor);

      analysisWorkDir = Files.createTempDirectory(getProject().getWorkingDir(), "sonarlint");
      List<ClientInputFile> inputFiles = buildInputFiles(analysisWorkDir, filesToAnalyze);
      Collection<IAnalysisConfigurator> usedConfigurators = configure(getProject(), inputFiles, mergedExtraProps, analysisWorkDir, monitor);

      for (SonarLintProperty sonarProperty : extraProps) {
        mergedExtraProps.put(sonarProperty.getName(), sonarProperty.getValue());
      }

      if (!inputFiles.isEmpty()) {
        Server server = null;
        if (getProject().isBound()) {
          server = (Server) SonarLintCorePlugin.getServersManager().getServer(getProjectConfig().getServerId());
          if (server == null) {
            return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID,
              "Project '" + getProject().getName() + "' is bound to an unknown SonarQube server: '" + getProjectConfig().getServerId()
                + "'. Please fix project binding or unbind project.");
          }
        }

        runAnalysisAndUpdateMarkers(server, filesToAnalyze, monitor, mergedExtraProps, inputFiles, analysisWorkDir, excludedRules);
      }

      analysisCompleted(usedDeprecatedConfigurators, usedConfigurators, mergedExtraProps, monitor);
      SonarLintCorePlugin.getAnalysisListenerManager().notifyListeners();
      SonarLintLogger.get().debug(String.format("Done in %d ms", System.currentTimeMillis() - startTime));
    } catch (CanceledException e) {
      return Status.CANCEL_STATUS;
    } catch (Exception e) {
      SonarLintLogger.get().error("Error during execution of SonarLint analysis", e);
      return new Status(Status.WARNING, SonarLintCorePlugin.PLUGIN_ID, "Error when executing SonarLint analysis", e);
    } finally {
      if (analysisWorkDir != null) {
        FileUtils.deleteRecursively(analysisWorkDir);
      }
    }

    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

  private void runAnalysisAndUpdateMarkers(@Nullable Server server, Map<ISonarLintFile, IDocument> docPerFiles, final IProgressMonitor monitor,
    Map<String, String> mergedExtraProps, List<ClientInputFile> inputFiles, Path analysisWorkDir, Collection<RuleKey> excludedRules) throws CoreException {
    StandaloneAnalysisConfiguration config;
    IPath projectLocation = getProject().getResource().getLocation();
    // In some unfrequent cases the project may be virtual and don't have physical location
    // so fallback to use analysis work dir
    Path projectBaseDir = projectLocation != null ? projectLocation.toFile().toPath() : analysisWorkDir;
    if (server != null) {
      SonarLintLogger.get().debug("Connected mode (using configuration of '" + getProjectConfig().getModuleKey() + "' in server '" + getProjectConfig().getServerId() + "')");
      config = new ConnectedAnalysisConfiguration(trimToNull(getProjectConfig().getModuleKey()), projectBaseDir, getProject().getWorkingDir(), inputFiles, mergedExtraProps);
    } else {
      SonarLintLogger.get().debug("Standalone mode (project not bound)");
      config = new StandaloneAnalysisConfiguration(projectBaseDir, getProject().getWorkingDir(), inputFiles, mergedExtraProps, excludedRules);
    }

    Map<ISonarLintIssuable, List<Issue>> issuesPerResource = new LinkedHashMap<>();
    filesToAnalyze.keySet().forEach(slFile -> issuesPerResource.put(slFile, new ArrayList<>()));

    long start = System.currentTimeMillis();
    AnalysisResults result = run(server, config, issuesPerResource, monitor);
    if (!monitor.isCanceled() && result != null) {
      updateMarkers(server, docPerFiles, issuesPerResource, result, triggerType, monitor);
      updateTelemetry(inputFiles, start);
    }
  }

  private static void updateTelemetry(List<ClientInputFile> inputFiles, long start) {
    SonarLintTelemetry telemetry = SonarLintCorePlugin.getTelemetry();
    if (inputFiles.size() == 1) {
      telemetry.analysisDoneOnSingleFile(getExtension(inputFiles.iterator().next()), (int) (System.currentTimeMillis() - start));
    } else {
      telemetry.analysisDoneOnMultipleFiles();
    }
  }

  private static String getExtension(ClientInputFile next) {
    String path = next.getPath();
    int lastDot = path.lastIndexOf('.');
    return lastDot >= 0 ? path.substring(lastDot) : "";
  }

  private static List<ClientInputFile> buildInputFiles(Path tempDirectory, final Map<ISonarLintFile, IDocument> filesToAnalyze) {
    List<ClientInputFile> inputFiles = new ArrayList<>(filesToAnalyze.size());

    for (final Map.Entry<ISonarLintFile, IDocument> fileWithDoc : filesToAnalyze.entrySet()) {
      ISonarLintFile file = fileWithDoc.getKey();
      String language = tryDetectLanguage(file);
      boolean isTest = TestFileClassifier.get().isTest(file);
      ClientInputFile inputFile = new EclipseInputFile(isTest, file, tempDirectory, fileWithDoc.getValue(), language);
      inputFiles.add(inputFile);
    }
    return inputFiles;
  }

  @CheckForNull
  private static String tryDetectLanguage(ISonarLintFile file) {
    String language = null;
    for (IFileLanguageProvider languageProvider : SonarLintCorePlugin.getExtensionTracker().getLanguageProviders()) {
      String detectedLanguage = languageProvider.language(file);
      if (detectedLanguage != null) {
        if (language == null) {
          language = detectedLanguage;
        } else if (!language.equals(detectedLanguage)) {
          SonarLintLogger.get().error("Conflicting languages detected for file " + file.getName() + ". " + language + " and " + detectedLanguage);
        }
      }
    }
    return language;
  }

  private static Collection<ProjectConfigurator> configureDeprecated(final ISonarLintProject project, Collection<ISonarLintFile> filesToAnalyze,
    final Map<String, String> extraProperties,
    final IProgressMonitor monitor) {
    Collection<ProjectConfigurator> usedConfigurators = new ArrayList<>();
    if (project.getResource() instanceof IProject) {
      ProjectConfigurationRequest configuratorRequest = new ProjectConfigurationRequest((IProject) project.getResource(),
        filesToAnalyze.stream()
          .map(f -> (f.getResource() instanceof IFile) ? (IFile) f.getResource() : null)
          .filter(Objects::nonNull)
          .collect(Collectors.toList()),
        extraProperties);
      Collection<ProjectConfigurator> configurators = SonarLintCorePlugin.getExtensionTracker().getConfigurators();
      for (ProjectConfigurator configurator : configurators) {
        if (configurator.canConfigure((IProject) project.getResource())) {
          configurator.configure(configuratorRequest, monitor);
          usedConfigurators.add(configurator);
        }
      }
    }

    return usedConfigurators;
  }

  private static Collection<IAnalysisConfigurator> configure(final ISonarLintProject project, List<ClientInputFile> filesToAnalyze,
    final Map<String, String> extraProperties, Path tempDir, final IProgressMonitor monitor) {
    Collection<IAnalysisConfigurator> usedConfigurators = new ArrayList<>();
    Collection<IAnalysisConfigurator> configurators = SonarLintCorePlugin.getExtensionTracker().getAnalysisConfigurators();
    DefaultPreAnalysisContext context = new DefaultPreAnalysisContext(project, extraProperties, filesToAnalyze, tempDir);
    for (IAnalysisConfigurator configurator : configurators) {
      if (configurator.canConfigure(project)) {
        configurator.configure(context, monitor);
        usedConfigurators.add(configurator);
      }
    }

    return usedConfigurators;
  }

  private void updateMarkers(@Nullable Server server, Map<ISonarLintFile, IDocument> docPerFile, Map<ISonarLintIssuable, List<Issue>> issuesPerResource, AnalysisResults result,
    TriggerType triggerType, final IProgressMonitor monitor)
    throws CoreException {
    Set<ISonarLintFile> failedFiles = result.failedAnalysisFiles().stream().map(ClientInputFile::<ISonarLintFile>getClientObject).collect(Collectors.toSet());
    Map<ISonarLintIssuable, List<Issue>> successfulFiles = issuesPerResource.entrySet().stream()
      .filter(e -> !failedFiles.contains(e.getKey()))
      // TODO handle non-file-level issues
      .filter(e -> e.getKey() instanceof ISonarLintFile)
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    trackIssues(server, docPerFile, successfulFiles, triggerType, monitor);
  }

  private void trackIssues(@Nullable Server server, Map<ISonarLintFile, IDocument> docPerFile, Map<ISonarLintIssuable, List<Issue>> rawIssuesPerResource, TriggerType triggerType,
    final IProgressMonitor monitor) {

    String localModuleKey = getProject().getName();

    if (server != null && triggerType.shouldUpdateProjectIssuesSync(rawIssuesPerResource.size())) {
      ServerConfiguration serverConfiguration = server.getConfig();
      ConnectedSonarLintEngine engine = server.getEngine();
      SonarLintLogger.get().debug("Download server issues for project " + getProject().getName());
      engine.downloadServerIssues(serverConfiguration, getProjectConfig().getModuleKey());
    }

    for (Map.Entry<ISonarLintIssuable, List<Issue>> entry : rawIssuesPerResource.entrySet()) {
      if (monitor.isCanceled()) {
        return;
      }
      ISonarLintFile resource = (ISonarLintFile) entry.getKey();
      IDocument documentOrNull = docPerFile.get((ISonarLintFile) resource);
      final IDocument documentNotNull;
      if (documentOrNull == null) {
        documentNotNull = resource.getDocument();
      } else {
        documentNotNull = documentOrNull;
      }
      List<Issue> rawIssues = entry.getValue();
      List<Trackable> trackables = rawIssues.stream().map(issue -> transform(issue, resource, documentNotNull)).collect(Collectors.toList());
      IssueTracker issueTracker = SonarLintCorePlugin.getOrCreateIssueTracker(getProject(), localModuleKey);
      String relativePath = resource.getProjectRelativePath();
      Collection<Trackable> tracked = issueTracker.matchAndTrackAsNew(relativePath, trackables);
      if (server != null && !tracked.isEmpty()) {
        tracked = trackServerIssuesSync(server, resource, tracked, triggerType.shouldUpdateFileIssuesSync(rawIssuesPerResource.size()));
      }
      ISchedulingRule markerRule = ResourcesPlugin.getWorkspace().getRuleFactory().markerRule(resource.getResource());
      try {
        getJobManager().beginRule(markerRule, monitor);
        SonarLintMarkerUpdater.createOrUpdateMarkers(resource, documentNotNull, tracked, triggerType, documentOrNull != null);
      } finally {
        getJobManager().endRule(markerRule);
      }
      // Now that markerId are set, store issues in cache
      issueTracker.updateCache(relativePath, tracked);
    }

    if (server != null && triggerType.shouldUpdateFileIssuesAsync()) {
      List<ISonarLintIssuable> filesWithAtLeastOneIssue = filesWithAtLeastOneIssue(rawIssuesPerResource);
      if (!filesWithAtLeastOneIssue.isEmpty()) {
        trackServerIssuesAsync(server, filesWithAtLeastOneIssue, docPerFile, triggerType);
      }
    }
  }

  private static List<ISonarLintIssuable> filesWithAtLeastOneIssue(Map<ISonarLintIssuable, List<Issue>> rawIssuesPerResource) {
    return rawIssuesPerResource.entrySet().stream()
      .filter(e -> !e.getValue().isEmpty())
      .map(Map.Entry::getKey)
      .collect(Collectors.toList());
  }

  private static RawIssueTrackable transform(Issue issue, ISonarLintFile resource, IDocument document) {
    Integer startLine = issue.getStartLine();
    if (startLine == null) {
      return new RawIssueTrackable(issue);
    }
    TextRange textRange = new TextRange(startLine, issue.getStartLineOffset(), issue.getEndLine(), issue.getEndLineOffset());
    String textRangeContent = readTextRangeContent(resource, document, textRange);
    String lineContent = readLineContent(resource, document, startLine);
    return new RawIssueTrackable(issue, textRange, textRangeContent, lineContent);
  }

  @CheckForNull
  private static String readTextRangeContent(ISonarLintFile resource, IDocument document, TextRange textRange) {
    Position position = MarkerUtils.getPosition(document, textRange);
    if (position != null) {
      try {
        return document.get(position.getOffset(), position.getLength());
      } catch (BadLocationException e) {
        SonarLintLogger.get().error("failed to get text range content of resource " + resource.getName(), e);
      }
    }
    return null;
  }

  @CheckForNull
  private static String readLineContent(ISonarLintFile resource, IDocument document, int startLine) {
    Position position = MarkerUtils.getPosition(document, startLine);
    if (position != null) {
      try {
        return document.get(position.getOffset(), position.getLength());
      } catch (BadLocationException e) {
        SonarLintLogger.get().error("Failed to get line content of file " + resource.getName(), e);
      }
    }
    return null;
  }

  private Collection<Trackable> trackServerIssuesSync(Server server, ISonarLintFile resource, Collection<Trackable> tracked, boolean updateServerIssues) {
    ServerConfiguration serverConfiguration = server.getConfig();
    ConnectedSonarLintEngine engine = server.getEngine();
    List<ServerIssue> serverIssues;
    if (updateServerIssues) {
      serverIssues = ServerIssueUpdater.fetchServerIssues(serverConfiguration, engine, getProjectConfig().getModuleKey(), resource);
    } else {
      serverIssues = engine.getServerIssues(getProjectConfig().getModuleKey(), resource.getProjectRelativePath());
    }
    Collection<Trackable> serverIssuesTrackable = serverIssues.stream().map(ServerIssueTrackable::new).collect(Collectors.toList());
    return IssueTracker.matchAndTrackServerIssues(serverIssuesTrackable, tracked);
  }

  private void trackServerIssuesAsync(Server server, Collection<ISonarLintIssuable> resources, Map<ISonarLintFile, IDocument> docPerFile, TriggerType triggerType) {
    ServerConfiguration serverConfiguration = server.getConfig();
    ConnectedSonarLintEngine engine = server.getEngine();
    String localModuleKey = getProject().getName();
    SonarLintCorePlugin.getInstance().getServerIssueUpdater().updateAsync(serverConfiguration, engine, getProject(), localModuleKey, getProjectConfig().getModuleKey(), resources,
      docPerFile, triggerType);
  }

  private static void analysisCompleted(Collection<ProjectConfigurator> usedDeprecatedConfigurators, Collection<IAnalysisConfigurator> usedConfigurators,
    Map<String, String> properties, final IProgressMonitor monitor) {
    Map<String, String> unmodifiableMap = Collections.unmodifiableMap(properties);
    for (ProjectConfigurator p : usedDeprecatedConfigurators) {
      p.analysisComplete(unmodifiableMap, monitor);
    }
    IPostAnalysisContext context = new IPostAnalysisContext() {

      @Override
      public ISonarLintProject getProject() {
        return getProject();
      }

      @Override
      public Map<String, String> getAnalysisProperties() {
        return unmodifiableMap;
      }
    };
    for (IAnalysisConfigurator p : usedConfigurators) {
      p.analysisComplete(context, monitor);
    }

  }

  @CheckForNull
  public AnalysisResults run(@Nullable IServer server, final StandaloneAnalysisConfiguration analysisConfig,
    final Map<ISonarLintIssuable, List<Issue>> issuesPerResource, IProgressMonitor monitor) {
    SonarLintLogger.get().debug("Starting analysis with configuration:\n" + analysisConfig.toString());
    SonarLintIssueListener issueListener = new SonarLintIssueListener(getProject(), issuesPerResource);
    AnalysisResults result;
    if (server != null) {
      result = server.runAnalysis((ConnectedAnalysisConfiguration) analysisConfig, issueListener, monitor);
    } else {
      StandaloneSonarLintClientFacade facadeToUse = SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade();
      result = facadeToUse.runAnalysis(analysisConfig, issueListener, monitor);
    }
    SonarLintLogger.get().info("Found " + issueListener.getIssueCount() + " issue(s)");
    return result;
  }
}
