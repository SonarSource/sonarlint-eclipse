/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JdtUtilsTest extends SonarTestCase {

  private JdtUtils jdtUtils = new JdtUtils();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Test
  public void shouldConfigureJavaSourceAndTarget() throws JavaModelException, IOException {
    IJavaProject project = mock(IJavaProject.class);
    IPreAnalysisContext context = mock(IPreAnalysisContext.class);

    when(project.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project.getResolvedClasspath(true)).thenReturn(new IClasspathEntry[] {});
    when(project.getOutputLocation()).thenReturn(new Path(temp.newFolder("output").getAbsolutePath()));

    jdtUtils.configureJavaProject(project, context);

    verify(context).setAnalysisProperty("sonar.java.source", "1.6");
    verify(context).setAnalysisProperty("sonar.java.target", "1.6");
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
    IPreAnalysisContext context = mock(IPreAnalysisContext.class);

    when(project.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project.getPath()).thenReturn(new Path(projectRoot.getAbsolutePath()));

    IClasspathEntry[] cpes = new IClasspathEntry[] {
      createCPE(IClasspathEntry.CPE_SOURCE, sourceFolder, outputFolder),
      createCPE(IClasspathEntry.CPE_SOURCE, testFolder, outputFolder)
    };

    when(project.getResolvedClasspath(true)).thenReturn(cpes);
    when(project.getOutputLocation()).thenReturn(new Path(outputFolder.getAbsolutePath()));

    jdtUtils.configureJavaProject(project, context);

    ArgumentCaptor<Collection<String>> captorBinaries = ArgumentCaptor.forClass(Collection.class);
    verify(context).setAnalysisProperty(ArgumentMatchers.eq("sonar.java.binaries"), captorBinaries.capture());
    ArgumentCaptor<Collection<String>> captorTestBinaries = ArgumentCaptor.forClass(Collection.class);
    verify(context).setAnalysisProperty(ArgumentMatchers.eq("sonar.java.test.binaries"), captorTestBinaries.capture());

    assertThat(captorBinaries.getValue()).containsExactlyInAnyOrder(outputFolder.getAbsolutePath().replaceAll(Pattern.quote("\\"), "/"));
    assertThat(captorTestBinaries.getValue()).isEmpty();
  }

  @Test
  public void shouldConfigureSimpleProjectWithTests() throws JavaModelException, IOException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    File workspaceRoot = root.getLocation().toFile();
    File projectRoot = new File(workspaceRoot, "myProject");
    projectRoot.mkdir();
    File sourceFolder = new File(projectRoot, "src");
    sourceFolder.mkdir();
    File testFolder = new File(projectRoot, "test");
    testFolder.mkdir();
    File mainOutputFolder = new File(projectRoot, "bin/main");
    mainOutputFolder.mkdirs();
    File testOutputFolder = new File(projectRoot, "bin/test");
    testOutputFolder.mkdirs();

    IJavaProject project = mock(IJavaProject.class);
    IPreAnalysisContext context = mock(IPreAnalysisContext.class);

    when(project.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project.getPath()).thenReturn(new Path(projectRoot.getAbsolutePath()));

    IClasspathEntry[] cpes = new IClasspathEntry[] {
      createCPE(IClasspathEntry.CPE_SOURCE, sourceFolder, mainOutputFolder),
      createTestCPE(IClasspathEntry.CPE_SOURCE, testFolder, testOutputFolder)
    };

    when(project.getResolvedClasspath(true)).thenReturn(cpes);
    when(project.getOutputLocation()).thenReturn(new Path(mainOutputFolder.getAbsolutePath()));

    jdtUtils.configureJavaProject(project, context);

    ArgumentCaptor<Collection<String>> captorBinaries = ArgumentCaptor.forClass(Collection.class);
    verify(context).setAnalysisProperty(ArgumentMatchers.eq("sonar.java.binaries"), captorBinaries.capture());
    ArgumentCaptor<Collection<String>> captorTestBinaries = ArgumentCaptor.forClass(Collection.class);
    verify(context).setAnalysisProperty(ArgumentMatchers.eq("sonar.java.test.binaries"), captorTestBinaries.capture());

    assertThat(captorBinaries.getValue()).containsExactlyInAnyOrder(mainOutputFolder.getAbsolutePath().replaceAll(Pattern.quote("\\"), "/"));
    assertThat(captorTestBinaries.getValue()).containsExactlyInAnyOrder(testOutputFolder.getAbsolutePath().replaceAll(Pattern.quote("\\"), "/"));
  }

  // SLE-159
  @Test
  public void doNotAddNonExistingPaths() throws JavaModelException, IOException {
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    File workspaceRoot = root.getLocation().toFile();
    File projectRoot = new File(workspaceRoot, "myProjectMissingOut");
    projectRoot.mkdir();
    File sourceFolder = new File(projectRoot, "src");
    sourceFolder.mkdir();
    File outputFolder = new File(projectRoot, "bin");

    IJavaProject project = mock(IJavaProject.class);
    IPreAnalysisContext context = mock(IPreAnalysisContext.class);

    when(project.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project.getPath()).thenReturn(new Path(projectRoot.getAbsolutePath()));

    IClasspathEntry[] cpes = new IClasspathEntry[] {
      createCPE(IClasspathEntry.CPE_SOURCE, sourceFolder, outputFolder)
    };

    when(project.getResolvedClasspath(true)).thenReturn(cpes);
    when(project.getOutputLocation()).thenReturn(new Path(outputFolder.getAbsolutePath()));

    jdtUtils.configureJavaProject(project, context);

    ArgumentCaptor<Collection<String>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(context).setAnalysisProperty(ArgumentMatchers.eq("sonar.java.binaries"), captor.capture());

    assertThat(captor.getValue()).isEmpty();
  }

  @Test
  public void shouldConfigureProjectsWithCircularDependencies() throws CoreException, IOException {
    // the bug appeared when at least 3 projects were involved: the first project depends on the second one which has a circular dependency
    // towards the second one
    IPreAnalysisContext context = mock(IPreAnalysisContext.class);
    // mock three projects that depend on each other
    final String project1Name = "project1";
    final String project2Name = "project2";
    final String project3Name = "project3";
    IJavaProject project1 = mock(IJavaProject.class, Mockito.RETURNS_DEEP_STUBS);
    IJavaProject project2 = mock(IJavaProject.class, Mockito.RETURNS_DEEP_STUBS);
    IJavaProject project3 = mock(IJavaProject.class, Mockito.RETURNS_DEEP_STUBS);
    // these are required during the call to configureJavaProject
    when(project1.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project1.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project1.getProject().getName()).thenReturn(project1Name);
    when(project2.getProject().getName()).thenReturn(project2Name);
    when(project3.getProject().getName()).thenReturn(project3Name);

    // create three classpathEntries, one for each Project
    IClasspathEntry entryProject1 = mock(IClasspathEntry.class, Mockito.RETURNS_DEEP_STUBS);
    IClasspathEntry entryProject2 = mock(IClasspathEntry.class, Mockito.RETURNS_DEEP_STUBS);
    IClasspathEntry entryProject3 = mock(IClasspathEntry.class, Mockito.RETURNS_DEEP_STUBS);
    when(entryProject1.getEntryKind()).thenReturn(IClasspathEntry.CPE_PROJECT);
    when(entryProject1.getPath().segment(0)).thenReturn(project1Name);
    when(entryProject1.getExtraAttributes()).thenReturn(new IClasspathAttribute[0]);
    when(entryProject2.getEntryKind()).thenReturn(IClasspathEntry.CPE_PROJECT);
    when(entryProject2.getPath().segment(0)).thenReturn(project2Name);
    when(entryProject2.getExtraAttributes()).thenReturn(new IClasspathAttribute[0]);
    when(entryProject3.getEntryKind()).thenReturn(IClasspathEntry.CPE_PROJECT);
    when(entryProject3.getPath().segment(0)).thenReturn(project3Name);
    when(entryProject3.getExtraAttributes()).thenReturn(new IClasspathAttribute[0]);
    // project1 depends on project2, which depends on project3, which depends on project2
    IClasspathEntry[] classpath1 = new IClasspathEntry[] {entryProject2};
    IClasspathEntry[] classpath2 = new IClasspathEntry[] {entryProject3};
    IClasspathEntry[] classpath3 = new IClasspathEntry[] {entryProject2};
    when(project1.getResolvedClasspath(true)).thenReturn(classpath1);
    when(project2.getResolvedClasspath(true)).thenReturn(classpath2);
    when(project3.getResolvedClasspath(true)).thenReturn(classpath3);

    // mock the JavaModel
    IJavaModel javaModel = mock(IJavaModel.class);
    when(javaModel.getJavaProject(project1Name)).thenReturn(project1);
    when(javaModel.getJavaProject(project2Name)).thenReturn(project2);
    when(javaModel.getJavaProject(project3Name)).thenReturn(project3);

    when(project1.getJavaModel()).thenReturn(javaModel);
    when(project2.getJavaModel()).thenReturn(javaModel);
    when(project3.getJavaModel()).thenReturn(javaModel);

    // this call should not fail (StackOverFlowError before patch)
    jdtUtils.configureJavaProject(project1, context);

  }

  private IClasspathEntry createCPE(int kind, File path, @Nullable File outputLocation) {
    IClasspathEntry cpe = mock(IClasspathEntry.class);
    when(cpe.getEntryKind()).thenReturn(kind);
    when(cpe.getPath()).thenReturn(new Path(path.getAbsolutePath()));
    when(cpe.getOutputLocation()).thenReturn(new Path(outputLocation.getAbsolutePath()));
    when(cpe.getExtraAttributes()).thenReturn(new IClasspathAttribute[0]);
    return cpe;
  }

  private IClasspathEntry createTestCPE(int kind, File path, @Nullable File outputLocation) {
    IClasspathEntry cpe = createCPE(kind, path, outputLocation);
    IClasspathAttribute[] iClasspathAttributes = new IClasspathAttribute[1];
    iClasspathAttributes[0] = new IClasspathAttribute() {

      @Override
      public String getValue() {
        return "true";
      }

      @Override
      public String getName() {
        return "test";
      }
    };
    when(cpe.getExtraAttributes()).thenReturn(iClasspathAttributes);
    return cpe;
  }

  @Test
  public void keepOnlyJavaFilesOnClasspathForJdtProject() throws Exception {
    IProject jdtProject = importEclipseProject("SimpleJdtProject");
    IFile onClassPath = (IFile) jdtProject.findMember("src/main/java/ClassOnDefaultPackage.java");
    IFile compileError = (IFile) jdtProject.findMember("src/main/java/ClassWithCompileError.java");
    IFile outsideClassPath = (IFile) jdtProject.findMember("ClassOutsideSourceFolder.java");
    IFile nonJava = (IFile) jdtProject.findMember("src/main/sample.js");

    assertThat(JdtUtils.shouldExclude(onClassPath)).isFalse();
    assertThat(JdtUtils.shouldExclude(compileError)).isTrue();
    assertThat(JdtUtils.shouldExclude(outsideClassPath)).isTrue();
    assertThat(JdtUtils.shouldExclude(nonJava)).isFalse();
  }

  @Test
  public void ignoreJavaFilesOnNonJdtProject() throws Exception {
    IProject nonJdtProject = importEclipseProject("SimpleNonJdtProject");
    IFile java = (IFile) nonJdtProject.findMember("src/main/ClassOnDefaultPackage.java");
    IFile nonJava = (IFile) nonJdtProject.findMember("src/main/sample.js");
    IFile contentTypeExtendingJava = (IFile) nonJdtProject.findMember("src/main/Program.cbl");

    assertThat(JdtUtils.shouldExclude(java)).isTrue();
    assertThat(JdtUtils.shouldExclude(nonJava)).isFalse();
    assertThat(JdtUtils.shouldExclude(contentTypeExtendingJava)).isFalse();
  }
}
