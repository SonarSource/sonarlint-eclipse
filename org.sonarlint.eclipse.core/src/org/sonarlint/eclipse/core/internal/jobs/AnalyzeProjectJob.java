/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.TextFileContext;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.internal.tracking.IssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.RawIssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.ServerIssueUpdater;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
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
    super(jobTitle(request), SonarLintProject.getInstance(request.getProject()));
    this.request = request;
    this.extraProps = PreferencesUtils.getExtraPropertiesForLocalAnalysis(request.getProject());
    setRule(SONARLINT_ANALYSIS_RULE);
  }

  private static String jobTitle(AnalyzeProjectRequest request) {
    if (request.getFiles() == null) {
      return "SonarLint analysis of project " + request.getProject().getName();
    }
    if (request.getFiles().size() == 1) {
      return "SonarLint analysis of file " + request.getFiles().iterator().next().getFile().getProjectRelativePath().toString() + " (Project " + request.getProject().getName()
        + ")";
    }
    return "SonarLint analysis of project " + request.getProject().getName() + " (" + request.getFiles().size() + " files)";
  }

  private final class AnalysisThread extends Thread {
    private final Map<IResource, List<Issue>> issuesPerResource;
    private final StandaloneAnalysisConfiguration config;
    private final SonarLintProject project;
    @Nullable
    private volatile AnalysisResults result;

    private AnalysisThread(Map<IResource, List<Issue>> issuesPerResource, StandaloneAnalysisConfiguration config, SonarLintProject project) {
      super("SonarLint analysis");
      this.issuesPerResource = issuesPerResource;
      this.config = config;
      this.project = project;
    }

    @Override
    public void run() {
      result = AnalyzeProjectJob.this.run(config, project, issuesPerResource);
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
      IProject project = request.getProject();
      SonarLintProject sonarProject = SonarLintProject.getInstance(project);
      IPath projectSpecificWorkDir = project.getWorkingLocation(SonarLintCorePlugin.PLUGIN_ID);
      Map<String, String> mergedExtraProps = new LinkedHashMap<>();
      final Map<IFile, IDocument> filesToAnalyze = request.getFiles().stream().collect(HashMap::new, (m, fWithDoc) -> m.put(fWithDoc.getFile(), fWithDoc.getDocument()),
        HashMap::putAll);
      Map<IFile, String> fileLanguages = new HashMap<>();
      Collection<ProjectConfigurator> usedConfigurators = configure(project, filesToAnalyze.keySet(), fileLanguages, mergedExtraProps, monitor);

      tempDirectory = Files.createTempDirectory("sonarlint");

      List<ClientInputFile> inputFiles = buildInputFiles(tempDirectory, filesToAnalyze, fileLanguages);

      for (SonarLintProperty sonarProperty : extraProps) {
        mergedExtraProps.put(sonarProperty.getName(), sonarProperty.getValue());
      }

      if (!inputFiles.isEmpty()) {
        runAnalysisAndUpdateMarkers(filesToAnalyze, monitor, project, sonarProject, projectSpecificWorkDir, mergedExtraProps, inputFiles);
      }

      analysisCompleted(usedConfigurators, mergedExtraProps, monitor);
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

  private void runAnalysisAndUpdateMarkers(Map<IFile, IDocument> docPerFiles, final IProgressMonitor monitor, IProject project, SonarLintProject sonarProject,
    IPath projectSpecificWorkDir,
    Map<String, String> mergedExtraProps, List<ClientInputFile> inputFiles) throws CoreException {
    StandaloneAnalysisConfiguration config;
    IPath projectLocation = project.getLocation();
    // In some unfrequent cases the project may be virtual and don't have physical location
    Path projectBaseDir = projectLocation != null ? projectLocation.toFile().toPath() : ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
    if (sonarProject.isBound()) {
      SonarLintLogger.get().debug("Connected mode (using configuration of '" + sonarProject.getModuleKey() + "' in server '" + sonarProject.getServerId() + "')");
      config = new ConnectedAnalysisConfiguration(trimToNull(sonarProject.getModuleKey()), projectBaseDir, projectSpecificWorkDir.toFile().toPath(), inputFiles, mergedExtraProps);
    } else {
      SonarLintLogger.get().debug("Standalone mode (project not bound)");
      config = new StandaloneAnalysisConfiguration(projectBaseDir, projectSpecificWorkDir.toFile().toPath(), inputFiles, mergedExtraProps);
    }

    Map<IResource, List<Issue>> issuesPerResource = new LinkedHashMap<>();
    request.getFiles().forEach(fileWithDoc -> issuesPerResource.put(fileWithDoc.getFile(), new ArrayList<>()));

    AnalysisResults result = runAndCheckCancellation(config, sonarProject, issuesPerResource, monitor);
    if (!monitor.isCanceled() && result != null) {
      updateMarkers(docPerFiles, issuesPerResource, result, request.getTriggerType());
    }
  }

  private static List<ClientInputFile> buildInputFiles(Path tempDirectory, final Map<IFile, IDocument> filesToAnalyze, Map<IFile, String> fileLanguages) {
    List<ClientInputFile> inputFiles = new ArrayList<>(filesToAnalyze.size());
    String allTestPattern = PreferencesUtils.getTestFileRegexps();
    String[] testPatterns = allTestPattern.split(",");
    final List<PathMatcher> pathMatchersForTests = createMatchersForTests(testPatterns);

    for (final Map.Entry<IFile, IDocument> fileWithDoc : filesToAnalyze.entrySet()) {
      IFile file = fileWithDoc.getKey();
      ClientInputFile inputFile = new EclipseInputFile(pathMatchersForTests, file, tempDirectory, fileWithDoc.getValue(), fileLanguages.get(file));
      inputFiles.add(inputFile);
    }
    return inputFiles;
  }

  private static List<PathMatcher> createMatchersForTests(String[] testPatterns) {
    final List<PathMatcher> pathMatchersForTests = new ArrayList<>();
    FileSystem fs = FileSystems.getDefault();
    for (String testPattern : testPatterns) {
      pathMatchersForTests.add(fs.getPathMatcher("glob:" + testPattern));
    }
    return pathMatchersForTests;
  }

  private static Collection<ProjectConfigurator> configure(final IProject project, Collection<IFile> filesToAnalyze, Map<IFile, String> fileLanguages,
    final Map<String, String> extraProperties,
    final IProgressMonitor monitor) {
    ProjectConfigurationRequest configuratorRequest = new ProjectConfigurationRequest(project, filesToAnalyze, fileLanguages, extraProperties);
    Collection<ProjectConfigurator> configurators = ConfiguratorUtils.getConfigurators();
    Collection<ProjectConfigurator> usedConfigurators = new ArrayList<>();
    for (ProjectConfigurator configurator : configurators) {
      if (configurator.canConfigure(project)) {
        configurator.configure(configuratorRequest, monitor);
        usedConfigurators.add(configurator);
      }
    }

    return usedConfigurators;
  }

  private void updateMarkers(Map<IFile, IDocument> docPerFile, Map<IResource, List<Issue>> issuesPerResource, AnalysisResults result, TriggerType triggerType)
    throws CoreException {
    Set<IFile> failedFiles = result.failedAnalysisFiles().stream().map(ClientInputFile::<IFile>getClientObject).collect(Collectors.toSet());
    Map<IResource, List<Issue>> successfulFiles = issuesPerResource.entrySet().stream()
      .filter(e -> !failedFiles.contains(e.getKey()))
      // TODO handle non-file-level issues
      .filter(e -> e.getKey() instanceof IFile)
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    trackIssues(docPerFile, successfulFiles, triggerType);
  }

  private void trackIssues(Map<IFile, IDocument> docPerFile, Map<IResource, List<Issue>> rawIssuesPerResource, TriggerType triggerType) throws CoreException {

    String localModuleKey = getSonarProject().getProject().getName();

    for (Map.Entry<IResource, List<Issue>> entry : rawIssuesPerResource.entrySet()) {
      IResource resource = entry.getKey();
      IDocument documentOrNull = docPerFile.get((IFile) resource);
      final IDocument documentNotNull;
      if (documentOrNull == null) {
        try (TextFileContext context = new TextFileContext((IFile) resource)) {
          documentNotNull = context.getDocument();
        }
      } else {
        documentNotNull = documentOrNull;
      }
      List<Issue> rawIssues = entry.getValue();
      List<Trackable> trackables = rawIssues.stream().map(issue -> transform(issue, resource, documentNotNull)).collect(Collectors.toList());
      IssueTracker issueTracker = SonarLintCorePlugin.getOrCreateIssueTracker(getSonarProject().getProject(), localModuleKey);
      String relativePath = resource.getProjectRelativePath().toString();
      Collection<Trackable> tracked = issueTracker.matchAndTrackAsNew(relativePath, trackables);
      if (shouldUpdateServerIssuesSync(triggerType)) {
        tracked = trackServerIssuesSync(resource, tracked);
      }
      SonarLintMarkerUpdater.createOrUpdateMarkers(resource, documentNotNull, tracked, triggerType, documentOrNull != null);
      // Now that markerId are set, store issues in cache
      issueTracker.updateCache(relativePath, tracked);
    }

    if (shouldUpdateServerIssuesAsync(triggerType)) {
      trackServerIssuesAsync(rawIssuesPerResource.keySet());
    }
  }

  private boolean shouldUpdateServerIssuesSync(TriggerType trigger) {
    return getSonarProject().isBound() && trigger != TriggerType.EDITOR_CHANGE && trigger != TriggerType.EDITOR_OPEN;
  }

  /**
   * To not have a delay when opening editor, server issues will be processed asynchronously
   */
  private boolean shouldUpdateServerIssuesAsync(TriggerType trigger) {
    return getSonarProject().isBound() && trigger == TriggerType.EDITOR_OPEN;
  }

  private static RawIssueTrackable transform(Issue issue, IResource resource, IDocument document) {
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
  private static String readTextRangeContent(IResource resource, IDocument document, TextRange textRange) {
    Position position = MarkerUtils.getPosition(document, textRange);
    if (position != null) {
      try {
        return document.get(position.getOffset(), position.getLength());
      } catch (BadLocationException e) {
        SonarLintLogger.get().error("failed to get text range content of resource " + resource.getFullPath(), e);
      }
    }
    return null;
  }

  @CheckForNull
  private static String readLineContent(IResource resource, IDocument document, int startLine) {
    Position position = MarkerUtils.getPosition(document, startLine);
    if (position != null) {
      try {
        return document.get(position.getOffset(), position.getLength());
      } catch (BadLocationException e) {
        SonarLintLogger.get().error("failed to get line content of resource " + resource.getFullPath(), e);
      }
    }
    return null;
  }

  private Collection<Trackable> trackServerIssuesSync(IResource resource, Collection<Trackable> tracked) {
    String serverId = getSonarProject().getServerId();

    if (serverId == null) {
      // not bound to a server -> nothing to do
      return tracked;
    }

    String serverModuleKey = SonarLintCorePlugin.getDefault().getProjectManager().readSonarLintConfiguration(this.getSonarProject().getProject()).getModuleKey();
    if (serverModuleKey == null) {
      // not bound to a module -> nothing to do
      return tracked;
    }

    Server server = (Server) ServersManager.getInstance().getServer(serverId);
    ServerConfiguration serverConfiguration = server.getConfig();
    ConnectedSonarLintEngine engine = server.getEngine();
    List<ServerIssue> serverIssues = ServerIssueUpdater.fetchServerIssues(serverConfiguration, engine, serverModuleKey, resource);
    Collection<Trackable> serverIssuesTrackable = serverIssues.stream().map(ServerIssueTrackable::new).collect(Collectors.toList());
    return IssueTracker.matchAndTrackServerIssues(serverIssuesTrackable, tracked);
  }

  private void trackServerIssuesAsync(Collection<IResource> resources) {
    String serverId = getSonarProject().getServerId();

    if (serverId == null) {
      // not bound to a server -> nothing to do
      return;
    }

    String serverModuleKey = SonarLintCorePlugin.getDefault().getProjectManager().readSonarLintConfiguration(this.getSonarProject().getProject()).getModuleKey();
    if (serverModuleKey == null) {
      // not bound to a module -> nothing to do
      return;
    }

    Server server = (Server) ServersManager.getInstance().getServer(serverId);
    ServerConfiguration serverConfiguration = server.getConfig();
    ConnectedSonarLintEngine engine = server.getEngine();
    String localModuleKey = getSonarProject().getProject().getName();
    SonarLintCorePlugin.getDefault().getServerIssueUpdater().updateAsync(serverConfiguration, engine, getSonarProject(), localModuleKey, serverModuleKey, resources);
  }

  private static void analysisCompleted(Collection<ProjectConfigurator> usedConfigurators, Map<String, String> properties, final IProgressMonitor monitor) {
    for (ProjectConfigurator p : usedConfigurators) {
      p.analysisComplete(Collections.unmodifiableMap(properties), monitor);
    }

  }

  @CheckForNull
  public AnalysisResults runAndCheckCancellation(final StandaloneAnalysisConfiguration config, final SonarLintProject project, final Map<IResource, List<Issue>> issuesPerResource,
    final IProgressMonitor monitor) {
    SonarLintLogger.get().debug("Starting analysis with configuration:\n" + config.toString());
    AnalysisThread t = new AnalysisThread(issuesPerResource, config, project);
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
  public AnalysisResults run(final StandaloneAnalysisConfiguration config, final SonarLintProject project, final Map<IResource, List<Issue>> issuesPerResource) {
    SonarLintIssueListener issueListener = new SonarLintIssueListener(project.getProject(), issuesPerResource);
    AnalysisResults result;
    if (StringUtils.isNotBlank(project.getServerId())) {
      IServer server = ServersManager.getInstance().getServer(project.getServerId());
      if (server == null) {
        throw new IllegalStateException(
          "Project '" + project.getProject().getName() + "' is linked to an unknow server: '" + project.getServerId() + "'. Please bind project again.");
      }
      result = server.runAnalysis((ConnectedAnalysisConfiguration) config, issueListener);
    } else {
      StandaloneSonarLintClientFacade facadeToUse = SonarLintCorePlugin.getDefault().getDefaultSonarLintClientFacade();
      result = facadeToUse.runAnalysis(config, issueListener);
    }
    SonarLintLogger.get().info("Found " + issueListener.getIssueCount() + " issue(s)");
    return result;
  }
}
