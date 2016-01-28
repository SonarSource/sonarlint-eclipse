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
package org.sonarlint.eclipse.jdt.internal;

import java.io.File;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;

public class JavaProjectConfigurator extends ProjectConfigurator {

  @Override
  public boolean canConfigure(IProject project) {
    return SonarJdtPlugin.hasJavaNature(project);
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    IProject project = request.getProject();
    IJavaProject javaProject = JavaCore.create(project);
    configureJavaProject(javaProject, request.getSonarProjectProperties());
  }

  // Visible for testing
  public void configureJavaProject(IJavaProject javaProject, Map<String, String> sonarProjectProperties) {
    String javaSource = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
    String javaTarget = javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);

    sonarProjectProperties.put("sonar.java.source", javaSource);
    sonarProjectProperties.put("sonar.java.target", javaTarget);

    try {
      JavaProjectConfiguration configuration = new JavaProjectConfiguration();
      configuration.dependentProjects().add(javaProject);
      addClassPathToSonarProject(javaProject, configuration, true);
      configurationToProperties(sonarProjectProperties, configuration);
    } catch (JavaModelException e) {
      SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
    }
  }

  /**
   * Adds the classpath of an eclipse project to the sonarProject recursively, i.e
   * it iterates all dependent projects. Libraries and output folders of dependent projects
   * are added, but no source folders.
   * @param javaProject the eclipse project to get the classpath from
   * @param sonarProjectProperties the sonar project properties to add the classpath to
   * @param context
   * @param topProject indicate we are working on the project to be analysed and not on a dependent project
   * @throws JavaModelException see {@link IJavaProject#getResolvedClasspath(boolean)}
   */
  private void addClassPathToSonarProject(IJavaProject javaProject, JavaProjectConfiguration context, boolean topProject) throws JavaModelException {
    IClasspathEntry[] classPath = javaProject.getResolvedClasspath(true);
    for (IClasspathEntry entry : classPath) {
      switch (entry.getEntryKind()) {
        case IClasspathEntry.CPE_SOURCE:
          if (!isSourceExcluded(entry)) {
            processSourceEntry(entry, javaProject, context, topProject);
          }
          break;
        case IClasspathEntry.CPE_LIBRARY:
          if (topProject || entry.isExported()) {
            final String libPath = resolveLibrary(javaProject, entry);
            if (libPath != null) {
              context.libraries().add(libPath);
            }
          }
          break;
        case IClasspathEntry.CPE_PROJECT:
          IJavaModel javaModel = javaProject.getJavaModel();
          IJavaProject referredProject = javaModel.getJavaProject(entry.getPath().segment(0));
          if (!context.dependentProjects().contains(referredProject)) {
            context.dependentProjects().add(referredProject);
            addClassPathToSonarProject(referredProject, context, false);
          }
          break;
        default:
          SonarLintCorePlugin.getDefault().info("Unhandled ClassPathEntry : " + entry);
          break;
      }
    }

    processOutputDir(javaProject.getOutputLocation(), context, topProject);
  }

  private static void processOutputDir(IPath outputDir, JavaProjectConfiguration context, boolean topProject) throws JavaModelException {
    String outDir = getAbsolutePath(outputDir);
    if (outDir != null) {
      if (topProject) {
        context.binaries().add(outDir);
      } else {
        // Output dir of dependents projects should be considered as libraries
        context.libraries().add(outDir);
      }
    } else {
      SonarLintCorePlugin.getDefault().info("Binary directory was not added because it was not found. Maybe should you enable auto build of your project.");
    }
  }

  private void processSourceEntry(IClasspathEntry entry, IJavaProject javaProject, JavaProjectConfiguration context, boolean topProject) throws JavaModelException {
    String srcDir = getRelativePath(javaProject.getPath(), entry.getPath());
    if (srcDir == null) {
      SonarLintCorePlugin.getDefault().info("Skipping non existing source entry: " + entry.getPath().toOSString());
      return;
    }
    if (entry.getOutputLocation() != null) {
      processOutputDir(entry.getOutputLocation(), context, topProject);
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

  private static void configurationToProperties(Map<String, String> sonarProjectProperties, JavaProjectConfiguration context) {
    setPropertyList(sonarProjectProperties, "sonar.libraries", context.libraries());
    // Eclipse doesn't separate main and test classpath
    setPropertyList(sonarProjectProperties, "sonar.java.libraries", context.libraries());
    setPropertyList(sonarProjectProperties, "sonar.java.test.libraries", context.libraries());
    setPropertyList(sonarProjectProperties, "sonar.binaries", context.binaries());
    // Eclipse doesn't separate main and test classpath
    setPropertyList(sonarProjectProperties, "sonar.java.binaries", context.binaries());
    setPropertyList(sonarProjectProperties, "sonar.java.test.binaries", context.binaries());
  }
}
