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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.SonarLintNotifications;
import org.sonarlint.eclipse.core.analysis.IAnalysisConfigurator;
import org.sonarlint.eclipse.core.analysis.IPostAnalysisContext;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.internal.backend.RunningAnalysesTracker;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.internal.utils.FileExclusionsChecker;
import org.sonarlint.eclipse.core.internal.utils.FileUtils;
import org.sonarlint.eclipse.core.internal.utils.JobUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.commons.api.progress.CanceledException;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.analysis.AnalyzeFilesResponse;

import static java.text.MessageFormat.format;

public class AnalyzeProjectJob extends AbstractSonarProjectJob {
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
    return new AnalyzeProjectJob(request);
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

      // We have to remove all markers from the report view first as SonarLintMarkerUpdater works per file and the
      // SonarLint Report view can have markers from multiple files we might not catch: E.g. analyzed two files first
      // and afterwards only analyzed one -> markers from both are shown!
      if (shouldClearReport) {
        ResourcesPlugin.getWorkspace().run(m -> SonarLintMarkerUpdater.deleteAllMarkersFromReport(), monitor);
      }

      // This is for working with the CDT integration and the CFamily analysis: It can be that files have to be
      // removed from the analysis because no necessary information could be gathered and otherwise the analysis will
      // fail as a whole.
      var removedFilesByCdt = mergedExtraProps.get(SonarLintUtils.SONARLINT_ANALYSIS_CDT_EXCLUSION_PROPERY);
      if (removedFilesByCdt != null) {
        SonarLintNotifications.get()
          .showNotification(new SonarLintNotifications.Notification(
            "C/C++ analysis not available (yet)",
            "Some C/C++ files were removed from the analysis by the CDT integration as no information was available "
              + "from the Eclipse CDT plug-in. This might happen when the project was not yet built or the project is "
              + "not configured correctly.",
            "The following files were removed from the analysis: '" + removedFilesByCdt + "'. The information "
              + "provided by Eclipse CDT is crucial for the analysis to work correctly, please consult the official "
              + "documentation at: " + SonarLintDocumentation.ECLIPSE_CDT_DOCS,
            SonarLintDocumentation.ECLIPSE_CDT_DOCS));

        for (var uri : StringUtils.splitFromCommaString(removedFilesByCdt)) {
          inputFiles = inputFiles.stream()
            .filter(eclipseFile -> !eclipseFile.getFile().getResource().getLocationURI().toString().equals(uri))
            .collect(Collectors.toList());
        }
      }

      if (!inputFiles.isEmpty()) {
        var start = System.currentTimeMillis();
        var result = run(filesToAnalyzeMap.keySet(), mergedExtraProps, start, monitor);
      }

      analysisCompleted(usedDeprecatedConfigurators, usedConfigurators, mergedExtraProps, monitor);
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
      return AnalysisReadyStatusCache.getAnalysisReadiness(ConfigScopeSynchronizer.getConfigScopeId(project));
    }

    var configurationScopeIds = files.stream()
      .map(file -> file.getFile().getProject())
      .map(ConfigScopeSynchronizer::getConfigScopeId)
      .collect(Collectors.toSet());

    // We have to check that all the affected projects (denoted by configuration scope id) are ready!
    // -> when even one is not ready, the whole bundle is not ready and we return false
    for (var configurationScopeId : configurationScopeIds) {
      if (!AnalysisReadyStatusCache.getAnalysisReadiness(configurationScopeId)) {
        return false;
      }
    }
    return true;
  }

  private static boolean isScmIgnored(ISonarLintFile file) {
    var ignored = file.isScmIgnored();
    if (ignored) {
      SonarLintLogger.get().debug("File '" + file.getName() + "' skipped from analysis because it is ignored by SCM");
    }
    return ignored;
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

  private AnalyzeFilesResponse run(final Set<ISonarLintFile> files, final Map<String, String> extraProps,
    long startTime, IProgressMonitor monitor) {
    var fileURIs = files.stream().map(slFile -> slFile.getResource().getLocationURI()).collect(Collectors.toList());

    var analysisId = UUID.randomUUID();
    var analysisState = new AnalysisState(analysisId, fileURIs, triggerType);

    try {
      RunningAnalysesTracker.get().track(analysisState);

      var future = SonarLintBackendService.get().analyzeFilesAndTrack(getProject(), analysisId, fileURIs, extraProps, triggerType.shouldFetch(), startTime);
      return JobUtils.waitForFutureInJob(monitor, future);
    } catch (Exception err) {
      // If the analysis fails we assume that there will also be no "raiseIssues(...)" called. If so, we only handle it
      // incorrectly if this fails on a manual analysis invocation (we assume it is an update coming from SonarLint
      // Core to existing issues).
      // If we wouldn't "finish" the analysis here and there would be no "raiseIssues(...)" with
      // "isIntermediatePublication" set to false called, this AnalysisState object would still be in memory until the
      // IDE (ergo SonarLint) is restarted.
      new FinishAnalysisStateJob(analysisState).schedule(5000);

      if (err instanceof InterruptedException) {
        Thread.currentThread().interrupt();
        throw new CanceledException();
      } else {
        throw new IllegalStateException(err);
      }
    }
  }

  /**
   *  As the "raiseIssues(...)" calls can come in asynchronously if the "analyzeFilesAndTrack" throws an exception
   *  right at the end we offer any of this calls some time to come in.
   */
  private static class FinishAnalysisStateJob extends AbstractSonarJob {
    private final AnalysisState analysisState;

    protected FinishAnalysisStateJob(AnalysisState analysisState) {
      super("Finish analysis " + analysisState.getId());
      this.analysisState = analysisState;
    }

    @Override
    protected IStatus doRun(IProgressMonitor monitor) throws CoreException {
      RunningAnalysesTracker.get().finish(analysisState);
      return Status.OK_STATUS;
    }
  }
}
