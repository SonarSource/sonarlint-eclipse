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
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
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
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.SonarMarker;
import org.sonarlint.eclipse.core.internal.markers.SonarMarker.Range;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.ServersManager;
import org.sonarlint.eclipse.core.internal.tracking.Input;
import org.sonarlint.eclipse.core.internal.tracking.Trackable;
import org.sonarlint.eclipse.core.internal.tracking.Tracker;
import org.sonarlint.eclipse.core.internal.tracking.Tracking;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarsource.sonarlint.core.client.api.common.analysis.ClientInputFile;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneAnalysisConfiguration;

import static org.sonarlint.eclipse.core.internal.utils.StringUtils.trimToNull;

public class AnalyzeProjectJob extends AbstractSonarProjectJob {

  private List<SonarLintProperty> extraProps;

  private final AnalyzeProjectRequest request;

  static final ISchedulingRule SONAR_ANALYSIS_RULE = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();

  public AnalyzeProjectJob(AnalyzeProjectRequest request) {
    super(jobTitle(request), SonarLintProject.getInstance(request.getProject()));
    this.request = request;
    this.extraProps = PreferencesUtils.getExtraPropertiesForLocalAnalysis(request.getProject());
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

  private final class EclipseInputFile implements ClientInputFile {
    private final List<PathMatcher> pathMatchersForTests;
    private final IFile file;
    private final Path filePath;

    private EclipseInputFile(List<PathMatcher> pathMatchersForTests, IFile file, Path filePath) {
      this.pathMatchersForTests = pathMatchersForTests;
      this.file = file;
      this.filePath = filePath;
    }

    @Override
    public java.nio.file.Path getPath() {
      return filePath;
    }

    @Override
    public boolean isTest() {
      for (PathMatcher matcher : pathMatchersForTests) {
        if (matcher.matches(filePath)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public Charset getCharset() {
      try {
        return Charset.forName(file.getCharset());
      } catch (CoreException e) {
        return null;
      }
    }

    @Override
    public <G> G getClientObject() {
      return (G) file;
    }
  }

  private static class PreviousMarkerCache {
    Map<IResource, List<IMarker>> markersByResource = new HashMap<>();

    public PreviousMarkerCache(AnalyzeProjectRequest request) {
      if (request.getFiles() != null) {
        for (IFile file : request.getFiles()) {
          markersByResource.put(file, new ArrayList<IMarker>(MarkerUtils.findMarkers(file)));
        }
      } else {
        for (IMarker m : MarkerUtils.findMarkers(request.getProject())) {
          if (!markersByResource.containsKey(m.getResource())) {
            markersByResource.put(m.getResource(), new ArrayList<IMarker>());
          }
          markersByResource.get(m.getResource()).add(m);
        }
      }
    }

    public void deleteUnmatched() throws CoreException {
      for (List<IMarker> entry : markersByResource.values()) {
        for (IMarker m : entry) {
          m.delete();
        }
      }
    }

    public List<IMarker> getPrevious(IResource r) {
      return markersByResource.containsKey(r) ? markersByResource.get(r) : Collections.<IMarker>emptyList();
    }
  }

  @Override
  protected IStatus doRun(final IProgressMonitor monitor) {

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
    if (sonarProject.isBound()) {
      config = new ConnectedAnalysisConfiguration(trimToNull(sonarProject.getModuleKey()), project.getLocation().toFile().toPath(),
        projectSpecificWorkDir.toFile().toPath(), inputFiles, mergedExtraProps);
    } else {
      config = new StandaloneAnalysisConfiguration(project.getLocation().toFile().toPath(), projectSpecificWorkDir.toFile().toPath(), inputFiles, mergedExtraProps);
    }

    Map<IResource, List<Issue>> issuesPerResource = run(config, sonarProject, monitor);
    updateMarkers(issuesPerResource);
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
        final java.nio.file.Path filePath = localFile.toPath();
        inputFiles.add(new EclipseInputFile(pathMatchersForTests, file, filePath));
      } catch (CoreException e) {
        SonarLintCorePlugin.getDefault().error("Error building input file for SonarLint analysis", e);
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

  private void updateMarkers(Map<IResource, List<Issue>> issuesPerResource) throws CoreException {
    ITextFileBufferManager iTextFileBufferManager = FileBuffers.getTextFileBufferManager();
    if (iTextFileBufferManager == null) {
      return;
    }
    PreviousMarkerCache markerCache = new PreviousMarkerCache(this.request);
    for (Entry<IResource, List<Issue>> resourceEntry : issuesPerResource.entrySet()) {
      IResource r = resourceEntry.getKey();
      try {
        List<IMarker> previousMarkers = markerCache.getPrevious(r);
        List<Issue> rawIssues = resourceEntry.getValue();
        if (r instanceof IFile) {
          issueTrackingOnFile(iTextFileBufferManager, r, previousMarkers, rawIssues);
        } else {
          issueTracking(r, previousMarkers, rawIssues, null);
        }
      } catch (Exception e) {
        SonarLintCorePlugin.getDefault().error("Unable to compute position of SonarLint marker on resource " + r.getName(), e);
      }
    }
    markerCache.deleteUnmatched();
  }

  private static void issueTrackingOnFile(ITextFileBufferManager iTextFileBufferManager, IResource r, List<IMarker> previousMarkers, List<Issue> rawIssues)
    throws CoreException, BadLocationException {
    IFile iFile = (IFile) r;
    try {
      iTextFileBufferManager.connect(iFile.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
      ITextFileBuffer iTextFileBuffer = iTextFileBufferManager.getTextFileBuffer(iFile.getFullPath(), LocationKind.IFILE);
      IDocument iDoc = iTextFileBuffer.getDocument();

      issueTracking(r, previousMarkers, rawIssues, iDoc);

    } finally {
      try {
        iTextFileBufferManager.disconnect(iFile.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
      } catch (CoreException e) {
        // Ignore
      }
    }
  }

  private static void issueTracking(IResource r, List<IMarker> previousMarkers, List<Issue> rawIssues, IDocument iDoc) throws BadLocationException, CoreException {
    Input<TrackableMarker> baseInput = prepareBaseInput(previousMarkers);
    Input<TrackableIssue> rawInput = prepareRawInput(iDoc, rawIssues);
    Tracking<TrackableIssue, TrackableMarker> tracking = new Tracker<TrackableIssue, TrackableMarker>().track(rawInput, baseInput);
    for (Entry<TrackableIssue, TrackableMarker> entry : tracking.getMatchedRaws().entrySet()) {
      Issue issue = entry.getKey().getWrapped();
      IMarker marker = entry.getValue().getWrapped();
      previousMarkers.remove(marker);
      SonarMarker.updateAttributes(marker, issue, iDoc);
    }
    for (TrackableIssue newIssue : tracking.getUnmatchedRaws()) {
      SonarMarker.create(iDoc, r, newIssue.getWrapped());
    }
  }

  private static void analysisCompleted(Collection<ProjectConfigurator> usedConfigurators, Map<String, String> properties, final IProgressMonitor monitor) {
    for (ProjectConfigurator p : usedConfigurators) {
      p.analysisComplete(Collections.unmodifiableMap(properties), monitor);
    }

  }

  public Map<IResource, List<Issue>> run(final StandaloneAnalysisConfiguration config, final SonarLintProject project, final IProgressMonitor monitor) {
    SonarLintCorePlugin.getDefault().debug("Start analysis with configuration:\n" + config.toString());
    Thread.UncaughtExceptionHandler h = exceptionHandler();
    final Map<IResource, List<Issue>> issuesPerResource = new HashMap<>();
    Thread t = new Thread("SonarLint analysis") {
      @Override
      public void run() {
        if (StringUtils.isNotBlank(project.getServerId())) {
          IServer server = ServersManager.getInstance().getServer(project.getServerId());
          if (server == null) {
            throw new IllegalStateException(
              "Project '" + project.getProject().getName() + "' is linked to an unknow server: '" + project.getServerId() + "'. Please bind project again.");
          }
          server.startAnalysis((ConnectedAnalysisConfiguration) config, new SonarLintIssueListener(issuesPerResource));
        } else {
          StandaloneSonarLintClientFacade facadeToUse = SonarLintCorePlugin.getDefault().getDefaultSonarLintClientFacade();
          facadeToUse.startAnalysis(config, new SonarLintIssueListener(issuesPerResource));
        }
      }
    };
    t.setDaemon(true);
    t.setUncaughtExceptionHandler(h);
    t.start();
    waitForThread(monitor, t);
    return issuesPerResource;
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

  private static Thread.UncaughtExceptionHandler exceptionHandler() {
    return new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread th, Throwable ex) {
        SonarLintCorePlugin.getDefault().error("Error during analysis", ex);
      }
    };
  }

  private static Input<TrackableMarker> prepareBaseInput(List<IMarker> previous) {
    final List<TrackableMarker> wrapped = wrap(previous);
    return new Input<AnalyzeProjectJob.TrackableMarker>() {
      @Override
      public Collection<TrackableMarker> getIssues() {
        return wrapped;
      }
    };
  }

  private static List<TrackableMarker> wrap(List<IMarker> previous) {
    final List<TrackableMarker> wrapped = new ArrayList<>();
    for (IMarker marker : previous) {
      wrapped.add(new TrackableMarker(marker));
    }
    return wrapped;
  }

  private static Input<TrackableIssue> prepareRawInput(IDocument iDoc, List<Issue> issues) throws BadLocationException {
    final List<TrackableIssue> wrapped = wrap(iDoc, issues);
    return new Input<AnalyzeProjectJob.TrackableIssue>() {
      @Override
      public Collection<TrackableIssue> getIssues() {
        return wrapped;
      }
    };
  }

  private static List<TrackableIssue> wrap(IDocument iDoc, List<Issue> issues) throws BadLocationException {
    final List<TrackableIssue> wrapped = new ArrayList<>();
    for (Issue issue : issues) {
      Integer checksum = iDoc != null ? computeChecksum(iDoc, issue) : null;
      wrapped.add(new TrackableIssue(issue, checksum));
    }
    return wrapped;
  }

  private static Integer computeChecksum(IDocument iDoc, Issue issue) throws BadLocationException {
    Integer checksum;
    Integer startLine = issue.getStartLine();
    if (startLine == null) {
      checksum = null;
    } else {
      Range rangeInFile = SonarMarker.findRangeInFile(issue, iDoc);
      checksum = SonarMarker.checksum(rangeInFile.getContent());
    }
    return checksum;
  }

  private static class TrackableIssue implements Trackable {

    private final Issue issue;
    private final Integer lineHash;

    public TrackableIssue(Issue issue, Integer lineHash) {
      this.issue = issue;
      this.lineHash = lineHash;
    }

    public Issue getWrapped() {
      return issue;
    }

    @Override
    public Integer getLine() {
      return issue.getStartLine();
    }

    @Override
    public String getMessage() {
      return SonarMarker.getMessage(issue);
    }

    @Override
    public Integer getLineHash() {
      return lineHash;
    }

    @Override
    public String getRuleKey() {
      return issue.getRuleKey();
    }

  }

  private static class TrackableMarker implements Trackable {

    private final IMarker marker;

    public TrackableMarker(IMarker marker) {
      this.marker = marker;
    }

    public IMarker getWrapped() {
      return marker;
    }

    @Override
    public Integer getLine() {
      int line = marker.getAttribute(IMarker.LINE_NUMBER, 0);
      return line != 0 ? line : null;
    }

    @Override
    public String getMessage() {
      return marker.getAttribute(IMarker.MESSAGE, "");
    }

    @Override
    public Integer getLineHash() {
      int attribute = marker.getAttribute(MarkerUtils.SONAR_MARKER_CHECKSUM_ATTR, 0);
      return attribute != 0 ? attribute : null;
    }

    @Override
    public String getRuleKey() {
      return marker.getAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, "");
    }

  }

}
