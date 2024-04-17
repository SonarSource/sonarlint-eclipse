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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IAnalysisConfigurator;
import org.sonarlint.eclipse.core.analysis.IPostAnalysisContext;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.internal.backend.RunningAnalysesTracker;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.event.AnalysisEvent;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.internal.tracking.ProjectIssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.RawIssueTrackable;
import org.sonarlint.eclipse.core.internal.utils.FileExclusionsChecker;
import org.sonarlint.eclipse.core.internal.utils.FileUtils;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.commons.api.progress.CanceledException;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesResponse;
import org.sonarsource.sonarlint.core.rpc.protocol.client.analysis.RawIssueDto;
import org.sonarsource.sonarlint.core.rpc.protocol.common.TextRangeDto;

import static java.text.MessageFormat.format;

public class AnalyzeProjectJob extends AbstractSonarProjectJob {
  // Because we have to await Sloop to get ready, the analysis might not be ready in the meantime
  // -> The analysis jobs run in a different process we have to make it thread safe!
  public static ConcurrentHashMap<String, Boolean> analysisReadyByConfigurationScopeId = new ConcurrentHashMap<>();

  @Nullable
  private final ISonarLintProject project;
  private final List<SonarLintProperty> extraProps;
  private final TriggerType triggerType;
  private final boolean shouldClearReport;
  private final Collection<FileWithDocument> files;

  public AnalyzeProjectJob(AnalyzeProjectRequest request) {
    super(jobTitle(request), request.getProject());
    this.project = request.getProject();
    this.extraProps = SonarLintGlobalConfiguration.getExtraPropertiesForLocalAnalysis(request.getProject());
    this.files = request.getFiles();
    this.triggerType = request.getTriggerType();
    this.shouldClearReport = request.shouldClearReport();
  }

  public static AbstractSonarProjectJob create(AnalyzeProjectRequest request) {
    return SonarLintCorePlugin.getConnectionManager()
      .resolveBinding(request.getProject())
      .<AbstractSonarProjectJob>map(b -> new AnalyzeConnectedProjectJob(request))
      .orElseGet(() -> new AnalyzeProjectJob(request));
  }

  public static void changeAnalysisReadiness(Set<String> configurationScopeIds, boolean readiness) {
    analysisReadyByConfigurationScopeId.putAll(
      configurationScopeIds.stream().collect(Collectors.toMap(k -> k, v -> readiness)));
  }

  private static String jobTitle(AnalyzeProjectRequest request) {
    if (request.getFiles().size() == 1) {
      return "SonarLint processing file " + request.getFiles().iterator().next().getFile().getName();
    }
    return format("SonarLint analysis of project {0} ({1} files processed)", request.getProject().getName(), request.getFiles().size());
  }

  @Override
  protected IStatus doRun(final IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }

    SonarLintLogger.get().debug("Trigger: " + triggerType.name());

    // Handle Sloop not ready for an analysis
    if (!checkIfReady()) {
      SonarLintLogger.get().debug("Analysis cancelled due to the engines not yet being ready");
      return Status.CANCEL_STATUS;
    }
    SonarLintLogger.get().debug("Analysis started with the engines being ready");

