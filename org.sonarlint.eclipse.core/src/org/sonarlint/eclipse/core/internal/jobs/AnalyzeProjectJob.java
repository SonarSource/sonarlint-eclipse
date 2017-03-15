/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
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
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
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
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.internal.tracking.IssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.RawIssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueUpdater;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.util.FileUtils;

import static org.sonarlint.eclipse.core.internal.utils.StringUtils.trimToNull;

public class AnalyzeProjectJob extends AbstractSonarProjectJob {
  private final List<SonarLintProperty> extraProps;
  private final AnalyzeProjectRequest request;

  static final ISchedulingRule SONARLINT_ANALYSIS_RULE = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();

  public AnalyzeProjectJob(AnalyzeProjectRequest request) {
    super(jobTitle(request), request.getProject());
    this.request = request;
    this.extraProps = PreferencesUtils.getExtraPropertiesForLocalAnalysis(request.getProject());
    setRule(SONARLINT_ANALYSIS_RULE);
  }

  private static String jobTitle(AnalyzeProjectRequest request) {
    if (request.getFiles() == null) {
      return "SonarLint analysis of project " + request.getProject().getName();
    }
    if (request.getFiles().size() == 1) {
      return "SonarLint analysis of file " + request.getFiles().iterator().next().getFile().getName();
    }
    return "SonarLint analysis of project " + request.getProject().getName() + " (" + request.getFiles().size() + " files)";
  }

  private final class AnalysisThread extends Thread {
    private final Map<ISonarLintIssuable, List<Issue>> issuesPerResource;
    private final StandaloneAnalysisConfiguration config;
    private final ISonarLintProject project;
    private final IServer server;
    @Nullable
    private volatile AnalysisResults result;

    private AnalysisThread(@Nullable IServer server, Map<ISonarLintIssuable, List<Issue>> issuesPerResource, StandaloneAnalysisConfiguration config, ISonarLintProject project) {
      super("SonarLint analysis");
      this.server = server;
      this.issuesPerResource = issuesPerResource;
      this.config = config;
      this.project = project;
    }

    @Override
    public void run() {
      result = AnalyzeProjectJob.this.run(server, config, project, issuesPerResource);
    }

    @CheckForNull
    public AnalysisResults getResult() {
      return result;
    }
  }

