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
package org.sonarlint.eclipse.core.internal.markers;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkerUtilsTest extends SonarTestCase {

  private static IProject project;

  @BeforeClass
  public static void importProject() throws Exception {
    project = importEclipseProject("SimpleProject");
    // Configure the project
    SonarLintProject.getInstance(project);
  }

  private static class TextFileContext implements AutoCloseable {
    private final IPath path;
    private final ITextFileBufferManager textFileBufferManager;
    private final IDocument document;

    TextFileContext(String filepath) throws CoreException {
      IFile file = project.getFile(filepath);
      this.path = file.getFullPath();
      this.textFileBufferManager = FileBuffers.getTextFileBufferManager();
      textFileBufferManager.connect(path, LocationKind.IFILE, new NullProgressMonitor());
      ITextFileBuffer textFileBuffer = textFileBufferManager.getTextFileBuffer(path, LocationKind.IFILE);
      document = textFileBuffer.getDocument();
    }

    @Override
    public void close() throws Exception {
      textFileBufferManager.disconnect(path, LocationKind.IFILE, new NullProgressMonitor());
    }
  }

  @Test
  public void testLineStartEnd() throws Exception {
    try (TextFileContext context = new TextFileContext("src/main/java/ViolationOnFile.java")) {
      TextRange textRange = new TextRange(2);
      FlatTextRange flatTextRange = MarkerUtils.toFlatTextRange(context.document, textRange);
      assertThat(flatTextRange.getStart()).isEqualTo(31);
      assertThat(flatTextRange.getEnd()).isEqualTo(63);
    }
  }

  @Test
  public void testLineStartEndCrLf() throws Exception {
    try (TextFileContext context = new TextFileContext("src/main/java/ViolationOnFileCrLf.java")) {
      TextRange textRange = new TextRange(2);
      FlatTextRange flatTextRange = MarkerUtils.toFlatTextRange(context.document, textRange);
      assertThat(flatTextRange.getStart()).isEqualTo(32);
      assertThat(flatTextRange.getEnd()).isEqualTo(64);
    }
  }

  @Test
  public void testPreciseIssueLocationSingleLine() throws Exception {
    try (TextFileContext context = new TextFileContext("src/main/java/ViolationOnFile.java")) {
      TextRange textRange = new TextRange(2, 23, 2, 31);
      FlatTextRange flatTextRange = MarkerUtils.toFlatTextRange(context.document, textRange);
      assertThat(flatTextRange.getStart()).isEqualTo(54);
      assertThat(flatTextRange.getEnd()).isEqualTo(62);
    }
  }

  @Test
  public void testPreciseIssueLocationMultiLine() throws Exception {
    try (TextFileContext context = new TextFileContext("src/main/java/ViolationOnFile.java")) {
      TextRange textRange = new TextRange(4, 34, 5, 12);
      FlatTextRange flatTextRange = MarkerUtils.toFlatTextRange(context.document, textRange);
      assertThat(flatTextRange.getStart()).isEqualTo(101);
      assertThat(flatTextRange.getEnd()).isEqualTo(119);
    }
  }
}
