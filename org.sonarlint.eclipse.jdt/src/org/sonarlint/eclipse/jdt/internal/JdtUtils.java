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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IFileTypeProvider.ISonarLintFileType;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.core.internal.utils.BundleUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

public class JdtUtils {

  public void configure(IPreAnalysisContext context, IProgressMonitor monitor) {
    var project = (IProject) context.getProject().getResource();
    if (project != null) {
      var javaProject = JavaCore.create(project);
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
   * SLE-34 Remove Java files that are not compiled. This should automatically exclude files that are excluded / unparsable.
   */
  public static boolean shouldExclude(IFile file) {
    var javaElt = JavaCore.create(file);
    if (javaElt == null) {
      // Not a Java file, don't exclude it
      return false;
    }
    if (!javaElt.exists()) {
      // SLE-218 Visual Cobol with JVM Development make JDT think .cbl files are Java files.
      // But still we want to analyze them, so only exclude files having the original java source content type.
      var javaContentType = Platform.getContentTypeManager().getContentType(JavaCore.JAVA_SOURCE_CONTENT_TYPE);
      var fileExtensions = javaContentType.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
      return List.of(fileExtensions).contains(file.getFileExtension());
    }

    // SLE-854 When a supposed Java file was found, we also have to check if it is a Java project. When not a Java
    // project, we shouldn't exclude files just because there is no JavaProject and no classpath configured correctly.
    // The file might have been flagged as Java because `JavaCore.getJavaLikeExtensions()` tried to categorize the file
    // by accident!
    return hasJavaNature(file.getProject())
      && (!javaElt.getJavaProject().isOnClasspath(javaElt) || !isStructureKnown(javaElt));
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
    var javaSource = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
    var javaTarget = javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);

    context.setAnalysisProperty("sonar.java.source", javaSource);
    context.setAnalysisProperty("sonar.java.target", javaTarget);

    var javaPreview = Optional.ofNullable(javaProject.getOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, true)).orElse(JavaCore.DISABLED);
    context.setAnalysisProperty("sonar.java.enablePreview", javaPreview.equalsIgnoreCase(JavaCore.ENABLED) ? "true" : "false");

    try {
      var configuration = new JavaProjectConfiguration();
      configuration.dependentProjects().add(javaProject);
      addClassPathToSonarProject(javaProject, configuration, true, false, false);
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
  private static void addClassPathToSonarProject(IJavaProject javaProject, JavaProjectConfiguration context, boolean topProject, boolean isTestEntry, boolean isWithoutTestCode)
    throws JavaModelException {
    var classPath = javaProject.getResolvedClasspath(true);
    for (var entry : classPath) {
      switch (entry.getEntryKind()) {
        case IClasspathEntry.CPE_SOURCE:
          processSourceEntry(entry, context, topProject, isTestEntry, isWithoutTestCode);
          break;
        case IClasspathEntry.CPE_LIBRARY:
          processLibraryEntry(entry, javaProject, context, topProject, isTestEntry, isWithoutTestCode);
          break;
        case IClasspathEntry.CPE_PROJECT:
          processProjectEntry(entry, javaProject, context, isTestEntry, isWithoutTestCode);
          break;
        default:
          SonarLintLogger.get().info("Unhandled ClassPathEntry : " + entry);
          break;
      }
    }

    processOutputDir(javaProject.getOutputLocation(), context, topProject, isTestEntry);
  }

  @Nullable
  protected static String getAbsolutePathAsString(IPath path) {
    var absolutePath = getAbsolutePath(path);
    return absolutePath != null ? absolutePath.toString() : null;
  }

  @Nullable
  private static IPath getAbsolutePath(IPath path) {
    // IPath should be resolved this way in order to handle linked resources (SONARIDE-271)
    var root = ResourcesPlugin.getWorkspace().getRoot();
    var res = root.findMember(path);
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

  @Nullable
  private static IPath pathIfExist(IPath path) {
    var file = path.toFile();
    if (file.exists()) {
      return path;
    }
    return null;
  }

  private static void processOutputDir(IPath outputDir, JavaProjectConfiguration context, boolean topProject, boolean testEntry) throws JavaModelException {
    var outDir = getAbsolutePathAsString(outputDir);
    if (outDir != null) {
      if (topProject) {
        if (testEntry) {
          context.testBinaries().add(outDir);
        } else {
          context.binaries().add(outDir);
          // Main source .class should be on tests classpath
          context.testLibraries().add(outDir);
        }
      } else {
        // Output dir of dependents projects should be considered as libraries
        if (testEntry) {
          context.testLibraries().add(outDir);
        } else {
          addMainClasspathEntry(context, outDir);
        }
      }
    } else {
      SonarLintLogger.get().debug("Binary directory '" + outputDir + "' was not added because it was not found. Maybe you should enable auto build of your project.");
    }
  }

  private static void processSourceEntry(IClasspathEntry entry, JavaProjectConfiguration context, boolean topProject, boolean testEntry, boolean isWithoutTestCode)
    throws JavaModelException {
    if (isSourceExcluded(entry) || (isTest(entry) && isWithoutTestCode)) {
      return;
    }
    if (entry.getOutputLocation() != null) {
      processOutputDir(entry.getOutputLocation(), context, topProject, testEntry || isTest(entry));
    }
  }

  private static void processLibraryEntry(IClasspathEntry entry, IJavaProject javaProject, JavaProjectConfiguration context, boolean topProject, boolean testEntry,
    boolean isWithoutTestCode)
    throws JavaModelException {
    if ((isTest(entry) && isWithoutTestCode) || (!topProject && !entry.isExported())) {
      return;
    }
    final var libPath = resolveLibrary(javaProject, entry);
    if (libPath != null) {
      if (testEntry || isTest(entry)) {
        context.testLibraries().add(libPath);
      } else {
        addMainClasspathEntry(context, libPath);
      }
    }
  }

  private static void addMainClasspathEntry(JavaProjectConfiguration context, final String libPath) {
    context.libraries().add(libPath);
    // Main classpath entries should be also added to the tests classpath
    context.testLibraries().add(libPath);
  }

  private static void processProjectEntry(IClasspathEntry entry, IJavaProject javaProject, JavaProjectConfiguration context, boolean testEntry, boolean isWithoutTestCode)
    throws JavaModelException {
    if (isTest(entry) && isWithoutTestCode) {
      return;
    }
    var javaModel = javaProject.getJavaModel();
    var referredProject = javaModel.getJavaProject(entry.getPath().segment(0));
    var dependentProjects = (testEntry || isTest(entry)) ? context.testDependentProjects() : context.dependentProjects();
    if (!dependentProjects.contains(referredProject)) {
      dependentProjects.add(referredProject);
      addClassPathToSonarProject(referredProject, context, false, testEntry || isTest(entry), isWithoutTestCode || isWithoutTestCode(entry));
    }
  }

  @Nullable
  private static String resolveLibrary(IJavaProject javaProject, IClasspathEntry entry) {
    final String libPath;
    var member = findPath(javaProject.getProject(), entry.getPath());
    if (member != null) {
      var location = member.getLocation();
      if (location == null) {
        SonarLintLogger.get().error("Library at '" + entry.getPath() + "' could not be resolved correctly on project '"
          + javaProject.getPath() + "' from workspace member: " + member);
        return null;
      }

      libPath = location.toOSString();
    } else {
      libPath = entry.getPath().makeAbsolute().toOSString();
    }
    if (!new File(libPath).exists()) {
      return null;
    }
    return libPath.endsWith(File.separator) ? libPath.substring(0, libPath.length() - 1) : libPath;
  }

  @Nullable
  private static IResource findPath(IProject project, IPath path) {
    var member = project.findMember(path);
    if (member == null) {
      var workSpaceRoot = project.getWorkspace().getRoot();
      member = workSpaceRoot.findMember(path);
    }
    return member;
  }

  /**
   * Allows to determine directories with resources to exclude them from analysis, otherwise analysis might fail due to SONAR-791.
   * This is a kind of workaround, which is based on the fact that M2Eclipse configures exclusion pattern "**" for directories with resources.
   */
  private static boolean isSourceExcluded(IClasspathEntry entry) {
    var exclusionPatterns = entry.getExclusionPatterns();
    if (exclusionPatterns != null) {
      for (var exclusionPattern : exclusionPatterns) {
        if ("**".equals(exclusionPattern.toString())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isTest(IClasspathEntry entry) {
    for (var attribute : entry.getExtraAttributes()) {
      if (IClasspathAttribute.TEST.equals(attribute.getName()) && "true".equals(attribute.getValue())) { //$NON-NLS-1$
        return true;
      }
    }
    return false;
  }

  private static boolean isWithoutTestCode(IClasspathEntry entry) {
    for (var attribute : entry.getExtraAttributes()) {
      if (IClasspathAttribute.WITHOUT_TEST_CODE.equals(attribute.getName()) && "true".equals(attribute.getValue())) { //$NON-NLS-1$
        return true;
      }
    }
    return false;
  }

  private static void configurationToProperties(IPreAnalysisContext analysisContext, JavaProjectConfiguration context) {
    analysisContext.setAnalysisProperty("sonar.java.libraries", context.libraries());
    analysisContext.setAnalysisProperty("sonar.java.test.libraries", context.testLibraries());
    analysisContext.setAnalysisProperty("sonar.java.binaries", context.binaries());
    analysisContext.setAnalysisProperty("sonar.java.test.binaries", context.testBinaries());
  }

  public static ISonarLintFileType qualify(ISonarLintFile slFile) {
    var file = slFile.getResource().getAdapter(IFile.class);
    if (file == null) {
      return ISonarLintFileType.UNKNOWN;
    }
    var javaElement = JavaCore.create(file);
    if (javaElement == null || !javaElement.exists()) {
      // Not a Java element, don't qualify the file
      return ISonarLintFileType.UNKNOWN;
    }
    var packageFragmentRoot = (IPackageFragmentRoot) javaElement.getAncestor(IJavaElement.PACKAGE_FRAGMENT_ROOT);
    if (packageFragmentRoot == null) {
      return ISonarLintFileType.UNKNOWN;
    }

    IClasspathEntry classpathEntry;
    try {
      classpathEntry = packageFragmentRoot.getResolvedClasspathEntry();
    } catch (JavaModelException e) {
      return ISonarLintFileType.UNKNOWN;
    }
    if (isTest(classpathEntry)) {
      return ISonarLintFileType.TEST;
    }
    // Support of test classpath was added in JDT 3.14, before that we can't guess
    if (BundleUtils.isBundleInstalledWithMinVersion(JavaCore.PLUGIN_ID, 3, 14)) {
      return ISonarLintFileType.MAIN;
    }
    return ISonarLintFileType.UNKNOWN;
  }

  public static Set<IPath> getExcludedPaths(IProject project) {
    var exclusions = new HashSet<IPath>();

    var javaProject = JavaCore.create(project);
    if (javaProject == null) {
      return exclusions;
    }

    try {
      // Get the main output location of the project, can differ from the source entries in the classpath tho!
      var outputLocation = javaProject.getOutputLocation();
      if (outputLocation != null) {
        exclusions.add(outputLocation);
      }

      for (var entry : javaProject.getResolvedClasspath(true)) {
        // We don't check for the libraries here as it will always contain ALL of them including JDK, etc.
        if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
          var output = entry.getOutputLocation();
          if (output != null && (outputLocation == null || !SonarLintUtils.isChild(output, outputLocation))) {
            exclusions.add(output);
          }
        }
      }
    } catch (JavaModelException err) {
      SonarLintLogger.get().traceIdeMessage("Cannot get outputs for exclusions of project '"
        + project.getName() + "' based on JDT!", err);
    }

    SonarLintLogger.get().traceIdeMessage("[JdtUtils#getExcludedPaths] The following paths have been excluded from "
      + "indexing for the project at '" + project.getFullPath().makeAbsolute().toOSString() + "': "
      + String.join(", ", exclusions.stream().map(Object::toString).collect(Collectors.toList())));

    return exclusions;
  }
}
