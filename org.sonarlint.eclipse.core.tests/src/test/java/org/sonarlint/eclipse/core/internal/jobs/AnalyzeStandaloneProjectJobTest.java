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
import java.util.List;
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
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.LogListener;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
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

  @BeforeClass
  public static void addLogListener() throws IOException, CoreException {
    listener = new LogListener() {

      @Override
      public void info(String msg, boolean fromAnalyzer) {
      }

      @Override
      public void error(String msg, boolean fromAnalyzer) {
        System.err.println(msg);
      }

      @Override
      public void debug(String msg, boolean fromAnalyzer) {
      }
    };
    SonarLintLogger.get().addLogListener(listener);

    project = importEclipseProject("SimpleJdtProject");
  }

  @AfterClass
  public static void removeLogListener() {
    SonarLintLogger.get().removeLogListener(listener);
  }

  @Before
  public void clean() throws BackingStoreException {
    ConfigurationScope.INSTANCE.getNode(SonarLintCorePlugin.UI_PLUGIN_ID).removeNode();
  }

  @After
  public void cleanup() {
    SonarLintMarkerUpdater.deleteAllMarkersFromReport();
  }

  @Test
  public void analyzeWithRuleParameters() throws Exception {
    // Don't run this test on macOS devices as Node.js might not be found!
    ignoreMacOS();
    
    var file = (IFile) project.findMember("src/main/sample.js");
    var slProject = new DefaultSonarLintProjectAdapter(project);
    var fileToAnalyze = new FileWithDocument(new DefaultSonarLintFileAdapter(slProject, file), null);
    var ruleConfig = new RuleConfig("javascript:S100", true);
    ruleConfig.getParams().put("format", "^[0-9]+$");
    SonarLintGlobalConfiguration.saveRulesConfig(List.of(ruleConfig));

    var underTest = new AnalyzeStandaloneProjectJob(new AnalyzeProjectRequest(slProject, List.of(fileToAnalyze),
      TriggerType.EDITOR_CHANGE, false, true));
    underTest.schedule();
    assertThat(underTest.join(100_000, new NullProgressMonitor())).isTrue();
    var status = underTest.getResult();
    assertThat(status.isOK()).isTrue();

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
      var underTest = new AnalyzeStandaloneProjectJob(
        new AnalyzeProjectRequest(slProject, List.of(file1ToAnalyze, file2ToAnalyze), TriggerType.EDITOR_CHANGE, false, false));
      underTest.schedule();
      assertThat(underTest.join(100_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();

      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_ON_THE_FLY_ID);

      assertThat(mcl.getEventCount()).isEqualTo(1);

      // Run the same analysis a second time to ensure the behavior is the same when markers are already present
      mcl.clearCounter();

      underTest.schedule();
      assertThat(underTest.join(100_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();

      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_ON_THE_FLY_ID);

      assertThat(mcl.getEventCount()).isEqualTo(1);

    } finally {
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
      var underTest = new AnalyzeStandaloneProjectJob(
        new AnalyzeProjectRequest(slProject, List.of(file1ToAnalyze, file2ToAnalyze), TriggerType.MANUAL, true, false));
      underTest.schedule();
      assertThat(underTest.join(100_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();

      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_REPORT_ID);

      // Only one event since there are no markers to clear
      assertThat(mcl.getEventCount()).isEqualTo(1);

      // Run the same analysis a second time to ensure the behavior is the same when markers are already present
      mcl.clearCounter();

      underTest.schedule();
      assertThat(underTest.join(100_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();

      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_REPORT_ID);

      // Two events, one for clearing past markers, one to create new markers
      assertThat(mcl.getEventCount()).isEqualTo(2);

    } finally {
      workspace.removeResourceChangeListener(mcl);
    }

  }

  @Test
  public void analyzeWithQuickFixesWhenFileIsClosed() throws Exception {
    var file = (IFile) project.findMember("src/main/java/com/quickfix/FileWithQuickFixes.java");
    var slProject = new DefaultSonarLintProjectAdapter(project);
    var fileToAnalyze = new FileWithDocument(new DefaultSonarLintFileAdapter(slProject, file), null);

    var underTest = new AnalyzeStandaloneProjectJob(
      new AnalyzeProjectRequest(slProject, List.of(fileToAnalyze), TriggerType.EDITOR_CHANGE, false, false));
    underTest.schedule();
    assertThat(underTest.join(20_000, new NullProgressMonitor())).isTrue();
    var status = underTest.getResult();
    assertThat(status.isOK()).isTrue();

    var markers = List.of(file.findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE, MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR))
      .contains(tuple("/SimpleJdtProject/src/main/java/com/quickfix/FileWithQuickFixes.java", 8,
        "Replace the type specification in this constructor call with the diamond operator (\"<>\").", "java:S2293"));
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