    var startTime = System.currentTimeMillis();
    Path analysisWorkDir = null;
    try {
      var excludedFiles = new ArrayList<ISonarLintFile>();
      var filesToAnalyze = new ArrayList<FileWithDocument>();

      var exclusionsChecker = new FileExclusionsChecker(getProject());
      files.forEach(fWithDoc -> {
        var file = fWithDoc.getFile();
        if (exclusionsChecker.isExcluded(file, true, monitor) || isScmIgnored(file)) {
          excludedFiles.add(file);
        } else {
          filesToAnalyze.add(fWithDoc);
        }
      });

      Map<ISonarLintFile, IDocument> filesToAnalyzeMap = filesToAnalyze
        .stream()
        .collect(HashMap::new, (m, fWithDoc) -> m.put(fWithDoc.getFile(), fWithDoc.getDocument()), HashMap::putAll);

      SonarLintLogger.get().debug("Clear markers on " + excludedFiles.size() + " excluded files");
      ResourcesPlugin.getWorkspace().run(m -> {
        excludedFiles.forEach(SonarLintMarkerUpdater::clearMarkers);

        if (shouldClearReport) {
          SonarLintMarkerUpdater.deleteAllMarkersFromReport();
        }
      }, monitor);

      if (filesToAnalyze.isEmpty()) {
        return Status.OK_STATUS;
      }

      // Analyze
      SonarLintLogger.get().info(this.getName() + "...");
      // Configure
      var mergedExtraProps = new LinkedHashMap<String, String>();
      var usedDeprecatedConfigurators = configureDeprecated(getProject(), filesToAnalyzeMap.keySet(), mergedExtraProps, monitor);

      analysisWorkDir = Files.createTempDirectory(getProject().getWorkingDir(), "sonarlint");
      var inputFiles = buildInputFiles(analysisWorkDir, filesToAnalyzeMap);
      var usedConfigurators = configure(getProject(), inputFiles, mergedExtraProps, analysisWorkDir, monitor);

      extraProps.forEach(sonarProperty -> mergedExtraProps.put(sonarProperty.getName(), sonarProperty.getValue()));

      if (!inputFiles.isEmpty()) {
        runAnalysisAndUpdateMarkers(filesToAnalyzeMap, monitor, mergedExtraProps);
      }

      analysisCompleted(usedDeprecatedConfigurators, usedConfigurators, mergedExtraProps, monitor);

      SonarLintCorePlugin.getAnalysisListenerManager().notifyListeners(new AnalysisEvent() {
        @Override
        public Set<ISonarLintProject> getProjects() {
          return Set.of(getProject());
        }
      });

      SonarLintLogger.get().debug(String.format("Done in %d ms", System.currentTimeMillis() - startTime));
    } catch (CanceledException e) {
      return Status.CANCEL_STATUS;
    } catch (Exception e) {
      SonarLintLogger.get().error("Error during execution of SonarLint analysis", e);
      return new Status(IStatus.WARNING, SonarLintCorePlugin.PLUGIN_ID, "Error when executing SonarLint analysis", e);
    } finally {
      if (analysisWorkDir != null) {
        FileUtils.deleteRecursively(analysisWorkDir);
      }
    }

    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

  /**
   *  When we actually analyze a project we want to check if it is ready to be analyzed. When we check no project we
   *  want to get all the projects involved and check if they're ready.
   *  -> When there are many projects in the workspace we only have to await the ones involved!
   */
  private boolean checkIfReady() {
    // For a single project it is more efficient to test for the project instead for every file. Even if there is just
    // one file, we don't have to invoke the costly stream methods.
    if (project != null) {
      return analysisReadyByConfigurationScopeId
        .getOrDefault(ConfigScopeSynchronizer.getConfigScopeId(project), false);
    }

    var configurationScopeIds = files.stream()
      .map(file -> file.getFile().getProject())
      .map(ConfigScopeSynchronizer::getConfigScopeId)
      .collect(Collectors.toSet());

    for (var configurationScopeId : configurationScopeIds) {
      if (Boolean.TRUE.equals(analysisReadyByConfigurationScopeId.get(configurationScopeId))) {
        return true;
      }
    }

    return false;
  }

  private static boolean isScmIgnored(ISonarLintFile file) {
    var ignored = file.isScmIgnored();
    if (ignored) {
      SonarLintLogger.get().debug("File '" + file.getName() + "' skipped from analysis because it is ignored by SCM");
    }
    return ignored;
  }

  private void runAnalysisAndUpdateMarkers(Map<ISonarLintFile, IDocument> docPerFiles, final IProgressMonitor monitor, Map<String, String> mergedExtraProps) throws CoreException {
    var issuesPerResource = new LinkedHashMap<ISonarLintIssuable, List<RawIssueDto>>();
    docPerFiles.keySet().forEach(slFile -> issuesPerResource.put(slFile, new ArrayList<>()));

    var start = System.currentTimeMillis();
    var result = run(docPerFiles, mergedExtraProps, issuesPerResource, start, monitor);
    if (!monitor.isCanceled()) {
      updateMarkers(docPerFiles, issuesPerResource, result, triggerType, monitor);
    }
  }

