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
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
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
import org.sonarlint.eclipse.core.internal.event.AnalysisEvent;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.internal.tracking.ProjectIssueTracker;
import org.sonarlint.eclipse.core.internal.tracking.RawIssueTrackable;
import org.sonarlint.eclipse.core.internal.utils.FileExclusionsChecker;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.analysis.api.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.AbstractAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.commons.Language;
import org.sonarsource.sonarlint.core.commons.TextRange;
import org.sonarsource.sonarlint.core.commons.progress.CanceledException;
import org.sonarsource.sonarlint.core.serverconnection.FileUtils;

import static java.text.MessageFormat.format;

public abstract class AbstractAnalyzeProjectJob<CONFIG extends AbstractAnalysisConfiguration> extends AbstractSonarProjectJob {
  private final List<SonarLintProperty> extraProps;
  private final TriggerType triggerType;
  private final boolean shouldClearReport;
  private final boolean checkUnsupportedLanguages;
  private final Collection<FileWithDocument> files;
  private final EnumSet<Language> unavailableLanguagesReference = EnumSet.noneOf(Language.class);

  protected AbstractAnalyzeProjectJob(AnalyzeProjectRequest request) {
    super(jobTitle(request), request.getProject());
    this.extraProps = SonarLintGlobalConfiguration.getExtraPropertiesForLocalAnalysis(request.getProject());
    this.files = request.getFiles();
    this.triggerType = request.getTriggerType();
    this.shouldClearReport = request.shouldClearReport();
    this.checkUnsupportedLanguages = request.checkUnsupportedLanguages();
  }

