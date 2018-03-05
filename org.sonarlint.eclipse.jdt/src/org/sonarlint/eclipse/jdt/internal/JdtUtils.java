/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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
import java.util.Arrays;
import javax.annotation.CheckForNull;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;

public class JdtUtils {

  public void configure(IPreAnalysisContext context, IProgressMonitor monitor) {
    IProject project = (IProject) context.getProject().getResource();
    if (project != null) {
      IJavaProject javaProject = JavaCore.create(project);
      configureJavaProject(javaProject, context);
    }
  }

  static boolean hasJavaNature(IProject project) {
    try {
      return project.hasNature(JavaCore.NATURE_ID);
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
      return false;
    }
  }

  /**
   * SLE-34 Remove Java files that are not compiled.This should automatically exclude files that are excluded / unparseable. 
   */
  public static boolean shouldExclude(IFile file) {
    IJavaElement javaElt = JavaCore.create(file);
    if (javaElt == null) {
      // Not a Java file, don't exclude it
      return false;
    }
    if (!javaElt.exists()) {
      // SLE-218 Visual Cobol with JVM Development make JDT think .cbl files are Java files.
      // But still we want to analyze them, so only exclude files having the original java source content type.
      IContentType javaContentType = Platform.getContentTypeManager().getContentType(JavaCore.JAVA_SOURCE_CONTENT_TYPE);
      String[] fileExtensions = javaContentType.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
      return Arrays.asList(fileExtensions).contains(file.getFileExtension());
    }
    return !javaElt.getJavaProject().isOnClasspath(javaElt) || !isStructureKnown(javaElt);
  }

  private static boolean isStructureKnown(IJavaElement javaElt) {
    try {
      return javaElt.isStructureKnown();
    } catch (JavaModelException e) {
      return false;
    }
  }

  // Visible for testing
  public void configureJavaProject(IJavaProject javaProject, IPreAnalysisContext context) {
    String javaSource = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
    String javaTarget = javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);

    context.setAnalysisProperty("sonar.java.source", javaSource);
    context.setAnalysisProperty("sonar.java.target", javaTarget);

