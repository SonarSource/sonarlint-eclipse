/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
import java.util.Arrays;
import java.util.Collections;
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.LogListener;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.preferences.RuleConfig;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintFileAdapter;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static java.util.Arrays.asList;
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

  @After
  public void cleanup() {
    SonarLintGlobalConfiguration.saveRulesConfig(Collections.emptyList());
    SonarLintMarkerUpdater.deleteAllMarkersFromReport();
  }

  @Test
  public void analyzeWithRuleParameters() throws Exception {
    IFile file = (IFile) project.findMember("src/main/sample.js");
    DefaultSonarLintProjectAdapter slProject = new DefaultSonarLintProjectAdapter(project);
    IDocument document = new SimpleDocument("function hello() {\n" +
      "  \n" +
      "}");
    FileWithDocument fileToAnalyze = new FileWithDocument(new DefaultSonarLintFileAdapter(slProject, file), document);
    RuleConfig ruleConfig = new RuleConfig("javascript:S100", true);
    ruleConfig.getParams().put("format", "^[0-9]+$");
    SonarLintGlobalConfiguration.saveRulesConfig(asList(ruleConfig));

    AnalyzeStandaloneProjectJob underTest = new AnalyzeStandaloneProjectJob(new AnalyzeProjectRequest(slProject, asList(fileToAnalyze), TriggerType.EDITOR_CHANGE));
    underTest.schedule();
    assertThat(underTest.join(10_000, new NullProgressMonitor())).isTrue();
    IStatus status = underTest.getResult();
    assertThat(status.isOK()).isTrue();

    List<IMarker> markers = Arrays.asList(file.findMarkers(SonarLintCorePlugin.MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE))
      .containsOnly(tuple("/SimpleJdtProject/src/main/sample.js", 1, "Rename this 'hello' function to match the regular expression '^[0-9]+$'."));
  }

  @Test
  public void analyze_should_only_triggers_a_single_event_for_all_marker_operations() throws Exception {
    DefaultSonarLintProjectAdapter slProject = new DefaultSonarLintProjectAdapter(project);
    FileWithDocument file1ToAnalyze = prepareFile1(project, slProject);
    FileWithDocument file2ToAnalyze = prepareFile2(project, slProject);

    MarkerChangeListener mcl = new MarkerChangeListener();
    workspace.addResourceChangeListener(mcl);

    try {
      AnalyzeStandaloneProjectJob underTest = new AnalyzeStandaloneProjectJob(
        new AnalyzeProjectRequest(slProject, asList(file1ToAnalyze, file2ToAnalyze), TriggerType.EDITOR_CHANGE));
      underTest.schedule();
      assertThat(underTest.join(10_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();

      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_ON_THE_FLY_ID);

      assertThat(mcl.getEventCount()).isEqualTo(1);

      // Run the same analysis a second time to ensure the behavior is the same when markers are already present
      mcl.clearCounter();

      underTest.schedule();
      assertThat(underTest.join(10_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();

      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_ON_THE_FLY_ID);

      assertThat(mcl.getEventCount()).isEqualTo(1);

    } finally {
      workspace.removeResourceChangeListener(mcl);
    }

  }

  @Test
  public void analyze_report_should_only_triggers_two_events_for_all_marker_operations_when_clear_report() throws Exception {
    DefaultSonarLintProjectAdapter slProject = new DefaultSonarLintProjectAdapter(project);
    FileWithDocument file1ToAnalyze = prepareFile1(project, slProject);
    FileWithDocument file2ToAnalyze = prepareFile2(project, slProject);

    MarkerChangeListener mcl = new MarkerChangeListener();
    workspace.addResourceChangeListener(mcl);

    try {
      AnalyzeStandaloneProjectJob underTest = new AnalyzeStandaloneProjectJob(
        new AnalyzeProjectRequest(slProject, asList(file1ToAnalyze, file2ToAnalyze), TriggerType.MANUAL, true));
      underTest.schedule();
      assertThat(underTest.join(10_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();

      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_REPORT_ID);

      // Only one event since there are no markers to clear
      assertThat(mcl.getEventCount()).isEqualTo(1);

      // Run the same analysis a second time to ensure the behavior is the same when markers are already present
      mcl.clearCounter();

      underTest.schedule();
      assertThat(underTest.join(10_000, new NullProgressMonitor())).isTrue();
      assertThat(underTest.getResult().isOK()).isTrue();

      verifyMarkers(file1ToAnalyze, file2ToAnalyze, SonarLintCorePlugin.MARKER_REPORT_ID);

      // Two events, one for clearing past markers, one to create new markers
      assertThat(mcl.getEventCount()).isEqualTo(2);

    } finally {
      workspace.removeResourceChangeListener(mcl);
    }

  }

  private void verifyMarkers(FileWithDocument file1ToAnalyze, FileWithDocument file2ToAnalyze, String markerType) throws CoreException {
    List<IMarker> markers1 = Arrays.asList(file1ToAnalyze.getFile().getResource().findMarkers(markerType, true, IResource.DEPTH_ONE));
    assertThat(markers1).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).hasSize(6);
    List<IMarker> markers2 = Arrays.asList(file2ToAnalyze.getFile().getResource().findMarkers(markerType, true, IResource.DEPTH_ONE));
    assertThat(markers2).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).hasSize(6);
  }

  private FileWithDocument prepareFile2(IProject project, DefaultSonarLintProjectAdapter slProject) {
    IFile file2 = (IFile) project.findMember("src/main/java/com/sonarsource/NpeWithFlow2.java");
    IDocument document2 = new SimpleDocument("package com.sonarsource;\n" +
      "\n" +
      "public class NpeWithFlow2 {\n" +
      "\n" +
      "  public int foo(boolean a, String foo) {\n" +
      "    if (a) {\n" +
      "      foo = null;\n" +
      "    } else {\n" +
      "      foo = null;\n" +
      "    }\n" +
      "    foo.toString();\n" +
      "    return 0;\n" +
      "  }\n" +
      "}");
    return new FileWithDocument(new DefaultSonarLintFileAdapter(slProject, file2), document2);
  }

  private FileWithDocument prepareFile1(IProject project, DefaultSonarLintProjectAdapter slProject) {
    IFile file1 = (IFile) project.findMember("src/main/java/com/sonarsource/NpeWithFlow.java");
    IDocument document = new SimpleDocument("package com.sonarsource;\n" +
      "\n" +
      "public class NpeWithFlow {\n" +
      "\n" +
      "  public int foo(boolean a, String foo) {\n" +
      "    if (a) {\n" +
      "      foo = null;\n" +
      "    } else {\n" +
      "      foo = null;\n" +
      "    }\n" +
      "    foo.toString();\n" +
      "    return 0;\n" +
      "  }\n" +
      "}");
    return new FileWithDocument(new DefaultSonarLintFileAdapter(slProject, file1), document);
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
      int flags = delta.getFlags();
      if ((flags & IResourceDelta.MARKERS) > 0) {
        System.out.println("Changed markers: " + delta.getMarkerDeltas().length);
        return true;
      }
      for (IResourceDelta child : delta.getAffectedChildren()) {
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
      Object[] tupleAttributes = new Object[attributes.length + 1];
      tupleAttributes[0] = marker.getResource().getFullPath().toPortableString();
      for (int i = 0; i < attributes.length; i++) {
        try {
          tupleAttributes[i + 1] = marker.getAttribute(attributes[i]);
        } catch (CoreException e) {
          throw new IllegalStateException("Unable to get attribute '" + attributes[i] + "'");
        }
      }
      return new Tuple(tupleAttributes);
    }
  }

  /**
   * Minimal implementation of IDocument
   */
  public class SimpleDocument implements IDocument {

    private final StringBuffer buffer;

    public SimpleDocument(String source) {
      this.buffer = new StringBuffer(source);
    }

    @Override
    public char getChar(int offset) {
      return this.buffer.charAt(offset);
    }

    @Override
    public int getLength() {
      return this.buffer.length();
    }

    @Override
    public String get() {
      return this.buffer.toString();
    }

    @Override
    public String get(int offset, int length) {
      return this.buffer.substring(offset, offset + length);
    }

    @Override
    public void set(String text) {
      // defining interface method
    }

    @Override
    public void replace(int offset, int length, String text) {

      this.buffer.replace(offset, offset + length, text);
    }

    @Override
    public void addDocumentListener(IDocumentListener listener) {
      // defining interface method
    }

    @Override
    public void removeDocumentListener(IDocumentListener listener) {
      // defining interface method
    }

    @Override
    public void addPrenotifiedDocumentListener(IDocumentListener documentAdapter) {
      // defining interface method
    }

    @Override
    public void removePrenotifiedDocumentListener(IDocumentListener documentAdapter) {
      // defining interface method
    }

    @Override
    public void addPositionCategory(String category) {
      // defining interface method
    }

    @Override
    public void removePositionCategory(String category) {
      // defining interface method
    }

    @Override
    public String[] getPositionCategories() {
      // defining interface method
      return null;
    }

    @Override
    public boolean containsPositionCategory(String category) {
      // defining interface method
      return false;
    }

    @Override
    public void addPosition(Position position) {
      // defining interface method
    }

    @Override
    public void removePosition(Position position) {
      // defining interface method
    }

    @Override
    public void addPosition(String category, Position position) {
      // defining interface method
    }

    @Override
    public void removePosition(String category, Position position) {
      // defining interface method
    }

    @Override
    public Position[] getPositions(String category) {
      // defining interface method
      return null;
    }

    @Override
    public boolean containsPosition(String category, int offset, int length) {
      // defining interface method
      return false;
    }

    @Override
    public int computeIndexInCategory(String category, int offset) {
      // defining interface method
      return 0;
    }

    @Override
    public void addPositionUpdater(IPositionUpdater updater) {
      // defining interface method
    }

    @Override
    public void removePositionUpdater(IPositionUpdater updater) {
      // defining interface method
    }

    @Override
    public void insertPositionUpdater(IPositionUpdater updater, int index) {
      // defining interface method
    }

    @Override
    public IPositionUpdater[] getPositionUpdaters() {
      // defining interface method
      return null;
    }

    @Override
    public String[] getLegalContentTypes() {
      // defining interface method
      return null;
    }

    @Override
    public String getContentType(int offset) {
      // defining interface method
      return null;
    }

    @Override
    public ITypedRegion getPartition(int offset) {
      // defining interface method
      return null;
    }

    @Override
    public ITypedRegion[] computePartitioning(int offset, int length) {
      // defining interface method
      return null;
    }

    @Override
    public void addDocumentPartitioningListener(IDocumentPartitioningListener listener) {
      // defining interface method
    }

    @Override
    public void removeDocumentPartitioningListener(IDocumentPartitioningListener listener) {
      // defining interface method
    }

    @Override
    public void setDocumentPartitioner(IDocumentPartitioner partitioner) {
      // defining interface method
    }

    @Override
    public IDocumentPartitioner getDocumentPartitioner() {
      // defining interface method
      return null;
    }

    @Override
    public int getLineLength(int line) {
      // defining interface method
      return 0;
    }

    @Override
    public int getLineOfOffset(int offset) {
      // defining interface method
      return 0;
    }

    @Override
    public int getLineOffset(int line) {
      // defining interface method
      return 0;
    }

    @Override
    public IRegion getLineInformation(int line) {
      // defining interface method
      return null;
    }

    @Override
    public IRegion getLineInformationOfOffset(int offset) {
      // defining interface method
      return null;
    }

    @Override
    public int getNumberOfLines() {
      // defining interface method
      return 0;
    }

    @Override
    public int getNumberOfLines(int offset, int length) {
      // defining interface method
      return 0;
    }

    @Override
    public int computeNumberOfLines(String text) {
      // defining interface method
      return 0;
    }

    @Override
    public String[] getLegalLineDelimiters() {
      // defining interface method
      return null;
    }

    @Override
    public String getLineDelimiter(int line) {
      // defining interface method
      return null;
    }

    /**
     * @see org.eclipse.jface.text.IDocument#search(int, java.lang.String, boolean, boolean, boolean)
     * @deprecated
     */
    @Deprecated
    @Override
    public int search(
      int startOffset,
      String findString,
      boolean forwardSearch,
      boolean caseSensitive,
      boolean wholeWord) {
      // defining interface method
      return 0;
    }

  }

}
