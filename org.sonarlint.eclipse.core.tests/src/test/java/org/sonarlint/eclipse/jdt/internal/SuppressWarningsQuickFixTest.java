/*
 * SonarLint for Eclipse
 * Copyright (C) SonarSource Sàrl
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
package org.sonarlint.eclipse.jdt.internal;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.junit.AfterClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SuppressWarningsQuickFixTest {
  private static final SuppressWarningsQuickFixGenerator generator = new SuppressWarningsQuickFixGenerator();

  @AfterClass
  public static void after() {
    var options = JavaCore.getOptions();
    options.put(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN, JavaCore.WARNING);
    JavaCore.setOptions(options);
  }

  @Test
  public void testIrrelevantMarkers() throws CoreException {
    var marker = mock(IMarker.class);
    when(marker.getType()).thenReturn("noJavaProblem");

    assertThat(generator.hasResolutions(marker)).isFalse();
    assertThat(generator.getResolutions(marker).length).isEqualTo(0);

    when(marker.getType()).thenReturn(SuppressWarningsQuickFixGenerator.JAVA_PROBLEM_TYPE);
    when(marker.getAttribute(IMarker.MESSAGE)).thenReturn(null);

    assertThat(generator.hasResolutions(marker)).isFalse();

    when(marker.getAttribute(IMarker.MESSAGE)).thenReturn(SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_GENERAL);

    assertThat(generator.hasResolutions(marker)).isFalse();
  }

  @Test
  public void testRelevantMarkers() throws CoreException {
    var marker = mock(IMarker.class);
    when(marker.getType()).thenReturn(SuppressWarningsQuickFixGenerator.JAVA_PROBLEM_TYPE);

    when(marker.getAttribute(IMarker.MESSAGE)).thenReturn(SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_GENERAL
      + SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_JAVA_ISSUE);
    assertThat(generator.hasResolutions(marker)).isTrue();

    when(marker.getAttribute(IMarker.MESSAGE)).thenReturn(SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_GENERAL
      + SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_JAVA_SECURITY_ISSUE);
    assertThat(generator.hasResolutions(marker)).isTrue();

    when(marker.getAttribute(IMarker.MESSAGE)).thenReturn(SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_GENERAL
      + SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_JAVA_BUGS_ISSUE);
    assertThat(generator.hasResolutions(marker)).isTrue();

    when(marker.getAttribute(IMarker.MESSAGE)).thenReturn(SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_GENERAL
      + SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_JAVA_ARCHITECTURE_ISSUE);
    assertThat(generator.hasResolutions(marker)).isTrue();
  }

  @Test
  public void testQuickFix() throws CoreException {
    var marker = mock(IMarker.class);
    when(marker.getType()).thenReturn(SuppressWarningsQuickFixGenerator.JAVA_PROBLEM_TYPE);
    when(marker.getAttribute(IMarker.MESSAGE)).thenReturn(SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_GENERAL
      + SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_JAVA_ISSUE);

    var quickFixes = generator.getResolutions(marker);
    assertThat(quickFixes.length).isEqualTo(1);

    var quickFix = (SuppressWarningsQuickFix) quickFixes[0];
    assertThat(quickFix.getLabel()).contains("SonarQube");
    assertThat(quickFix.getDescription()).contains("SonarQube");

    quickFix.run(marker);
    assertThat(JavaCore.getOption(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN)).isEqualTo(JavaCore.IGNORE);
  }
}
