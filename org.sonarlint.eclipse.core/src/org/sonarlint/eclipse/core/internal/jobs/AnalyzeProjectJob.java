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

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
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
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonarlint.eclipse.core.internal.markers.FlatTextRange;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.TextFileContext;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.Server;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.internal.tracking.IssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;

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
      return "SonarLint analysis of file " + request.getFiles().iterator().next().getProjectRelativePath().toString() + " (Project " + request.getProject().getName() + ")";
    }
    return "SonarLint analysis of project " + request.getProject().getName() + " (" + request.getFiles().size() + " files)";
  }

  private final class AnalysisThread extends Thread {
    private final Map<IResource, List<Issue>> issuesPerResource;
    private final StandaloneAnalysisConfiguration config;
    private final SonarLintProject project;
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

    public AnalysisResults getResult() {
      return result;
    }
  }

  private final class SonarLintIssueListener implements IssueListener {
    private final Map<IResource, List<Issue>> issuesPerResource;

    private SonarLintIssueListener(Map<IResource, List<Issue>> issuesPerResource) {
      this.issuesPerResource = issuesPerResource;
    }

    @Override
    public void handle(Issue issue) {
      IResource r;
      ClientInputFile inputFile = issue.getInputFile();
      if (inputFile == null) {
        r = request.getProject();
      } else {
        r = inputFile.getClientObject();
      }
      if (!issuesPerResource.containsKey(r)) {
        issuesPerResource.put(r, new ArrayList<Issue>());
      }
      issuesPerResource.get(r).add(issue);
    }
  }

  @Override
  protected IStatus doRun(final IProgressMonitor monitor) {
    long startTime = System.currentTimeMillis();
    // Analyze
    try {
      // Configure
      IProject project = request.getProject();
      SonarLintProject sonarProject = SonarLintProject.getInstance(project);
      IPath projectSpecificWorkDir = project.getWorkingLocation(SonarLintCorePlugin.PLUGIN_ID);
      Map<String, String> mergedExtraProps = new LinkedHashMap<>();
      final List<IFile> filesToAnalyze = new ArrayList<>(request.getFiles().size());
      Collection<ProjectConfigurator> usedConfigurators = populateFilesToAnalyze(monitor, project, mergedExtraProps, filesToAnalyze);

      List<ClientInputFile> inputFiles = buildInputFiles(filesToAnalyze, monitor);

      for (SonarLintProperty sonarProperty : extraProps) {
        mergedExtraProps.put(sonarProperty.getName(), sonarProperty.getValue());
      }

      if (!inputFiles.isEmpty()) {
        runAnalysisAndUpdateMarkers(monitor, project, sonarProject, projectSpecificWorkDir, mergedExtraProps, inputFiles);
      }

      analysisCompleted(usedConfigurators, mergedExtraProps, monitor);
      SonarLintCorePlugin.getDefault().debug(String.format("Analysis completed in %d ms", System.currentTimeMillis() - startTime));
    } catch (Exception e) {
      SonarLintCorePlugin.getDefault().error("Error during execution of SonarLint analysis", e);
      return new Status(Status.WARNING, SonarLintCorePlugin.PLUGIN_ID, "Error when executing SonarLint analysis", e);
    }
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }

    return Status.OK_STATUS;
  }

  private void runAnalysisAndUpdateMarkers(final IProgressMonitor monitor, IProject project, SonarLintProject sonarProject, IPath projectSpecificWorkDir,
    Map<String, String> mergedExtraProps, List<ClientInputFile> inputFiles) throws CoreException {
    StandaloneAnalysisConfiguration config;
    IPath projectLocation = project.getLocation();
    // In some unfrequent cases the project may be virtual and don't have physical location
    Path projectBaseDir = projectLocation != null ? projectLocation.toFile().toPath() : ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
    if (sonarProject.isBound()) {
      config = new ConnectedAnalysisConfiguration(trimToNull(sonarProject.getModuleKey()), projectBaseDir, projectSpecificWorkDir.toFile().toPath(), inputFiles, mergedExtraProps);
    } else {
      config = new StandaloneAnalysisConfiguration(projectBaseDir, projectSpecificWorkDir.toFile().toPath(), inputFiles, mergedExtraProps);
    }

    Map<IResource, List<Issue>> issuesPerResource = new LinkedHashMap<>();
    request.getFiles().forEach(file -> issuesPerResource.put(file, new ArrayList<>()));

    AnalysisResults result = runAndCheckCancellation(config, sonarProject, issuesPerResource, monitor);
    if (!monitor.isCanceled() && result != null) {
      updateMarkers(issuesPerResource, result, request.getTriggerType());
    }
  }

  private List<ClientInputFile> buildInputFiles(final List<IFile> filesToAnalyze, IProgressMonitor monitor) {
    List<ClientInputFile> inputFiles = new ArrayList<>(filesToAnalyze.size());
    String allTestPattern = PreferencesUtils.getTestFileRegexps();
    String[] testPatterns = allTestPattern.split(",");
    final List<PathMatcher> pathMatchersForTests = createMatchersForTests(testPatterns);
    for (final IFile file : filesToAnalyze) {
      try {
        IFileStore fileStore = EFS.getStore(file.getLocationURI());
        File localFile = fileStore.toLocalFile(EFS.NONE, monitor);
        if (localFile == null) {
          // Try to get a cached copy (for virtual file systems)
          localFile = fileStore.toLocalFile(EFS.CACHE, monitor);
        }
        final Path filePath = localFile.toPath();
        inputFiles.add(new EclipseInputFile(pathMatchersForTests, file, filePath));
      } catch (CoreException e) {
        SonarLintCorePlugin.getDefault().error("Error building input file for SonarLint analysis: " + file.getName(), e);
      }
    }
    return inputFiles;
  }

  private Collection<ProjectConfigurator> populateFilesToAnalyze(final IProgressMonitor monitor, IProject project,
    Map<String, String> mergedExtraProps, final List<IFile> filesToAnalyze) {
    filesToAnalyze.addAll(request.getFiles());
    return configure(project, filesToAnalyze, mergedExtraProps, monitor);
  }

  private static List<PathMatcher> createMatchersForTests(String[] testPatterns) {
    final List<PathMatcher> pathMatchersForTests = new ArrayList<>();
    FileSystem fs = FileSystems.getDefault();
    for (String testPattern : testPatterns) {
      pathMatchersForTests.add(fs.getPathMatcher("glob:" + testPattern));
    }
    return pathMatchersForTests;
  }

  private static Collection<ProjectConfigurator> configure(final IProject project, Collection<IFile> filesToAnalyze, final Map<String, String> extraProperties,
    final IProgressMonitor monitor) {
    ProjectConfigurationRequest configuratorRequest = new ProjectConfigurationRequest(project, filesToAnalyze, extraProperties);
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

  private void updateMarkers(Map<IResource, List<Issue>> issuesPerResource, AnalysisResults result, TriggerType triggerType) throws CoreException {
    ITextFileBufferManager textFileBufferManager = FileBuffers.getTextFileBufferManager();
    if (textFileBufferManager == null) {
      return;
    }

    Set<IFile> failedFiles = result.failedAnalysisFiles().stream().map(f -> f.<IFile>getClientObject()).collect(Collectors.toSet());
    for (Entry<IResource, List<Issue>> resourceEntry : issuesPerResource.entrySet()) {
      IResource resource = resourceEntry.getKey();
      if (failedFiles.contains(resource)) {
        continue;
      }
      if (resource instanceof IFile) {
        trackIssues(resource, resourceEntry.getValue(), triggerType);
      } else {
        // TODO handle non-file-level issues
      }
    }
  }

  private void trackIssues(IResource resource, List<Issue> rawIssues, TriggerType triggerType) throws CoreException {
    IProject project = resource.getProject();
    String localModuleKey = project.getName();
    SonarLintCorePlugin.getDefault().getModulePathManager().setModulePath(localModuleKey, project.getLocation().toString());
    String relativePath = resource.getProjectRelativePath().toString();

    try (TextFileContext context = new TextFileContext((IFile) resource)) {
      IDocument document = context.getDocument();

      trackLocalIssues(localModuleKey, relativePath, document, rawIssues);
      if (shouldUpdateServerIssues(triggerType)) {
        trackServerIssues(localModuleKey, relativePath, resource);
      }
    }
  }

  private boolean shouldUpdateServerIssues(TriggerType trigger) {
    return getSonarProject().isBound() && (trigger == TriggerType.EDITOR_OPEN || trigger == TriggerType.ACTION);
  }

  private static void trackLocalIssues(String localModuleKey, String relativePath, @Nullable IDocument document, List<Issue> rawIssues) {
    List<Trackable> trackables = rawIssues.stream().map(issue -> transform(issue, relativePath, document)).collect(Collectors.toList());
    SonarLintCorePlugin.getOrCreateIssueTracker(localModuleKey).matchAndTrackAsNew(relativePath, trackables);
  }

  private static IssueTrackable transform(Issue issue, String relativePath, @Nullable IDocument document) {
    TextRange textRange = new TextRange(issue.getStartLine(), issue.getStartLineOffset(), issue.getEndLine(), issue.getEndLineOffset());
    String textRangeContent = null;
    String lineContent = null;
    if (document != null) {
      FlatTextRange flatTextRange = MarkerUtils.getFlatTextRange(document, textRange);
      if (flatTextRange != null) {
        try {
          textRangeContent = document.get(flatTextRange.getStart(), flatTextRange.getLength());
        } catch (BadLocationException e) {
          SonarLintCorePlugin.getDefault().error("failed to get text range content of file " + relativePath, e);
        }
      }

      FlatTextRange lineTextRange = MarkerUtils.getFlatTextRange(document, issue.getStartLine());
      if (lineTextRange != null) {
        try {
          lineContent = document.get(lineTextRange.getStart(), lineTextRange.getLength());
        } catch (BadLocationException e) {
          SonarLintCorePlugin.getDefault().error("failed to get line content of file " + relativePath, e);
        }
      }
    }
    return new IssueTrackable(issue, textRange, textRangeContent, lineContent);
  }

  private static void trackServerIssues(String localModuleKey, String relativePath, IResource resource) {
    String serverId = SonarLintProject.getInstance(resource).getServerId();
    if (serverId == null) {
      // not bound to a server -> nothing to do
      return;
    }

    String serverModuleKey = SonarLintCorePlugin.getDefault().getProjectManager().readSonarLintConfiguration(resource.getProject()).getModuleKey();
    if (serverModuleKey == null) {
      // not bound to a module -> nothing to do
      return;
    }

    Server server = (Server) ServersManager.getInstance().getServer(serverId);
    ServerConfiguration serverConfiguration = server.getConfig();
    ConnectedSonarLintEngine engine = server.getEngine();
    SonarLintCorePlugin.getDefault().getServerIssueUpdater().updateFor(serverConfiguration, engine, localModuleKey, serverModuleKey, relativePath);
  }

  private static void analysisCompleted(Collection<ProjectConfigurator> usedConfigurators, Map<String, String> properties, final IProgressMonitor monitor) {
    for (ProjectConfigurator p : usedConfigurators) {
      p.analysisComplete(Collections.unmodifiableMap(properties), monitor);
    }

  }

  public AnalysisResults runAndCheckCancellation(final StandaloneAnalysisConfiguration config, final SonarLintProject project, final Map<IResource, List<Issue>> issuesPerResource,
    final IProgressMonitor monitor) {
    SonarLintCorePlugin.getDefault().debug("Start analysis with configuration:\n" + config.toString());
    AnalysisThread t = new AnalysisThread(issuesPerResource, config, project);
    t.setDaemon(true);
    t.setUncaughtExceptionHandler((th, ex) -> SonarLintCorePlugin.getDefault().error("Error during analysis", ex));
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
          SonarLintCorePlugin.getDefault().error("Unable to properly terminate SonarLint analysis");
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
  public AnalysisResults run(final StandaloneAnalysisConfiguration config, final SonarLintProject project, final Map<IResource, List<Issue>> issuesPerResource) {
    if (StringUtils.isNotBlank(project.getServerId())) {
      IServer server = ServersManager.getInstance().getServer(project.getServerId());
      if (server == null) {
        throw new IllegalStateException(
          "Project '" + project.getProject().getName() + "' is linked to an unknow server: '" + project.getServerId() + "'. Please bind project again.");
      }
      return server.runAnalysis((ConnectedAnalysisConfiguration) config, new SonarLintIssueListener(issuesPerResource));
    } else {
      StandaloneSonarLintClientFacade facadeToUse = SonarLintCorePlugin.getDefault().getDefaultSonarLintClientFacade();
      return facadeToUse.runAnalysis(config, new SonarLintIssueListener(issuesPerResource));
    }
  }
}