  private static List<EclipseInputFile> buildInputFiles(Path tempDirectory, final Map<ISonarLintFile, IDocument> filesToAnalyze) {
    var inputFiles = new ArrayList<EclipseInputFile>(filesToAnalyze.size());

    for (final var fileWithDoc : filesToAnalyze.entrySet()) {
      var file = fileWithDoc.getKey();
      var inputFile = new EclipseInputFile(file, tempDirectory, fileWithDoc.getValue());
      inputFiles.add(inputFile);
    }
    return inputFiles;
  }

  private static Collection<ProjectConfigurator> configureDeprecated(final ISonarLintProject project, Collection<ISonarLintFile> filesToAnalyze,
    final Map<String, String> extraProperties,
    final IProgressMonitor monitor) {
    var usedConfigurators = new ArrayList<ProjectConfigurator>();
    if (project.getResource() instanceof IProject) {
      var configuratorRequest = new ProjectConfigurationRequest((IProject) project.getResource(),
        filesToAnalyze.stream()
          .map(f -> (f.getResource() instanceof IFile) ? (IFile) f.getResource() : null)
          .filter(Objects::nonNull)
          .collect(Collectors.toList()),
        extraProperties);
      var configurators = SonarLintExtensionTracker.getInstance().getConfigurators();
      for (var configurator : configurators) {
        if (configurator.canConfigure((IProject) project.getResource())) {
          configurator.configure(configuratorRequest, monitor);
          usedConfigurators.add(configurator);
        }
      }
    }

    return usedConfigurators;
  }

  private static Collection<IAnalysisConfigurator> configure(final ISonarLintProject project, List<EclipseInputFile> filesToAnalyze,
    final Map<String, String> extraProperties, Path tempDir, final IProgressMonitor monitor) {
    var usedConfigurators = new ArrayList<IAnalysisConfigurator>();
    var configurators = SonarLintExtensionTracker.getInstance().getAnalysisConfigurators();
    var context = new DefaultPreAnalysisContext(project, extraProperties, filesToAnalyze, tempDir);
    for (var configurator : configurators) {
      if (configurator.canConfigure(project)) {
        configurator.configure(context, monitor);
        usedConfigurators.add(configurator);
      }
    }

    return usedConfigurators;
  }

  private void updateMarkers(Map<ISonarLintFile, IDocument> docPerFile, Map<ISonarLintIssuable, List<RawIssueDto>> issuesPerResource, AnalyzeFilesResponse result,
    TriggerType triggerType, final IProgressMonitor monitor) throws CoreException {
    var failedFileUris = result.getFailedAnalysisFiles();
    var successfulFiles = issuesPerResource.entrySet().stream()
      .filter(e -> !failedFileUris.contains(e.getKey().getResource().getLocationURI()))
      // TODO handle non-file-level issues
      .filter(e -> e.getKey() instanceof ISonarLintFile)
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    ResourcesPlugin.getWorkspace().run(m -> trackIssues(docPerFile, successfulFiles, triggerType, monitor), monitor);
  }