    try {
      JavaProjectConfiguration configuration = new JavaProjectConfiguration();
      configuration.dependentProjects().add(javaProject);
      addClassPathToSonarProject(javaProject, configuration, true);
      configurationToProperties(context, configuration);
    } catch (JavaModelException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  /**
   * Adds the classpath of an eclipse project to the sonarProject recursively, i.e
   * it iterates all dependent projects. Libraries and output folders of dependent projects
   * are added, but no source folders.
   * @param javaProject the eclipse project to get the classpath from
   * @param sonarProjectProperties the sonar project properties to add the classpath to
   * @param context
   * @param topProject indicate we are working on the project to be analyzed and not on a dependent project
   * @throws JavaModelException see {@link IJavaProject#getResolvedClasspath(boolean)}
   */
  private static void addClassPathToSonarProject(IJavaProject javaProject, JavaProjectConfiguration context, boolean topProject) throws JavaModelException {
    IClasspathEntry[] classPath = javaProject.getResolvedClasspath(true);
    for (IClasspathEntry entry : classPath) {
      switch (entry.getEntryKind()) {
        case IClasspathEntry.CPE_SOURCE:
          processSourceEntry(entry, context, topProject);
          break;
        case IClasspathEntry.CPE_LIBRARY:
          processLibraryEntry(entry, javaProject, context, topProject);
          break;
        case IClasspathEntry.CPE_PROJECT:
          processProjectEntry(entry, javaProject, context);
          break;
        default:
          SonarLintLogger.get().info("Unhandled ClassPathEntry : " + entry);
          break;
      }
    }

    processOutputDir(javaProject.getOutputLocation(), context, topProject);
  }

  @CheckForNull
  protected static String getAbsolutePathAsString(IPath path) {
    IPath absolutePath = getAbsolutePath(path);
    return absolutePath != null ? absolutePath.toString() : null;
  }

  @CheckForNull
  private static IPath getAbsolutePath(IPath path) {
    // IPath should be resolved this way in order to handle linked resources (SONARIDE-271)
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IResource res = root.findMember(path);
    if (res != null) {
      if (res.getLocation() != null) {
        return pathIfExist(res.getLocation());
      } else {
        SonarLintLogger.get().error("Unable to resolve absolute path for " + res.getLocationURI());
        return null;
      }
    } else {
      return pathIfExist(path);
    }
  }

  private static IPath pathIfExist(IPath path) {
    File file = path.toFile();
    if (file.exists()) {
      return path;
    }
    return null;
  }

  private static void processOutputDir(IPath outputDir, JavaProjectConfiguration context, boolean topProject) throws JavaModelException {
    String outDir = getAbsolutePathAsString(outputDir);
    if (outDir != null) {
      if (topProject) {
        context.binaries().add(outDir);
      } else {
        // Output dir of dependents projects should be considered as libraries
        context.libraries().add(outDir);
      }
    } else {
      SonarLintLogger.get().info("Binary directory '" + outputDir + "' was not added because it was not found. Maybe you should enable auto build of your project.");
    }
  }

  private static void processSourceEntry(IClasspathEntry entry, JavaProjectConfiguration context, boolean topProject) throws JavaModelException {
    if (isSourceExcluded(entry)) {
      return;
    }
    if (entry.getOutputLocation() != null) {
      processOutputDir(entry.getOutputLocation(), context, topProject);
    }
  }

  private static void processLibraryEntry(IClasspathEntry entry, IJavaProject javaProject, JavaProjectConfiguration context, boolean topProject) throws JavaModelException {
    if (topProject || entry.isExported()) {
      final String libPath = resolveLibrary(javaProject, entry);
      if (libPath != null) {
        context.libraries().add(libPath);
      }
    }
  }

  private static void processProjectEntry(IClasspathEntry entry, IJavaProject javaProject, JavaProjectConfiguration context) throws JavaModelException {
    IJavaModel javaModel = javaProject.getJavaModel();
    IJavaProject referredProject = javaModel.getJavaProject(entry.getPath().segment(0));
    if (!context.dependentProjects().contains(referredProject)) {
      context.dependentProjects().add(referredProject);
      addClassPathToSonarProject(referredProject, context, false);
    }
  }

  private static String resolveLibrary(IJavaProject javaProject, IClasspathEntry entry) {
    final String libPath;
    IResource member = findPath(javaProject.getProject(), entry.getPath());
    if (member != null) {
      libPath = member.getLocation().toOSString();
    } else {
      libPath = entry.getPath().makeAbsolute().toOSString();
    }
    if (!new File(libPath).exists()) {
      return null;
    }
    return libPath.endsWith(File.separator) ? libPath.substring(0, libPath.length() - 1) : libPath;
  }

  private static IResource findPath(IProject project, IPath path) {
    IResource member = project.findMember(path);
    if (member == null) {
      IWorkspaceRoot workSpaceRoot = project.getWorkspace().getRoot();
      member = workSpaceRoot.findMember(path);
    }
    return member;
  }

  /**
   * Allows to determine directories with resources to exclude them from analysis, otherwise analysis might fail due to SONAR-791.
   * This is a kind of workaround, which is based on the fact that M2Eclipse configures exclusion pattern "**" for directories with resources.
   */
  private static boolean isSourceExcluded(IClasspathEntry entry) {
    IPath[] exclusionPatterns = entry.getExclusionPatterns();
    if (exclusionPatterns != null) {
      for (IPath exclusionPattern : exclusionPatterns) {
        if ("**".equals(exclusionPattern.toString())) {
          return true;
        }
      }
    }
    return false;
  }

  private static void configurationToProperties(IPreAnalysisContext analysisContext, JavaProjectConfiguration context) {
    analysisContext.setAnalysisProperty("sonar.libraries", context.libraries());
    // Eclipse doesn't separate main and test classpath
    analysisContext.setAnalysisProperty("sonar.java.libraries", context.libraries());
    analysisContext.setAnalysisProperty("sonar.java.test.libraries", context.libraries());
    analysisContext.setAnalysisProperty("sonar.binaries", context.binaries());
    // Eclipse doesn't separate main and test classpath
    analysisContext.setAnalysisProperty("sonar.java.binaries", context.binaries());
    analysisContext.setAnalysisProperty("sonar.java.test.binaries", context.binaries());
  }
}
