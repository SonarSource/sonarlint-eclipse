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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.assertj.core.groups.Tuple;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.LogListener;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.event.AnalysisEvent;
import org.sonarlint.eclipse.core.internal.event.AnalysisListener;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.preferences.RuleConfig;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintFileAdapter;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class AnalyzeStandaloneProjectJobTest extends SonarTestCase {

  private static LogListener listener;
  private static IProject project;

  protected static class MarkerUpdateListener implements AnalysisListener {
    private CountDownLatch markersUpdatedLatch = new CountDownLatch(0);

    public void prepareOneAnalysis() {
      markersUpdatedLatch = new CountDownLatch(1);
    }

    public boolean waitForMarkers() throws InterruptedException {
      return markersUpdatedLatch.await(1, TimeUnit.MINUTES);
    }

    @Override
    public void usedAnalysis(AnalysisEvent event) {
      markersUpdatedLatch.countDown();
    }
  }

  protected static MarkerUpdateListener markerUpdateListener = new MarkerUpdateListener();

  protected static void prepareOneAnalysis() {
    markerUpdateListener.markersUpdatedLatch = new CountDownLatch(1);
  }

  @BeforeClass
  public static void addLogListener() throws IOException, CoreException, InterruptedException {
    listener = new LogListener() {

      @Override
      public void info(String msg, boolean fromAnalyzer) {
        System.out.println("INFO " + msg);
      }

      @Override
      public void error(String msg, boolean fromAnalyzer) {
        System.err.println("ERROR " + msg);
      }

      @Override
      public void debug(String msg, boolean fromAnalyzer) {
        System.out.println("DEBUG " + msg);
      }

      @Override
      public void traceIdeMessage(@Nullable String msg) {
        // INFO: We ignore Eclipse-specific tracing in UTs
      }
    };
    SonarLintLogger.get().addLogListener(listener);

    SonarLintCorePlugin.getAnalysisListenerManager().addListener(markerUpdateListener);

    project = importEclipseProject("SimpleJdtProject");

    // After importing projects we have to await them being readied by SLCORE:
    // -> first they are not yet ready when imported
    var allProjectsReady = new CountDownLatch(1);
    Executors.newSingleThreadExecutor().submit(() -> {
      while (true) {
        var map = new HashMap<String, Boolean>(AnalysisReadyStatusCache.getCache());
        if (!map.isEmpty() && map.values().stream().allMatch(Boolean::booleanValue)) {
          allProjectsReady.countDown();
          break;
        }
        try {
          Thread.sleep(200);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    assertThat(allProjectsReady.await(5, TimeUnit.SECONDS)).isTrue();
  }

  @AfterClass
  public static void removeLogListener() {
    SonarLintCorePlugin.getAnalysisListenerManager().removeListener(markerUpdateListener);
    SonarLintLogger.get().removeLogListener(listener);
  }

  @Before
  public void clean() throws BackingStoreException {
    ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID).clear();
  }

  @After
  public void cleanup() {
    SonarLintMarkerUpdater.deleteAllMarkersFromReport();
  }

  @Test
  public void analyzeWithRuleParameters() throws Exception {
    // Don't run this test on macOS devices as Node.js might not be found!
    Assume.assumeFalse(Platform.getOS().equals("macosx"));

    var file = (IFile) project.findMember("src/main/sample.js");
    var slProject = new DefaultSonarLintProjectAdapter(project);
    var fileToAnalyze = new FileWithDocument(new DefaultSonarLintFileAdapter(slProject, file), null);
    var ruleConfig = new RuleConfig("javascript:S100", true);
    ruleConfig.getParams().put("format", "^[0-9]+$");
    SonarLintGlobalConfiguration.saveRulesConfig(List.of(ruleConfig));

    markerUpdateListener.prepareOneAnalysis();
    var underTest = new AnalyzeProjectJob(new AnalyzeProjectRequest(slProject, List.of(fileToAnalyze),
      TriggerType.EDITOR_CHANGE, false));
    underTest.schedule();
    assertThat(underTest.join(100_000, new NullProgressMonitor())).isTrue();
    var status = underTest.getResult();
    assertThat(status.isOK()).isTrue();
    assertThat(markerUpdateListener.waitForMarkers()).isTrue();

    var markers = List.of(file.findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE))
      .contains(tuple("/SimpleJdtProject/src/main/sample.js", 1, "Rename this 'hello' function to match the regular expression '^[0-9]+$'."));
  }

  @Test
  public void analyze_should_only_triggers_a_single_event_for_all_marker_operations() throws Exception {
    var slProject = new DefaultSonarLintProjectAdapter(project);
    var file1ToAnalyze = prepareFile1(project, slProject);
    var file2ToAnalyze = prepareFile2(project, slProject);

    var mcl = new MarkerChangeListener();
    workspace.addResourceChangeListener(mcl);

    try {
      markerUpdateListener.prepareOneAnalysis();
      var underTest = new AnalyzeProjectJob(
        new AnalyzeProjectRequest(slProject, List.of(file1ToAnalyze, file2ToAnalyze), TriggerType.EDITOR_CHANGE, false));
      underTest.schedule();
      assertThat(underTest.join(100_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();
      assertThat(markerUpdateListener.waitForMarkers()).isTrue();

      // INFO: There should be one event coming in as the files just got new markers
      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_ON_THE_FLY_ID);
      awaitAssertions(() -> assertThat(mcl.getEventCount()).isEqualTo(1));

      // Run the same analysis a second time to ensure the behavior is the same when markers are already present
      mcl.clearCounter();

      markerUpdateListener.prepareOneAnalysis();
      underTest.schedule();
      assertThat(underTest.join(100_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();
      assertThat(markerUpdateListener.waitForMarkers()).isTrue();

      // One event, as on-the-fly markers aren't removed before the analysis and added afterwards: Only afterwards we
      // try to check if we can re-use existing ones, otherwise delete old and create new ones. This is done in an
      // atomic operation via "ResourcesPlugin.getWorkspace().run(m -> { ... });"!
      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_ON_THE_FLY_ID);
      awaitAssertions(() -> assertThat(mcl.getEventCount()).isEqualTo(1));
    } finally {
      mcl.clearCounter();
      workspace.removeResourceChangeListener(mcl);
    }

  }

  @Test
  public void analyze_report_should_only_triggers_two_events_for_all_marker_operations_when_clear_report() throws Exception {
    var slProject = new DefaultSonarLintProjectAdapter(project);
    var file1ToAnalyze = prepareFile1(project, slProject);
    var file2ToAnalyze = prepareFile2(project, slProject);

    var mcl = new MarkerChangeListener();
    workspace.addResourceChangeListener(mcl);

    try {
      markerUpdateListener.prepareOneAnalysis();
      var underTest = new AnalyzeProjectJob(
        new AnalyzeProjectRequest(slProject, List.of(file1ToAnalyze, file2ToAnalyze), TriggerType.MANUAL, true));
      underTest.schedule();
      assertThat(underTest.join(100_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();
      assertThat(markerUpdateListener.waitForMarkers()).isTrue();

      // INFO: There should be one event coming in as the files just got new markers
      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_REPORT_ID);
      awaitAssertions(() -> assertThat(mcl.getEventCount()).isEqualTo(1));

      // Run the same analysis a second time to ensure the behavior is the same when markers are already present
      mcl.clearCounter();

      markerUpdateListener.prepareOneAnalysis();
      underTest.schedule();
      assertThat(underTest.join(100_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();
      assertThat(markerUpdateListener.waitForMarkers()).isTrue();

      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_REPORT_ID);

      // Two events, one for clearing past markers, one to create new markers
      // For report markers we delete them all before the analysis and don't re-use them like for the on-the-fly markers
      awaitAssertions(() -> assertThat(mcl.getEventCount()).isEqualTo(2));
    } finally {
      mcl.clearCounter();
      workspace.removeResourceChangeListener(mcl);
    }

  }

  @Test
  public void analyzeWithQuickFixesWhenFileIsClosed() throws Exception {
    var file = (IFile) project.findMember("src/main/java/com/quickfix/FileWithQuickFixes.java");
    var slProject = new DefaultSonarLintProjectAdapter(project);
    var fileToAnalyze = new FileWithDocument(new DefaultSonarLintFileAdapter(slProject, file), null);

    markerUpdateListener.prepareOneAnalysis();
    var underTest = new AnalyzeProjectJob(
      new AnalyzeProjectRequest(slProject, List.of(fileToAnalyze), TriggerType.EDITOR_CHANGE, false));
    underTest.schedule();
    assertThat(underTest.join(20_000, new NullProgressMonitor())).isTrue();
    var status = underTest.getResult();
    assertThat(status.isOK()).isTrue();
    assertThat(markerUpdateListener.waitForMarkers()).isTrue();

    assertThat(List.of(file.findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE)))
      .extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE, MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR))
      .contains(tuple("/SimpleJdtProject/src/main/java/com/quickfix/FileWithQuickFixes.java", 8,
        "Replace the type specification in this constructor call with the diamond operator (\"<>\").", "java:S2293"));

    var markers = List.of(file.findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    var markerWithQuickFix = markers.stream().filter(m -> m.getAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR, "").equals("java:S2293")).findFirst().get();
    var issueQuickFixes = MarkerUtils.getIssueQuickFixes(markerWithQuickFix);
    assertThat(issueQuickFixes.getQuickFixes()).hasSize(1);
    var markerQuickFix = issueQuickFixes.getQuickFixes().get(0);
    assertThat(markerQuickFix.getMessage()).isEqualTo("Replace with <>");
    assertThat(markerQuickFix.getTextEdits()).hasSize(1);
    var markerTextEdit = markerQuickFix.getTextEdits().get(0);
    assertThat(markerTextEdit.getNewText()).isEqualTo("<>");
    assertThat(markerTextEdit.getMarker()).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE, IMarker.CHAR_START, IMarker.CHAR_END))
      .isEqualTo(tuple("/SimpleJdtProject/src/main/java/com/quickfix/FileWithQuickFixes.java", 8, null, 158, 166));

  }

  private void verifyMarkers(FileWithDocument file1ToAnalyze, FileWithDocument file2ToAnalyze, String markerType) throws CoreException {
    var markers1 = List.of(file1ToAnalyze.getFile().getResource().findMarkers(markerType, true, IResource.DEPTH_ONE));
    assertThat(markers1).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).hasSize(6);
    var markers2 = List.of(file2ToAnalyze.getFile().getResource().findMarkers(markerType, true, IResource.DEPTH_ONE));
    assertThat(markers2).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).hasSize(6);
  }

  private FileWithDocument prepareFile2(IProject project, DefaultSonarLintProjectAdapter slProject) {
    var file2 = (IFile) project.findMember("src/main/java/com/sonarsource/NpeWithFlow2.java");
    return new FileWithDocument(new DefaultSonarLintFileAdapter(slProject, file2), null);
  }

  private FileWithDocument prepareFile1(IProject project, DefaultSonarLintProjectAdapter slProject) {
    var file1 = (IFile) project.findMember("src/main/java/com/sonarsource/NpeWithFlow.java");
    return new FileWithDocument(new DefaultSonarLintFileAdapter(slProject, file1), null);
  }

  public static MarkerAttributesExtractor markerAttributes(String... attributes) {
    return new MarkerAttributesExtractor(attributes);
  }

  private static class MarkerChangeListener implements IResourceChangeListener {

    private final AtomicInteger markerChangeEventChangeCount = new AtomicInteger(0);

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
      if (isMarkerChangeEvent(event.getDelta())) {
        markerChangeEventChangeCount.incrementAndGet();
      }
    }

    private boolean isMarkerChangeEvent(IResourceDelta delta) {
      var flags = delta.getFlags();
      if ((flags & IResourceDelta.MARKERS) > 0) {
        System.out.println("Changed markers: " + delta.getMarkerDeltas().length);
        return true;
      }
      for (var child : delta.getAffectedChildren()) {
        if (isMarkerChangeEvent(child)) {
          return true;
        }
      }
      return false;
    }

    public int getEventCount() {
      return markerChangeEventChangeCount.get();
    }

    public void clearCounter() {
      markerChangeEventChangeCount.set(0);
    }
  }

  public static class MarkerAttributesExtractor implements Function<IMarker, Tuple> {

    private final String[] attributes;

    public MarkerAttributesExtractor(String... attributes) {
      this.attributes = attributes;
    }

    @Override
    public Tuple apply(IMarker marker) {
      var tupleAttributes = new Object[attributes.length + 1];
      tupleAttributes[0] = marker.getResource().getFullPath().toPortableString();
      for (var i = 0; i < attributes.length; i++) {
        try {
          tupleAttributes[i + 1] = marker.getAttribute(attributes[i]);
        } catch (CoreException e) {
          throw new IllegalStateException("Unable to get attribute '" + attributes[i] + "'");
        }
      }
      return new Tuple(tupleAttributes);
    }
  }

}