  protected void trackIssues(Map<ISonarLintFile, IDocument> docPerFile,
    Map<ISonarLintIssuable, List<RawIssueDto>> rawIssuesPerResource, TriggerType triggerType,
    final IProgressMonitor monitor) {
    if (rawIssuesPerResource.entrySet().isEmpty()) {
      return;
    }

    // To access the preference service only once and not per issue
    var issueFilterPreference = SonarLintGlobalConfiguration.getIssueFilter();

    // To access the preference service only once and not per issue
    var issuePeriodPreference = SonarLintGlobalConfiguration.getIssuePeriod();

    // If the project connection offers changing the status on anticipated issues (SonarQube 10.2+) we can enable the
    // context menu option on the markers.
    var viableForStatusChange = SonarLintUtils.checkProjectSupportsAnticipatedStatusChange(getProject());

    var issueTracker = SonarLintCorePlugin.getOrCreateIssueTracker(getProject());

    for (var entry : rawIssuesPerResource.entrySet()) {
      if (monitor.isCanceled()) {
        return;
      }
      var file = (ISonarLintFile) entry.getKey();
      var openedDocument = Optional.ofNullable(docPerFile.get(file));
      var rawIssues = entry.getValue();
      List<RawIssueTrackable> trackables;
      if (!rawIssues.isEmpty()) {
        var document = openedDocument.orElseGet(file::getDocument);
        trackables = rawIssues.stream().map(issue -> transform(issue, file, document)).collect(Collectors.toList());
      } else {
        trackables = Collections.emptyList();
      }
      trackFileIssues(file, trackables, issueTracker, triggerType, rawIssuesPerResource.size(), monitor);
      var tracked = issueTracker.getTracked(file);
      SonarLintMarkerUpdater.createOrUpdateMarkers(file, openedDocument, tracked, triggerType, issuePeriodPreference,
        issueFilterPreference, viableForStatusChange);
    }
  }

  protected void trackFileIssues(ISonarLintFile file, List<RawIssueTrackable> trackables, ProjectIssueTracker issueTracker, TriggerType triggerType,
    int totalTrackedFiles,
    IProgressMonitor monitor) {
    issueTracker.processRawIssues(file, trackables);
  }

  private static RawIssueTrackable transform(RawIssueDto issue, ISonarLintFile resource, IDocument document) {
    var textRange = issue.getTextRange();
    if (textRange == null) {
      return new RawIssueTrackable(issue);
    }
    var textRangeContent = readTextRangeContent(resource, document, textRange);
    var lineContent = readLineContent(resource, document, textRange.getStartLine());
    return new RawIssueTrackable(issue, textRangeContent, lineContent);
  }

  @Nullable
  private static String readTextRangeContent(ISonarLintFile resource, IDocument document, TextRangeDto textRange) {
    var position = MarkerUtils.getPosition(document, textRange);
    if (position != null) {
      try {
        return document.get(position.getOffset(), position.getLength());
      } catch (BadLocationException e) {
        SonarLintLogger.get().error("failed to get text range content of resource " + resource.getName(), e);
      }
    }
    return null;
  }

  @Nullable
  private static String readLineContent(ISonarLintFile resource, IDocument document, int startLine) {
    var position = MarkerUtils.getPosition(document, startLine);
    if (position != null) {
      try {
        return document.get(position.getOffset(), position.getLength());
      } catch (BadLocationException e) {
        SonarLintLogger.get().error("Failed to get line content of file " + resource.getName(), e);
      }
    }
    return null;
  }

  private static void analysisCompleted(Collection<ProjectConfigurator> usedDeprecatedConfigurators, Collection<IAnalysisConfigurator> usedConfigurators,
    Map<String, String> properties, final IProgressMonitor monitor) {
    var unmodifiableMap = Collections.unmodifiableMap(properties);
    for (var p : usedDeprecatedConfigurators) {
      p.analysisComplete(unmodifiableMap, monitor);
    }
    var context = new IPostAnalysisContext() {

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

  private AnalyzeFilesResponse run(final Map<ISonarLintFile, IDocument> docPerFiles, final Map<String, String> extraProps,
    final Map<ISonarLintIssuable, List<RawIssueDto>> issuesPerResource, long startTime, IProgressMonitor monitor) {
    var analysisId = UUID.randomUUID();
    var analysisState = new AnalysisState(analysisId, getProject(), issuesPerResource);
    try {
      RunningAnalysesTracker.get().track(analysisState);
      var future = SonarLintBackendService.get().analyzeFiles(getProject(), analysisId, docPerFiles, extraProps, startTime);
      AnalyzeFilesResponse response = JobUtils.waitForFutureInJob(monitor, future);
      SonarLintLogger.get().info("Found " + analysisState.getIssueCount() + " issue(s)");
      return response;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CanceledException();
    } catch (ExecutionException e) {
      throw new IllegalStateException(e);
    } finally {
      RunningAnalysesTracker.get().finish(analysisState);
    }
  }
}
