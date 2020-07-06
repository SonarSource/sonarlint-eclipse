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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.assertj.core.groups.Tuple;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.SonarLintLogger;
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

  @BeforeClass
  public static void prepare() {
    listener = new LogListener() {

      @Override
      public void showNotification(String title, String shortMsg, String longMsg) {

      }

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
  }

  @AfterClass
  public static void cleanup() {
    SonarLintLogger.get().removeLogListener(listener);
  }

  @Test
  public void analyzeWithRuleParameters() throws Exception {
    IProject project = importEclipseProject("SimpleJdtProject");
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

  public static MarkerAttributesExtractor markerAttributes(String... attributes) {
    return new MarkerAttributesExtractor(attributes);
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

    private StringBuffer buffer;

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
