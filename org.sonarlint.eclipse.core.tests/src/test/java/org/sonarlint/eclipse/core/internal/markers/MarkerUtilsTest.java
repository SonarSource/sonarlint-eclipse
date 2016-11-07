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

import org.eclipse.core.resources.IProject;
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
    try (TextFileContext context = new TextFileContext(project.getFile("src/main/java/ViolationOnFile.java"))) {
      TextRange textRange = new TextRange(2);
      FlatTextRange flatTextRange = MarkerUtils.getFlatTextRange(context.getDocument(), textRange);
      assertThat(flatTextRange.getStart()).isEqualTo(31);
      assertThat(flatTextRange.getEnd()).isEqualTo(63);
      assertThat(context.getDocument().get(flatTextRange.getStart(), flatTextRange.getLength())).isEqualTo("  public static String INSTANCE;");
    }
  }

  @Test
  public void testLineStartEndCrLf() throws Exception {
    try (TextFileContext context = new TextFileContext(project.getFile("src/main/java/ViolationOnFileCrLf.java"))) {
      TextRange textRange = new TextRange(2);
      FlatTextRange flatTextRange = MarkerUtils.getFlatTextRange(context.getDocument(), textRange);
      assertThat(flatTextRange.getStart()).isEqualTo(32);
      assertThat(flatTextRange.getEnd()).isEqualTo(64);
      assertThat(context.getDocument().get(flatTextRange.getStart(), flatTextRange.getLength())).isEqualTo("  public static String INSTANCE;");
    }
  }

  @Test
  public void testPreciseIssueLocationSingleLine() throws Exception {
    try (TextFileContext context = new TextFileContext(project.getFile("src/main/java/ViolationOnFile.java"))) {
      TextRange textRange = new TextRange(2, 23, 2, 31);
      FlatTextRange flatTextRange = MarkerUtils.getFlatTextRange(context.getDocument(), textRange);
      assertThat(flatTextRange.getStart()).isEqualTo(54);
      assertThat(flatTextRange.getEnd()).isEqualTo(62);
      assertThat(context.getDocument().get(flatTextRange.getStart(), flatTextRange.getLength())).isEqualTo("INSTANCE");
    }
  }

  @Test
  public void testPreciseIssueLocationMultiLine() throws Exception {
    try (TextFileContext context = new TextFileContext(project.getFile("src/main/java/ViolationOnFile.java"))) {
      TextRange textRange = new TextRange(4, 34, 5, 12);
      FlatTextRange flatTextRange = MarkerUtils.getFlatTextRange(context.getDocument(), textRange);
      assertThat(flatTextRange.getStart()).isEqualTo(101);
      assertThat(flatTextRange.getEnd()).isEqualTo(119);
      assertThat(context.getDocument().get(flatTextRange.getStart(), flatTextRange.getLength())).isEqualTo("\"foo\"\n     + \"bar\"");
    }
  }

  @Test
  public void testNonexistentLine() throws Exception {
    try (TextFileContext context = new TextFileContext(project.getFile("src/main/java/ViolationOnFile.java"))) {
      int nonexistentLine = context.getDocument().getNumberOfLines() + 1;
      FlatTextRange flatTextRange = MarkerUtils.getFlatTextRange(context.getDocument(), nonexistentLine);
      assertThat(flatTextRange).isNull();
    }
  }

  @Test
  public void testNonexistentTextRange() throws Exception {
    try (TextFileContext context = new TextFileContext(project.getFile("src/main/java/ViolationOnFile.java"))) {
      int nonexistentLine = context.getDocument().getNumberOfLines() + 1;
      TextRange textRange = new TextRange(nonexistentLine, 5, nonexistentLine, 12);
      FlatTextRange flatTextRange = MarkerUtils.getFlatTextRange(context.getDocument(), textRange);
      assertThat(flatTextRange).isNull();
    }
  }

  @Test
  public void testTextRangeWithoutLine() throws Exception {
    try (TextFileContext context = new TextFileContext(project.getFile("src/main/java/ViolationOnFile.java"))) {
      FlatTextRange flatTextRange = MarkerUtils.getFlatTextRange(context.getDocument(), new TextRange(null));
      assertThat(flatTextRange).isNull();
    }
  }
}
