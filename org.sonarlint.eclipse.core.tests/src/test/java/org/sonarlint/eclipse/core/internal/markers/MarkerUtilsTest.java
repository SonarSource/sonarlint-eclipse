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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
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

  @Test
  public void testLineStartEnd() throws Exception {
    IFile file = project.getFile("src/main/java/ViolationOnFile.java");
    ITextFileBufferManager iTextFileBufferManager = FileBuffers.getTextFileBufferManager();
    iTextFileBufferManager.connect(file.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
    ITextFileBuffer iTextFileBuffer = iTextFileBufferManager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
    IDocument iDoc = iTextFileBuffer.getDocument();

    TextRange textRange = new TextRange(2);
    FlatTextRange flatTextRange = MarkerUtils.toFlatTextRange(iDoc, textRange);
    assertThat(flatTextRange.getStart()).isEqualTo(31);
    assertThat(flatTextRange.getEnd()).isEqualTo(63);

    iTextFileBufferManager.disconnect(file.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
  }

  @Test
  public void testLineStartEndCrLf() throws Exception {
    IFile file = project.getFile("src/main/java/ViolationOnFileCrLf.java");
    String content;
    try (InputStream is = file.getContents()) {
      java.util.Scanner s = new java.util.Scanner(is, file.getCharset()).useDelimiter("\\A");
      content = s.hasNext() ? s.next() : "";
    }
    content.replaceAll("\n", "\r\n");
    file.setContents(new ByteArrayInputStream(content.getBytes()), IFile.FORCE, new NullProgressMonitor());
    ITextFileBufferManager iTextFileBufferManager = FileBuffers.getTextFileBufferManager();
    iTextFileBufferManager.connect(file.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
    ITextFileBuffer iTextFileBuffer = iTextFileBufferManager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
    IDocument iDoc = iTextFileBuffer.getDocument();

    TextRange textRange = new TextRange(2);
    FlatTextRange flatTextRange = MarkerUtils.toFlatTextRange(iDoc, textRange);
    assertThat(flatTextRange.getStart()).isEqualTo(32);
    assertThat(flatTextRange.getEnd()).isEqualTo(64);

    iTextFileBufferManager.disconnect(file.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
  }

  @Test
  public void testPreciseIssueLocationSingleLine() throws Exception {
    IFile file = project.getFile("src/main/java/ViolationOnFile.java");
    ITextFileBufferManager iTextFileBufferManager = FileBuffers.getTextFileBufferManager();
    iTextFileBufferManager.connect(file.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
    ITextFileBuffer iTextFileBuffer = iTextFileBufferManager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
    IDocument iDoc = iTextFileBuffer.getDocument();

    TextRange textRange = new TextRange(2, 23, 2, 31);
    FlatTextRange flatTextRange = MarkerUtils.toFlatTextRange(iDoc, textRange);
    assertThat(flatTextRange.getStart()).isEqualTo(54);
    assertThat(flatTextRange.getEnd()).isEqualTo(62);

    iTextFileBufferManager.disconnect(file.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
  }

  @Test
  public void testPreciseIssueLocationMultiLine() throws Exception {
    IFile file = project.getFile("src/main/java/ViolationOnFile.java");
    ITextFileBufferManager iTextFileBufferManager = FileBuffers.getTextFileBufferManager();
    iTextFileBufferManager.connect(file.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
    ITextFileBuffer iTextFileBuffer = iTextFileBufferManager.getTextFileBuffer(file.getFullPath(), LocationKind.IFILE);
    IDocument iDoc = iTextFileBuffer.getDocument();

    TextRange textRange = new TextRange(4, 34, 5, 12);
    FlatTextRange flatTextRange = MarkerUtils.toFlatTextRange(iDoc, textRange);
    assertThat(flatTextRange.getStart()).isEqualTo(101);
    assertThat(flatTextRange.getEnd()).isEqualTo(119);

    iTextFileBufferManager.disconnect(file.getFullPath(), LocationKind.IFILE, new NullProgressMonitor());
  }
}
