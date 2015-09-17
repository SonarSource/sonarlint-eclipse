/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.core.internal.markers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarMarkerTest extends SonarTestCase {

  private static final String key = "org.sonar-ide.tests.SimpleProject:SimpleProject";
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
    HashMap<String, Object> markers = new HashMap<String, Object>();
    SonarMarker.addLine(markers, 2, file);
    assertThat((Integer) markers.get(IMarker.CHAR_START)).isEqualTo(31);
    assertThat((Integer) markers.get(IMarker.CHAR_END)).isEqualTo(63);
  }

  @Test
  public void testLineStartEndCrLf() throws Exception {
    IFile file = project.getFile("src/main/java/ViolationOnFileCrLf.java");
    InputStream is = file.getContents();
    String content;
    try {
      content = IOUtils.toString(is, file.getCharset());
    } finally {
      IOUtils.closeQuietly(is);
    }
    content.replaceAll("\n", "\r\n");
    file.setContents(new ByteArrayInputStream(content.getBytes()), IFile.FORCE, new NullProgressMonitor());
    HashMap<String, Object> markers = new HashMap<String, Object>();
    SonarMarker.addLine(markers, 2, file);
    assertThat((Integer) markers.get(IMarker.CHAR_START)).isEqualTo(32);
    assertThat((Integer) markers.get(IMarker.CHAR_END)).isEqualTo(64);
  }
}
