/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.ide.eclipse.jdt.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JavaProjectConfiguratorTest {

  private JavaProjectConfigurator configurator = new JavaProjectConfigurator();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldConfigureProjectsWithJavaNature() throws CoreException {
    IProject project = mock(IProject.class);
    when(project.hasNature(JavaCore.NATURE_ID)).thenReturn(true);
    assertThat(configurator.canConfigure(project), is(true));
  }

  @Test
  public void shouldConfigureJavaSourceAndTarget() throws JavaModelException, IOException {
    IJavaProject project = mock(IJavaProject.class);
    Properties sonarProperties = new Properties();

    when(project.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project.getResolvedClasspath(true)).thenReturn(new IClasspathEntry[] {});
    when(project.getOutputLocation()).thenReturn(new Path(temp.newFolder("output").getAbsolutePath()));

    configurator.configureJavaProject(project, sonarProperties);

    assertTrue(sonarProperties.containsKey("sonar.java.source"));
    assertThat(sonarProperties.getProperty("sonar.java.source"), is("1.6"));
    assertTrue(sonarProperties.containsKey("sonar.java.target"));
    assertThat(sonarProperties.getProperty("sonar.java.target"), is("1.6"));
  }

  @Test
  public void shouldConfigureSimpleProject() throws JavaModelException, IOException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    File workspaceRoot = root.getLocation().toFile();
    File projectRoot = new File(workspaceRoot, "myProject");
    projectRoot.mkdir();
    File sourceFolder = new File(projectRoot, "src");
    sourceFolder.mkdir();
    File testFolder = new File(projectRoot, "test");
    testFolder.mkdir();
    File outputFolder = new File(projectRoot, "bin");
    outputFolder.mkdir();

    IJavaProject project = mock(IJavaProject.class);
    Properties sonarProperties = new Properties();

    when(project.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project.getPath()).thenReturn(new Path(projectRoot.getAbsolutePath()));

    IClasspathEntry[] cpes = new IClasspathEntry[] {
      createCPE(IClasspathEntry.CPE_SOURCE, sourceFolder),
      createCPE(IClasspathEntry.CPE_SOURCE, testFolder)
    };

    when(project.getResolvedClasspath(true)).thenReturn(cpes);
    when(project.getOutputLocation()).thenReturn(new Path(outputFolder.getAbsolutePath()));

    configurator.configureJavaProject(project, sonarProperties);

    // TODO Find a way to mock a project inside Eclipse

    // assertTrue(sonarProperties.containsKey("sonar.sources"));
    // assertThat(sonarProperties.getProperty("sonar.sources"), is(sourceFolder.getPath()));
    // assertTrue(sonarProperties.containsKey("sonar.tests"));
    // assertThat(sonarProperties.getProperty("sonar.tests"), is(testFolder.getPath()));
    // assertTrue(sonarProperties.containsKey("sonar.binaries"));
    // assertThat(sonarProperties.getProperty("sonar.binaries"), is(outputFolder.getPath()));
  }

  private IClasspathEntry createCPE(int kind, File path) {
    IClasspathEntry cpe = mock(IClasspathEntry.class);
    when(cpe.getEntryKind()).thenReturn(kind);
    when(cpe.getPath()).thenReturn(new Path(path.getAbsolutePath()));
    return cpe;
  }

}
