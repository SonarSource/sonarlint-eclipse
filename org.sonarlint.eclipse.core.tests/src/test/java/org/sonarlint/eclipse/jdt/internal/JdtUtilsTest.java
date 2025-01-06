/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.sonarlint.eclipse.core.analysis.IFileTypeProvider.ISonarLintFileType;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintFileAdapter;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.tests.common.SonarTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JdtUtilsTest extends SonarTestCase {

  private final JdtUtils jdtUtils = new JdtUtils();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private static IProject jdtProject;
  private static IProject nonJdtProject;

  @BeforeClass
  public static void prepare() throws Exception {
    jdtProject = importEclipseProject("SimpleJdtProject");
    nonJdtProject = importEclipseProject("SimpleNonJdtProject");
  }

  @Test
  public void shouldConfigureJavaSourceAndTarget() throws JavaModelException, IOException {
    var project = mock(IJavaProject.class);
    var context = mock(IPreAnalysisContext.class);

    when(project.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project.getResolvedClasspath(true)).thenReturn(new IClasspathEntry[] {});
    when(project.getOutputLocation()).thenReturn(new Path(temp.newFolder("output").getAbsolutePath()));

    jdtUtils.configureJavaProject(project, context);

    verify(context).setAnalysisProperty("sonar.java.source", "1.6");
    verify(context).setAnalysisProperty("sonar.java.target", "1.6");
  }

  /* SLE-614: Check the new Sonar property: default value (either null or disabled( */
  @Test
  public void test_sonarJavaPreview_default() throws JavaModelException {
    var root = ResourcesPlugin.getWorkspace().getRoot();
    var workspaceRoot = root.getLocation().toFile();
    var projectRoot = new File(workspaceRoot, "SLE_614_project_1");
    projectRoot.mkdir();
    var sourceFolder = new File(projectRoot, "src");
    sourceFolder.mkdir();
    var outputFolder = new File(projectRoot, "bin");
    outputFolder.mkdirs();

    var project = mock(IJavaProject.class);
    var context = mock(IPreAnalysisContext.class);

    when(project.getPath()).thenReturn(new Path(projectRoot.getAbsolutePath()));

    var classpath = new IClasspathEntry[] {
      createCPE(IClasspathEntry.CPE_SOURCE, sourceFolder, outputFolder)
    };
    when(project.getResolvedClasspath(true)).thenReturn(classpath);
    when(project.getOutputLocation()).thenReturn(new Path(outputFolder.getAbsolutePath()));

    jdtUtils.configureJavaProject(project, context);

    verify(context).setAnalysisProperty("sonar.java.enablePreview", "false");
  }

  /* SLE-614: Check the new Sonar property: simulate enabled by the user */
  @Test
  public void test_sonarJavaPreview_changed() throws JavaModelException {
    var root = ResourcesPlugin.getWorkspace().getRoot();
    var workspaceRoot = root.getLocation().toFile();
    var projectRoot = new File(workspaceRoot, "SLE_614_project_2");
    projectRoot.mkdir();
    var sourceFolder = new File(projectRoot, "src");
    sourceFolder.mkdir();
    var outputFolder = new File(projectRoot, "bin");
    outputFolder.mkdirs();

    var project = mock(IJavaProject.class);
    var context = mock(IPreAnalysisContext.class);

    when(project.getOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, true)).thenReturn(JavaCore.ENABLED);
    when(project.getPath()).thenReturn(new Path(projectRoot.getAbsolutePath()));

    var classpath = new IClasspathEntry[] {
      createCPE(IClasspathEntry.CPE_SOURCE, sourceFolder, outputFolder)
    };
    when(project.getResolvedClasspath(true)).thenReturn(classpath);
    when(project.getOutputLocation()).thenReturn(new Path(outputFolder.getAbsolutePath()));

    jdtUtils.configureJavaProject(project, context);

    verify(context).setAnalysisProperty("sonar.java.enablePreview", "true");
  }

  @Test
  public void shouldConfigureSimpleProject() throws JavaModelException, IOException {
    var root = ResourcesPlugin.getWorkspace().getRoot();
    var workspaceRoot = root.getLocation().toFile();
    var projectRoot = new File(workspaceRoot, "myProject");
    projectRoot.mkdir();
    var sourceFolder = new File(projectRoot, "src");
    sourceFolder.mkdir();
    var testFolder = new File(projectRoot, "test");
    testFolder.mkdir();
    var outputFolder = new File(projectRoot, "bin");
    outputFolder.mkdir();

    var project = mock(IJavaProject.class);
    var context = mock(IPreAnalysisContext.class);

    when(project.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project.getPath()).thenReturn(new Path(projectRoot.getAbsolutePath()));

    var cpes = new IClasspathEntry[] {
      createCPE(IClasspathEntry.CPE_SOURCE, sourceFolder, outputFolder),
      createCPE(IClasspathEntry.CPE_SOURCE, testFolder, outputFolder)
    };

    when(project.getResolvedClasspath(true)).thenReturn(cpes);
    when(project.getOutputLocation()).thenReturn(new Path(outputFolder.getAbsolutePath()));

    jdtUtils.configureJavaProject(project, context);

    var captorBinaries = ArgumentCaptor.forClass(Collection.class);
    verify(context).setAnalysisProperty(ArgumentMatchers.eq("sonar.java.binaries"), captorBinaries.capture());
    var captorTestBinaries = ArgumentCaptor.forClass(Collection.class);
    verify(context).setAnalysisProperty(ArgumentMatchers.eq("sonar.java.test.binaries"), captorTestBinaries.capture());

    assertThat(captorBinaries.getValue()).containsExactlyInAnyOrder(outputFolder.getAbsolutePath().replaceAll(Pattern.quote("\\"), "/"));
    assertThat(captorTestBinaries.getValue()).isEmpty();
  }

  @Test
  public void shouldConfigureSimpleProjectWithTests() throws JavaModelException, IOException {
    var root = ResourcesPlugin.getWorkspace().getRoot();
    var workspaceRoot = root.getLocation().toFile();
    var projectRoot = new File(workspaceRoot, "myProject");
    projectRoot.mkdir();
    var sourceFolder = new File(projectRoot, "src");
    sourceFolder.mkdir();
    var testFolder = new File(projectRoot, "test");
    testFolder.mkdir();
    var mainOutputFolder = new File(projectRoot, "bin/main");
    mainOutputFolder.mkdirs();
    var testOutputFolder = new File(projectRoot, "bin/test");
    testOutputFolder.mkdirs();

    var project = mock(IJavaProject.class);
    var context = mock(IPreAnalysisContext.class);

    when(project.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project.getPath()).thenReturn(new Path(projectRoot.getAbsolutePath()));

    var cpes = new IClasspathEntry[] {
      createCPE(IClasspathEntry.CPE_SOURCE, sourceFolder, mainOutputFolder),
      createTestCPE(IClasspathEntry.CPE_SOURCE, testFolder, testOutputFolder)
    };

    when(project.getResolvedClasspath(true)).thenReturn(cpes);
    when(project.getOutputLocation()).thenReturn(new Path(mainOutputFolder.getAbsolutePath()));

    jdtUtils.configureJavaProject(project, context);

    var captorBinaries = ArgumentCaptor.forClass(Collection.class);
    verify(context).setAnalysisProperty(ArgumentMatchers.eq("sonar.java.binaries"), captorBinaries.capture());
    var captorTestBinaries = ArgumentCaptor.forClass(Collection.class);
    verify(context).setAnalysisProperty(ArgumentMatchers.eq("sonar.java.test.binaries"), captorTestBinaries.capture());

    assertThat(captorBinaries.getValue()).containsExactlyInAnyOrder(mainOutputFolder.getAbsolutePath().replaceAll(Pattern.quote("\\"), "/"));
    assertThat(captorTestBinaries.getValue()).containsExactlyInAnyOrder(testOutputFolder.getAbsolutePath().replaceAll(Pattern.quote("\\"), "/"));
  }

  // SLE-159
  @Test
  public void doNotAddNonExistingPaths() throws JavaModelException, IOException {
    var root = ResourcesPlugin.getWorkspace().getRoot();
    var workspaceRoot = root.getLocation().toFile();
    var projectRoot = new File(workspaceRoot, "myProjectMissingOut");
    projectRoot.mkdir();
    var sourceFolder = new File(projectRoot, "src");
    sourceFolder.mkdir();
    var outputFolder = new File(projectRoot, "bin");

    var project = mock(IJavaProject.class);
    var context = mock(IPreAnalysisContext.class);

    when(project.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project.getPath()).thenReturn(new Path(projectRoot.getAbsolutePath()));

    var cpes = new IClasspathEntry[] {
      createCPE(IClasspathEntry.CPE_SOURCE, sourceFolder, outputFolder)
    };

    when(project.getResolvedClasspath(true)).thenReturn(cpes);
    when(project.getOutputLocation()).thenReturn(new Path(outputFolder.getAbsolutePath()));

    jdtUtils.configureJavaProject(project, context);

    var captor = ArgumentCaptor.forClass(Collection.class);
    verify(context).setAnalysisProperty(ArgumentMatchers.eq("sonar.java.binaries"), captor.capture());

    assertThat(captor.getValue()).isEmpty();
  }

  @Test
  public void shouldConfigureProjectsWithCircularDependencies() throws CoreException, IOException {
    // the bug appeared when at least 3 projects were involved: the first project depends on the second one which has a circular dependency
    // towards the second one
    var context = mock(IPreAnalysisContext.class);
    // mock three projects that depend on each other
    final var project1Name = "project1";
    final var project2Name = "project2";
    final var project3Name = "project3";
    var project1 = mock(IJavaProject.class, Mockito.RETURNS_DEEP_STUBS);
    var project2 = mock(IJavaProject.class, Mockito.RETURNS_DEEP_STUBS);
    var project3 = mock(IJavaProject.class, Mockito.RETURNS_DEEP_STUBS);
    // these are required during the call to configureJavaProject
    when(project1.getOption(JavaCore.COMPILER_SOURCE, true)).thenReturn("1.6");
    when(project1.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true)).thenReturn("1.6");
    when(project1.getProject().getName()).thenReturn(project1Name);
    when(project2.getProject().getName()).thenReturn(project2Name);
    when(project3.getProject().getName()).thenReturn(project3Name);

    // create three classpathEntries, one for each Project
    var entryProject1 = mock(IClasspathEntry.class, Mockito.RETURNS_DEEP_STUBS);
    var entryProject2 = mock(IClasspathEntry.class, Mockito.RETURNS_DEEP_STUBS);
    var entryProject3 = mock(IClasspathEntry.class, Mockito.RETURNS_DEEP_STUBS);
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
    var classpath1 = new IClasspathEntry[] {entryProject2};
    var classpath2 = new IClasspathEntry[] {entryProject3};
    var classpath3 = new IClasspathEntry[] {entryProject2};
    when(project1.getResolvedClasspath(true)).thenReturn(classpath1);
    when(project2.getResolvedClasspath(true)).thenReturn(classpath2);
    when(project3.getResolvedClasspath(true)).thenReturn(classpath3);

    // mock the JavaModel
    var javaModel = mock(IJavaModel.class);
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
    var cpe = mock(IClasspathEntry.class);
    when(cpe.getEntryKind()).thenReturn(kind);
    when(cpe.getPath()).thenReturn(new Path(path.getAbsolutePath()));
    when(cpe.getOutputLocation()).thenReturn(new Path(outputLocation.getAbsolutePath()));
    when(cpe.getExtraAttributes()).thenReturn(new IClasspathAttribute[0]);
    return cpe;
  }

  private IClasspathEntry createTestCPE(int kind, File path, @Nullable File outputLocation) {
    var cpe = createCPE(kind, path, outputLocation);
    var iClasspathAttributes = new IClasspathAttribute[1];
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
    var onClassPath = (IFile) jdtProject.findMember("src/main/java/ClassOnDefaultPackage.java");
    var compileError = (IFile) jdtProject.findMember("src/main/java/ClassWithCompileError.java");
    var outsideClassPath = (IFile) jdtProject.findMember("ClassOutsideSourceFolder.java");
    var nonJava = (IFile) jdtProject.findMember("src/main/sample.js");

    assertThat(JdtUtils.shouldExclude(onClassPath)).isFalse();
    assertThat(JdtUtils.shouldExclude(compileError)).isTrue();
    assertThat(JdtUtils.shouldExclude(outsideClassPath)).isTrue();
    assertThat(JdtUtils.shouldExclude(nonJava)).isFalse();
  }

  @Test
  public void qualifyTestFiles() throws Exception {
    var onClassPath = (IFile) jdtProject.findMember("src/main/java/ClassOnDefaultPackage.java");
    var compileError = (IFile) jdtProject.findMember("src/main/java/ClassWithCompileError.java");
    var outsideClassPath = (IFile) jdtProject.findMember("ClassOutsideSourceFolder.java");
    var nonJava = (IFile) jdtProject.findMember("src/main/sample.js");
    var testFile = (IFile) jdtProject.findMember("src/test/java/MyTest.java");

    var slPrj = new DefaultSonarLintProjectAdapter(jdtProject);

    assertThat(JdtUtils.qualify(new DefaultSonarLintFileAdapter(slPrj, onClassPath))).isEqualTo(ISonarLintFileType.MAIN);
    assertThat(JdtUtils.qualify(new DefaultSonarLintFileAdapter(slPrj, compileError))).isEqualTo(ISonarLintFileType.MAIN);
    assertThat(JdtUtils.qualify(new DefaultSonarLintFileAdapter(slPrj, outsideClassPath))).isEqualTo(ISonarLintFileType.UNKNOWN);
    assertThat(JdtUtils.qualify(new DefaultSonarLintFileAdapter(slPrj, nonJava))).isEqualTo(ISonarLintFileType.UNKNOWN);
    assertThat(JdtUtils.qualify(new DefaultSonarLintFileAdapter(slPrj, testFile))).isEqualTo(ISonarLintFileType.TEST);
  }

  @Test
  public void ignoreJavaFilesOnNonJdtProject() throws Exception {
    var java = (IFile) nonJdtProject.findMember("src/main/ClassOnDefaultPackage.java");
    var nonJava = (IFile) nonJdtProject.findMember("src/main/sample.js");
    var contentTypeExtendingJava = (IFile) nonJdtProject.findMember("src/main/Program.cbl");

    assertThat(JdtUtils.shouldExclude(java)).isTrue();
    assertThat(JdtUtils.shouldExclude(nonJava)).isFalse();
    assertThat(JdtUtils.shouldExclude(contentTypeExtendingJava)).isFalse();
  }
}