  public static AbstractSonarProjectJob create(AnalyzeProjectRequest request) {
    return SonarLintCorePlugin.getConnectionManager()
      .resolveBinding(request.getProject())
      .<AbstractSonarProjectJob>map(b -> new AnalyzeConnectedProjectJob(request, b.getProjectBinding(), b.getEngineFacade()))
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
    var startTime = System.currentTimeMillis();
    SonarLintLogger.get().debug("Trigger: " + triggerType.name());

    Path analysisWorkDir = null;
    try {
      var excludedFiles = new ArrayList<ISonarLintFile>();
      var filesToAnalyze = new ArrayList<FileWithDocument>();

      var exclusionsChecker = new FileExclusionsChecker(getProject());
      files.forEach(fWithDoc -> {
        var file = fWithDoc.getFile();
        if (exclusionsChecker.isExcluded(file, true) || isScmIgnored(file)) {
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
        runAnalysisAndUpdateMarkers(filesToAnalyzeMap, monitor, mergedExtraProps, inputFiles, analysisWorkDir);
      }

      analysisCompleted(usedDeprecatedConfigurators, usedConfigurators, mergedExtraProps, monitor);

      SonarLintCorePlugin.getAnalysisListenerManager().notifyListeners(new AnalysisEvent() {
        @Override
        public Set<Language> getUnavailableLanguages() {
          return unavailableLanguagesReference;
        }

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
        try {
          FileUtils.deleteRecursively(analysisWorkDir);
        } catch (Exception e) {
          SonarLintLogger.get().debug("Unable to delete temp directory: " + analysisWorkDir, e);
        }
      }
    }

    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

  private static boolean isScmIgnored(ISonarLintFile file) {
    var ignored = file.isScmIgnored();
    if (ignored) {
      SonarLintLogger.get().debug("File '" + file.getName() + "' skipped from analysis because it is ignored by SCM");
    }
    return ignored;
  }

  private void runAnalysisAndUpdateMarkers(Map<ISonarLintFile, IDocument> docPerFiles, final IProgressMonitor monitor,
    Map<String, String> mergedExtraProps, List<ClientInputFile> inputFiles, Path analysisWorkDir) throws CoreException {
    var projectLocation = getProject().getResource().getLocation();
    // In some unfrequent cases the project may be virtual and don't have physical location
    // so fallback to use analysis work dir
    var projectBaseDir = projectLocation != null ? projectLocation.toFile().toPath() : analysisWorkDir;
    var config = prepareAnalysisConfig(projectBaseDir, inputFiles, mergedExtraProps);

    var issuesPerResource = new LinkedHashMap<ISonarLintIssuable, List<Issue>>();
    docPerFiles.keySet().forEach(slFile -> issuesPerResource.put(slFile, new ArrayList<>()));

    var start = System.currentTimeMillis();
    var result = run(config, issuesPerResource, monitor);
    if (!monitor.isCanceled()) {
      updateMarkers(docPerFiles, issuesPerResource, result, triggerType, monitor);
      updateTelemetry(result, start, issuesPerResource);

      if (checkUnsupportedLanguages) {
        // Collect all the languages we just analyzed and that are unavailable in standalone mode. This will be re-used
        // to handle it accordingly in the UI (e.g. display notification to the user).
        var bindingOpt = SonarLintCorePlugin.getConnectionManager().resolveBinding(getProject());
        if (bindingOpt.isEmpty()) {
          var languages = result.languagePerFile().values().stream().collect(Collectors.toCollection(HashSet::new));
          var languagesConnectedMode = EnumSet.noneOf(Language.class);
          languagesConnectedMode.addAll(SonarLintUtils.CONNECTED_MODE_LANGUAGES);
          languagesConnectedMode.addAll(SonarLintUtils.CONNECTED_MODE_LANGUAGES_CDT);
          languages.retainAll(languagesConnectedMode);
          unavailableLanguagesReference.addAll(languages);
        }
      }
    }
  }

  protected abstract CONFIG prepareAnalysisConfig(Path projectBaseDir, List<ClientInputFile> inputFiles, Map<String, String> mergedExtraProps);

  private static void updateTelemetry(AnalysisResults result, long start, Map<ISonarLintIssuable, List<Issue>> issuesPerResource) {
    var telemetry = SonarLintCorePlugin.getTelemetry();
    if (result.languagePerFile().size() == 1) {
      telemetry.analysisDoneOnSingleFile(result.languagePerFile().entrySet().iterator().next().getValue(), (int) (System.currentTimeMillis() - start));
    } else {
      telemetry.analysisDoneOnMultipleFiles();
    }
    telemetry.addReportedRules(issuesPerResource.values().stream().flatMap(Collection::stream).map(Issue::getRuleKey).collect(Collectors.toSet()));
  }

  private static List<ClientInputFile> buildInputFiles(Path tempDirectory, final Map<ISonarLintFile, IDocument> filesToAnalyze) {
    var inputFiles = new ArrayList<ClientInputFile>(filesToAnalyze.size());

    for (final var fileWithDoc : filesToAnalyze.entrySet()) {
      var file = fileWithDoc.getKey();
      var language = tryDetectLanguage(file);
      var isTest = TestFileClassifier.get().isTest(file);
      var inputFile = new EclipseInputFile(isTest, file, tempDirectory, fileWithDoc.getValue(), language);
      inputFiles.add(inputFile);
    }
    return inputFiles;
  }

  @Nullable
  private static Language tryDetectLanguage(ISonarLintFile file) {
    String language = null;
    for (var languageProvider : SonarLintExtensionTracker.getInstance().getLanguageProviders()) {
      var detectedLanguage = languageProvider.language(file);
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

  private static Collection<IAnalysisConfigurator> configure(final ISonarLintProject project, List<ClientInputFile> filesToAnalyze,
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

  private void updateMarkers(Map<ISonarLintFile, IDocument> docPerFile, Map<ISonarLintIssuable, List<Issue>> issuesPerResource, AnalysisResults result,
    TriggerType triggerType, final IProgressMonitor monitor) throws CoreException {
    var failedFiles = result.failedAnalysisFiles().stream().map(ClientInputFile::<ISonarLintFile>getClientObject).collect(Collectors.toSet());
    var successfulFiles = issuesPerResource.entrySet().stream()
      .filter(e -> !failedFiles.contains(e.getKey()))
      // TODO handle non-file-level issues
      .filter(e -> e.getKey() instanceof ISonarLintFile)
      .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    ResourcesPlugin.getWorkspace().run(m -> trackIssues(docPerFile, successfulFiles, triggerType, monitor), monitor);
  }

  protected void trackIssues(Map<ISonarLintFile, IDocument> docPerFile,
    Map<ISonarLintIssuable, List<Issue>> rawIssuesPerResource, TriggerType triggerType,
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

  private static RawIssueTrackable transform(Issue issue, ISonarLintFile resource, IDocument document) {
    var startLine = issue.getStartLine();
    if (startLine == null) {
      return new RawIssueTrackable(issue);
    }
    var textRangeContent = readTextRangeContent(resource, document, issue.getTextRange());
    var lineContent = readLineContent(resource, document, startLine);
    return new RawIssueTrackable(issue, textRangeContent, lineContent);
  }

  @Nullable
  private static String readTextRangeContent(ISonarLintFile resource, IDocument document, @Nullable TextRange textRange) {
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

  public AnalysisResults run(final CONFIG analysisConfig, final Map<ISonarLintIssuable, List<Issue>> issuesPerResource, IProgressMonitor monitor) {
    SonarLintLogger.get().debug("Starting analysis with configuration:\n" + analysisConfig.toString());
    var issueListener = new SonarLintIssueListener(getProject(), issuesPerResource);
    var result = runAnalysis(analysisConfig, issueListener, monitor);
    SonarLintLogger.get().info("Found " + issueListener.getIssueCount() + " issue(s)");
    return result;
  }

  protected abstract AnalysisResults runAnalysis(CONFIG analysisConfig, SonarLintIssueListener issueListener, IProgressMonitor monitor);
}
