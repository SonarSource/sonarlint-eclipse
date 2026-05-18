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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SuppressWarningsQuickFixTest {
  private static final SuppressWarningsQuickFixGenerator generator = new SuppressWarningsQuickFixGenerator();

  @After
  public void after() {
    var options = JavaCore.getOptions();
    options.put(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN, JavaCore.WARNING);
    JavaCore.setOptions(options);
  }

  @Test
  public void testIrrelevantMarkers() throws CoreException {
    var marker = mock(IMarker.class);
    when(marker.getType()).thenReturn("noJavaProblem");

    assertThat(generator.hasResolutions(marker)).isFalse();
    assertThat(generator.getResolutions(marker)).isEmpty();

    when(marker.getType()).thenReturn(SuppressWarningsQuickFixGenerator.JAVA_PROBLEM_TYPE);
    when(marker.getAttribute(IMarker.MESSAGE)).thenReturn(null);

    assertThat(generator.hasResolutions(marker)).isFalse();

    when(marker.getAttribute(IMarker.MESSAGE)).thenReturn(SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_GENERAL);

    assertThat(generator.hasResolutions(marker)).isFalse();
  }

  @Test
  public void testRelevantMarkers() throws CoreException {
    var marker = getRelevantMarker();
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
    var marker = getRelevantMarker();

    var quickFix = getQuickFix(marker);
    assertThat(quickFix.getLabel()).contains("SonarQube");
    assertThat(quickFix.getDescription()).contains("SonarQube");

    quickFix.run(marker);
    assertThat(JavaCore.getOption(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN)).isEqualTo(JavaCore.IGNORE);
  }

  /**
   *  This tests the conditions to check whether the project related resource has a project and therefore can delegate
   *  it to the JDT logic to create a Java project. As there is no project, it is not possible.
   */
  @Test
  public void testWithoutProjectMarker() throws CoreException {
    var resourceWithoutProject = mock(IResource.class);
    when(resourceWithoutProject.getProject()).thenReturn(null);

    var markerWithResource = getRelevantMarker();
    when(markerWithResource.getResource()).thenReturn(resourceWithoutProject);

    var quickFix = getQuickFix(markerWithResource);
    quickFix.run(markerWithResource);
    assertThat(JavaCore.getOption(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN)).isEqualTo(JavaCore.IGNORE);
  }

  /**
   *  This tests the marker with a project, but the JavaModel cannot be mocked and therefore the Java project created
   *  by JDT logic is closed. The quickfix therefore falls back to the global options.
   *  
   *  The JavaModel and the related logic can only be tested by an integration test which is quite costly.
   */
  @Test
  public void testClosedJavaProjectMarker() throws CoreException {
    var closedProject = mock(IProject.class);
    when(closedProject.getType()).thenReturn(IResource.PROJECT);

    var resourceWithProject = mock(IResource.class);
    when(resourceWithProject.getProject()).thenReturn(closedProject);

    var markerWithResource = getRelevantMarker();
    when(markerWithResource.getResource()).thenReturn(resourceWithProject);

    var quickFix = getQuickFix(markerWithResource);
    quickFix.run(markerWithResource);

    // This means the option is not applied on the project but global level.
    assertThat(JavaCore.getOption(JavaCore.COMPILER_PB_UNHANDLED_WARNING_TOKEN)).isEqualTo(JavaCore.IGNORE);
  }

  private IMarker getRelevantMarker() throws CoreException {
    var marker = mock(IMarker.class);
    when(marker.getType()).thenReturn(SuppressWarningsQuickFixGenerator.JAVA_PROBLEM_TYPE);
    when(marker.getAttribute(IMarker.MESSAGE)).thenReturn(SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_GENERAL
      + SuppressWarningsQuickFixGenerator.MARKER_MESSAGE_JAVA_ISSUE);
    return marker;
  }

  private SuppressWarningsQuickFix getQuickFix(IMarker marker) {
    var quickFixes = generator.getResolutions(marker);
    assertThat(quickFixes).hasSize(1);
    return (SuppressWarningsQuickFix) quickFixes[0];
  }
}