  @Override
  protected IStatus doRun(final IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    long startTime = System.currentTimeMillis();
    SonarLintLogger.get().debug("Trigger: " + request.getTriggerType().name());
    SonarLintLogger.get().info(this.getName() + "...");
    // Analyze
    Path tempDirectory = null;
    try {
      // Configure
      Map<String, String> mergedExtraProps = new LinkedHashMap<>();
      final Map<ISonarLintFile, IDocument> filesToAnalyze = request.getFiles().stream().collect(HashMap::new, (m, fWithDoc) -> m.put(fWithDoc.getFile(), fWithDoc.getDocument()),
        HashMap::putAll);
      Collection<ProjectConfigurator> usedDeprecatedConfigurators = configureDeprecated(getProject(), filesToAnalyze.keySet(), mergedExtraProps, monitor);
      tempDirectory = Files.createTempDirectory(getProject().getWorkingDir(), "sonarlint");
      Collection<IAnalysisConfigurator> usedConfigurators = configure(getProject(), filesToAnalyze.keySet(), mergedExtraProps, tempDirectory, monitor);

      List<ClientInputFile> inputFiles = buildInputFiles(tempDirectory, filesToAnalyze);

      for (SonarLintProperty sonarProperty : extraProps) {
        mergedExtraProps.put(sonarProperty.getName(), sonarProperty.getValue());
      }

      if (!inputFiles.isEmpty()) {
        runAnalysisAndUpdateMarkers(filesToAnalyze, monitor, mergedExtraProps, inputFiles);
      }

      analysisCompleted(usedDeprecatedConfigurators, usedConfigurators, mergedExtraProps, monitor);
      SonarLintCorePlugin.getAnalysisListenerManager().notifyListeners();
      SonarLintLogger.get().debug(String.format("Done in %d ms", System.currentTimeMillis() - startTime));
    } catch (Exception e) {
      SonarLintLogger.get().error("Error during execution of SonarLint analysis", e);
      return new Status(Status.WARNING, SonarLintCorePlugin.PLUGIN_ID, "Error when executing SonarLint analysis", e);
    } finally {
      if (tempDirectory != null) {
        FileUtils.deleteRecursively(tempDirectory);
      }
    }

    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

  private void runAnalysisAndUpdateMarkers(Map<ISonarLintFile, IDocument> docPerFiles, final IProgressMonitor monitor, Map<String, String> mergedExtraProps,
    List<ClientInputFile> inputFiles) throws CoreException {
    StandaloneAnalysisConfiguration config;
    Path projectBaseDir = getProject().getBaseDir();
    Server server;
    if (getProject().isBound()) {
      server = (Server) ServersManager.getInstance().getServer(getProjectConfig().getServerId());
      if (server == null) {
        throw new IllegalStateException(
          "Project '" + getProject().getName() + "' is bound to an unknow server: '" + getProjectConfig().getServerId() + "'. Please fix project binding.");
      }
      SonarLintLogger.get().debug("Connected mode (using configuration of '" + getProjectConfig().getModuleKey() + "' in server '" + getProjectConfig().getServerId() + "')");
      config = new ConnectedAnalysisConfiguration(trimToNull(getProjectConfig().getModuleKey()), projectBaseDir, getProject().getWorkingDir(), inputFiles, mergedExtraProps);
    } else {
      server = null;
      SonarLintLogger.get().debug("Standalone mode (project not bound)");
      config = new StandaloneAnalysisConfiguration(projectBaseDir, getProject().getWorkingDir(), inputFiles, mergedExtraProps);
    }

    Map<ISonarLintIssuable, List<Issue>> issuesPerResource = new LinkedHashMap<>();
    request.getFiles().forEach(

      fileWithDoc -> issuesPerResource.put(fileWithDoc.getFile(), new ArrayList<>()));

    AnalysisResults result = runAndCheckCancellation(server, config, issuesPerResource, monitor);
    if (!monitor.isCanceled() && result != null) {
      updateMarkers(server, docPerFiles, issuesPerResource, result, request.getTriggerType(), monitor);
    }
  }

  private static List<ClientInputFile> buildInputFiles(Path tempDirectory, final Map<ISonarLintFile, IDocument> filesToAnalyze) {
    List<ClientInputFile> inputFiles = new ArrayList<>(filesToAnalyze.size());
    String allTestPattern = PreferencesUtils.getTestFileRegexps();
    String[] testPatterns = allTestPattern.split(",");
    final List<PathMatcher> pathMatchersForTests = createMatchersForTests(testPatterns);

    for (final Map.Entry<ISonarLintFile, IDocument> fileWithDoc : filesToAnalyze.entrySet()) {
      ISonarLintFile file = fileWithDoc.getKey();
      String language = tryDetectLanguage(file);
      ClientInputFile inputFile = new EclipseInputFile(pathMatchersForTests, file, tempDirectory, fileWithDoc.getValue(), language);
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

  private static List<PathMatcher> createMatchersForTests(String[] testPatterns) {
    final List<PathMatcher> pathMatchersForTests = new ArrayList<>();
    FileSystem fs = FileSystems.getDefault();
    for (String testPattern : testPatterns) {
      pathMatchersForTests.add(fs.getPathMatcher("glob:" + testPattern));
    }
    return pathMatchersForTests;
  }

  private static Collection<ProjectConfigurator> configureDeprecated(final ISonarLintProject project, Collection<ISonarLintFile> filesToAnalyze,
    final Map<String, String> extraProperties,
    final IProgressMonitor monitor) {
    Collection<ProjectConfigurator> usedConfigurators = new ArrayList<>();
    if (project.getUnderlyingProject() != null) {
      ProjectConfigurationRequest configuratorRequest = new ProjectConfigurationRequest(project.getUnderlyingProject(),
        filesToAnalyze.stream()
          .map(ISonarLintFile::getUnderlyingFile)
          .filter(Objects::nonNull)
          .collect(Collectors.toList()),
        extraProperties);
      Collection<ProjectConfigurator> configurators = SonarLintCorePlugin.getExtensionTracker().getConfigurators();
      for (ProjectConfigurator configurator : configurators) {
        if (configurator.canConfigure(project.getUnderlyingProject())) {
          configurator.configure(configuratorRequest, monitor);
          usedConfigurators.add(configurator);
        }
      }
    }

    return usedConfigurators;
  }

  private static Collection<IAnalysisConfigurator> configure(final ISonarLintProject project, Collection<ISonarLintFile> filesToAnalyze,
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
    final IProgressMonitor monitor)
    throws CoreException {

    String localModuleKey = getProject().getName();

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
      if (server != null && shouldUpdateServerIssuesSync(triggerType)) {
        tracked = trackServerIssuesSync(server, resource, tracked);
      }
      SonarLintMarkerUpdater.createOrUpdateMarkers(resource, documentNotNull, tracked, triggerType, documentOrNull != null);
      // Now that markerId are set, store issues in cache
      issueTracker.updateCache(relativePath, tracked);
    }

    if (server != null && shouldUpdateServerIssuesAsync(triggerType)) {
      trackServerIssuesAsync(server, rawIssuesPerResource.keySet(), monitor);
    }
  }

  private static boolean shouldUpdateServerIssuesSync(TriggerType trigger) {
    return trigger != TriggerType.EDITOR_CHANGE && trigger != TriggerType.EDITOR_OPEN;
  }

  /**
   * To not have a delay when opening editor, server issues will be processed asynchronously
   */
  private static boolean shouldUpdateServerIssuesAsync(TriggerType trigger) {
    return trigger == TriggerType.EDITOR_OPEN;
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

  private Collection<Trackable> trackServerIssuesSync(Server server, ISonarLintFile resource, Collection<Trackable> tracked) {
    ServerConfiguration serverConfiguration = server.getConfig();
    ConnectedSonarLintEngine engine = server.getEngine();
    List<ServerIssue> serverIssues = ServerIssueUpdater.fetchServerIssues(serverConfiguration, engine, getProjectConfig().getModuleKey(), resource);
    Collection<Trackable> serverIssuesTrackable = serverIssues.stream().map(ServerIssueTrackable::new).collect(Collectors.toList());
    return IssueTracker.matchAndTrackServerIssues(serverIssuesTrackable, tracked);
  }

  private void trackServerIssuesAsync(Server server, Collection<ISonarLintIssuable> resources, final IProgressMonitor monitor) {
    ServerConfiguration serverConfiguration = server.getConfig();
    ConnectedSonarLintEngine engine = server.getEngine();
    String localModuleKey = getProject().getName();
    SonarLintCorePlugin.getDefault().getServerIssueUpdater().updateAsync(serverConfiguration, engine, getProject(), localModuleKey, getProjectConfig().getModuleKey(), resources);
  }

  private static void analysisCompleted(Collection<ProjectConfigurator> usedDeprecatedConfigurators, Collection<IAnalysisConfigurator> usedConfigurators,
    Map<String, String> properties, final IProgressMonitor monitor) {
    Map<String, String> unmodifiableMap = Collections.unmodifiableMap(properties);
    for (ProjectConfigurator p : usedDeprecatedConfigurators) {
      p.analysisComplete(unmodifiableMap, monitor);
    }
    for (IAnalysisConfigurator p : usedConfigurators) {
      p.analysisComplete(() -> unmodifiableMap, monitor);
    }

  }

  @CheckForNull
  public AnalysisResults runAndCheckCancellation(@Nullable IServer server, final StandaloneAnalysisConfiguration config,
    final Map<ISonarLintIssuable, List<Issue>> issuesPerResource,
    final IProgressMonitor monitor) {
    SonarLintLogger.get().debug("Starting analysis with configuration:\n" + config.toString());
    AnalysisThread t = new AnalysisThread(server, issuesPerResource, config, getProject());
    t.setDaemon(true);
    t.setUncaughtExceptionHandler((th, ex) -> SonarLintLogger.get().error("Error during analysis", ex));
    t.start();
    waitForThread(monitor, t);
    return t.getResult();
  }

  private static void waitForThread(final IProgressMonitor monitor, Thread t) {
    while (t.isAlive()) {
      if (monitor.isCanceled()) {
        t.interrupt();
        try {
          t.join(5000);
        } catch (InterruptedException e) {
          // just quit
        }
        if (t.isAlive()) {
          SonarLintLogger.get().error("Unable to properly terminate SonarLint analysis");
        }
        break;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // Here we don't care
      }
    }
  }

  // Visible for testing
  @CheckForNull
  public AnalysisResults run(@Nullable IServer server, final StandaloneAnalysisConfiguration analysisConfig, final ISonarLintProject project,
    final Map<ISonarLintIssuable, List<Issue>> issuesPerResource) {
    SonarLintIssueListener issueListener = new SonarLintIssueListener(project, issuesPerResource);
    AnalysisResults result;
    if (server != null) {
      result = server.runAnalysis((ConnectedAnalysisConfiguration) analysisConfig, issueListener);
    } else {
      StandaloneSonarLintClientFacade facadeToUse = SonarLintCorePlugin.getDefault().getDefaultSonarLintClientFacade();
      result = facadeToUse.runAnalysis(analysisConfig, issueListener);
    }
    SonarLintLogger.get().info("Found " + issueListener.getIssueCount() + " issue(s)");
    return result;
  }
}
