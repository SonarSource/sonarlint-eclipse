/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
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
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.TextRange;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.core.internal.tracking.IssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.RawIssueTrackable;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarlint.eclipse.core.internal.utils.FileExclusionsChecker;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.AbstractAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.common.Language;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.exceptions.CanceledException;
import org.sonarsource.sonarlint.core.client.api.util.FileUtils;

import static java.text.MessageFormat.format;

public abstract class AbstractAnalyzeProjectJob<CONFIG extends AbstractAnalysisConfiguration> extends AbstractSonarProjectJob {
  private final List<SonarLintProperty> extraProps;
  private final TriggerType triggerType;
  private final boolean shouldClearReport;
  private final Collection<FileWithDocument> files;

  protected AbstractAnalyzeProjectJob(AnalyzeProjectRequest request) {
    super(jobTitle(request), request.getProject());
    this.extraProps = SonarLintGlobalConfiguration.getExtraPropertiesForLocalAnalysis(request.getProject());
    this.files = request.getFiles();
    this.triggerType = request.getTriggerType();
    this.shouldClearReport = request.shouldClearReport();
  }

  public static AbstractSonarProjectJob create(AnalyzeProjectRequest request) {
    return SonarLintCorePlugin.getServersManager()
      .resolveBinding(request.getProject())
      .<AbstractSonarProjectJob>map(b -> new AnalyzeConnectedProjectJob(request, b.getProjectBinding(), (ConnectedEngineFacade) b.getEngineFacade()))
      .orElseGet(() -> new AnalyzeStandaloneProjectJob(request));
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
    long startTime = System.currentTimeMillis();
    SonarLintLogger.get().debug("Trigger: " + triggerType.name());

    Path analysisWorkDir = null;
    try {
      Collection<ISonarLintFile> excludedFiles = new ArrayList<>();
      Collection<FileWithDocument> filesToAnalyze = new ArrayList<>();

      FileExclusionsChecker exclusionsChecker = new FileExclusionsChecker(getProject());
      files.forEach(fWithDoc -> {
        if (exclusionsChecker.isExcluded(fWithDoc.getFile(), true)) {
          excludedFiles.add(fWithDoc.getFile());
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
      Map<String, String> mergedExtraProps = new LinkedHashMap<>();
      Collection<ProjectConfigurator> usedDeprecatedConfigurators = configureDeprecated(getProject(), filesToAnalyzeMap.keySet(), mergedExtraProps, monitor);

      analysisWorkDir = Files.createTempDirectory(getProject().getWorkingDir(), "sonarlint");
      List<ClientInputFile> inputFiles = buildInputFiles(analysisWorkDir, filesToAnalyzeMap);
      Collection<IAnalysisConfigurator> usedConfigurators = configure(getProject(), inputFiles, mergedExtraProps, analysisWorkDir, monitor);

      extraProps.forEach(sonarProperty -> mergedExtraProps.put(sonarProperty.getName(), sonarProperty.getValue()));

      if (!inputFiles.isEmpty()) {
        runAnalysisAndUpdateMarkers(filesToAnalyzeMap, monitor, mergedExtraProps, inputFiles, analysisWorkDir);
      }

      analysisCompleted(usedDeprecatedConfigurators, usedConfigurators, mergedExtraProps, monitor);
      SonarLintCorePlugin.getAnalysisListenerManager().notifyListeners();
      SonarLintLogger.get().debug(String.format("Done in %d ms", System.currentTimeMillis() - startTime));
    } catch (

    CanceledException e) {
      return Status.CANCEL_STATUS;
    } catch (Exception e) {
      SonarLintLogger.get().error("Error during execution of SonarLint analysis", e);
      return new Status(IStatus.WARNING, SonarLintCorePlugin.PLUGIN_ID, "Error when executing SonarLint analysis", e);
    } finally {
      if (analysisWorkDir != null) {
        try {
          FileUtils.deleteRecursively(analysisWorkDir);
        } catch (Exception e) {
          SonarLintLogger.get().debug("Unable to delete temp directory: " + analysisWorkDir, e);
        }
      }
    }

    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

  private void runAnalysisAndUpdateMarkers(Map<ISonarLintFile, IDocument> docPerFiles, final IProgressMonitor monitor,
    Map<String, String> mergedExtraProps, List<ClientInputFile> inputFiles, Path analysisWorkDir) throws CoreException {
    IPath projectLocation = getProject().getResource().getLocation();
    // In some unfrequent cases the project may be virtual and don't have physical location
    // so fallback to use analysis work dir
    Path projectBaseDir = projectLocation != null ? projectLocation.toFile().toPath() : analysisWorkDir;
    CONFIG config = prepareAnalysisConfig(projectBaseDir, inputFiles, mergedExtraProps);

    Map<ISonarLintIssuable, List<Issue>> issuesPerResource = new LinkedHashMap<>();
    docPerFiles.keySet().forEach(slFile -> issuesPerResource.put(slFile, new ArrayList<>()));

    long start = System.currentTimeMillis();
    AnalysisResults result = run(config, issuesPerResource, monitor);
    if (!monitor.isCanceled()) {
      updateMarkers(docPerFiles, issuesPerResource, result, triggerType, monitor);
      updateTelemetry(result, start);
    }
  }

  protected abstract CONFIG prepareAnalysisConfig(Path projectBaseDir, List<ClientInputFile> inputFiles, Map<String, String> mergedExtraProps);

  private static void updateTelemetry(AnalysisResults result, long start) {
    SonarLintTelemetry telemetry = SonarLintCorePlugin.getTelemetry();
    if (result.languagePerFile().size() == 1) {
      telemetry.analysisDoneOnSingleFile(result.languagePerFile().entrySet().iterator().next().getValue(), (int) (System.currentTimeMillis() - start));
    } else {
      telemetry.analysisDoneOnMultipleFiles();
    }
  }

  private static List<ClientInputFile> buildInputFiles(Path tempDirectory, final Map<ISonarLintFile, IDocument> filesToAnalyze) {
    List<ClientInputFile> inputFiles = new ArrayList<>(filesToAnalyze.size());

    for (final Map.Entry<ISonarLintFile, IDocument> fileWithDoc : filesToAnalyze.entrySet()) {
      ISonarLintFile file = fileWithDoc.getKey();
      Language language = tryDetectLanguage(file);
      boolean isTest = TestFileClassifier.get().isTest(file);
      ClientInputFile inputFile = new EclipseInputFile(isTest, file, tempDirectory, fileWithDoc.getValue(), language);
      inputFiles.add(inputFile);
    }
    return inputFiles;
  }

  @Nullable
  private static Language tryDetectLanguage(ISonarLintFile file) {
    String language = null;
    for (IFileLanguageProvider languageProvider : SonarLintExtensionTracker.getInstance().getLanguageProviders()) {
      String detectedLanguage = languageProvider.language(file);
      if (detectedLanguage != null) {
        if (language == null) {
          language = detectedLanguage;
        } else if (!language.equals(detectedLanguage)) {
          SonarLintLogger.get().error("Conflicting languages detected for file " + file.getName() + ". " + language + " and " + detectedLanguage);
        }
      }
    }
    return language != null ? Language.forKey(language).orElse(null) : null;
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
      Collection<ProjectConfigurator> configurators = SonarLintExtensionTracker.getInstance().getConfigurators();
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
    Collection<IAnalysisConfigurator> configurators = SonarLintExtensionTracker.getInstance().getAnalysisConfigurators();
    DefaultPreAnalysisContext context = new DefaultPreAnalysisContext(project, extraProperties, filesToAnalyze, tempDir);
    for (IAnalysisConfigurator configurator : configurators) {
      if (configurator.canConfigure(project)) {
        configurator.configure(context, monitor);
        usedConfigurators.add(configurator);
      }
    }

    return usedConfigurators;
  }

  private void updateMarkers(Map<ISonarLintFile, IDocument> docPerFile, Map<ISonarLintIssuable, List<Issue>> issuesPerResource, AnalysisResults result,
    TriggerType triggerType, final IProgressMonitor monitor) throws CoreException {
    Set<ISonarLintFile> failedFiles = result.failedAnalysisFiles().stream().map(ClientInputFile::<ISonarLintFile>getClientObject).collect(Collectors.toSet());
    Map<ISonarLintIssuable, List<Issue>> successfulFiles = issuesPerResource.entrySet().stream()
      .filter(e -> !failedFiles.contains(e.getKey()))
      // TODO handle non-file-level issues
      .filter(e -> e.getKey() instanceof ISonarLintFile)
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    ResourcesPlugin.getWorkspace().run(m -> trackIssues(docPerFile, successfulFiles, triggerType, monitor), monitor);
  }

  protected void trackIssues(Map<ISonarLintFile, IDocument> docPerFile, Map<ISonarLintIssuable, List<Issue>> rawIssuesPerResource, TriggerType triggerType,
    final IProgressMonitor monitor) {

    for (Map.Entry<ISonarLintIssuable, List<Issue>> entry : rawIssuesPerResource.entrySet()) {
      if (monitor.isCanceled()) {
        return;
      }
      ISonarLintFile file = (ISonarLintFile) entry.getKey();
      Optional<IDocument> openedDocument = Optional.ofNullable(docPerFile.get(file));
      IssueTracker issueTracker = SonarLintCorePlugin.getOrCreateIssueTracker(getProject());
      List<Issue> rawIssues = entry.getValue();
      List<Trackable> trackables;
      if (!rawIssues.isEmpty()) {
        IDocument document = openedDocument.orElseGet(file::getDocument);
        trackables = rawIssues.stream().map(issue -> transform(issue, file, document)).collect(Collectors.toList());
      } else {
        trackables = Collections.emptyList();
      }
      Collection<Trackable> tracked = trackFileIssues(file, trackables, issueTracker, triggerType, rawIssuesPerResource.size(), monitor);
      SonarLintMarkerUpdater.createOrUpdateMarkers(file, openedDocument, tracked, triggerType);
      // Now that markerId are set, store issues in cache
      issueTracker.updateCache(file, tracked);
    }
  }

  protected Collection<Trackable> trackFileIssues(ISonarLintFile file, List<Trackable> trackables, IssueTracker issueTracker, TriggerType triggerType, int totalTrackedFiles,
    IProgressMonitor monitor) {
    return issueTracker.matchAndTrackAsNew(file, trackables);
  }

  private static RawIssueTrackable transform(Issue issue, ISonarLintFile resource, IDocument document) {
    Integer startLine = issue.getStartLine();
    if (startLine == null) {
      return new RawIssueTrackable(issue);
    }
    TextRange textRange = TextRange.get(startLine, issue.getStartLineOffset(), issue.getEndLine(), issue.getEndLineOffset());
    String textRangeContent = readTextRangeContent(resource, document, textRange);
    String lineContent = readLineContent(resource, document, startLine);
    return new RawIssueTrackable(issue, textRange, textRangeContent, lineContent);
  }

  @Nullable
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

  @Nullable
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

  public AnalysisResults run(final CONFIG analysisConfig, final Map<ISonarLintIssuable, List<Issue>> issuesPerResource, IProgressMonitor monitor) {
    SonarLintLogger.get().debug("Starting analysis with configuration:\n" + analysisConfig.toString());
    SonarLintIssueListener issueListener = new SonarLintIssueListener(getProject(), issuesPerResource);
    AnalysisResults result = runAnalysis(analysisConfig, issueListener, monitor);
    SonarLintLogger.get().info("Found " + issueListener.getIssueCount() + " issue(s)");
    return result;
  }

  protected abstract AnalysisResults runAnalysis(CONFIG analysisConfig, SonarLintIssueListener issueListener, IProgressMonitor monitor);
}
