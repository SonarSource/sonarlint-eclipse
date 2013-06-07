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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurator;
import org.sonar.ide.eclipse.core.configurator.SonarConfiguratorProperties;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class JavaProjectConfigurator extends ProjectConfigurator {

  private static final Logger LOG = LoggerFactory.getLogger(JavaProjectConfigurator.class);
  // TODO Allow to configure this pattern in Sonar Eclipse preferences
  private static final String TEST_PATTERN = ".*test.*";

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
  public void configureJavaProject(IJavaProject javaProject, Properties sonarProjectProperties) {
    String javaSource = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
    String javaTarget = javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);

    sonarProjectProperties.setProperty(SonarConfiguratorProperties.PROJECT_LANGUAGE_PROPERTY, "java");
    sonarProjectProperties.setProperty("sonar.java.source", javaSource);
    LOG.info("Source Java version: {}", javaSource);
    sonarProjectProperties.setProperty("sonar.java.target", javaTarget);
    LOG.info("Target Java version: {}", javaTarget);

    try {
      JavaProjectContext context = new JavaProjectContext();
      context.getProjects().add(javaProject);
      addClassPathToSonarProject(javaProject, context, true);
      putContextToProperties(sonarProjectProperties, context);
    } catch (JavaModelException e) {
      LOG.error(e.getMessage(), e);
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
  private void addClassPathToSonarProject(IJavaProject javaProject, JavaProjectContext context, boolean topProject) throws JavaModelException {
    IClasspathEntry[] classPath = javaProject.getResolvedClasspath(true);
    Set<String> sonarLibraries = new HashSet<String>();
    for (IClasspathEntry entry : classPath) {
      switch (entry.getEntryKind()) {
        case IClasspathEntry.CPE_SOURCE:
          if (!isSourceExcluded(entry)) {
            processSourceEntry(entry, javaProject, context, topProject);
          }
          break;
        case IClasspathEntry.CPE_LIBRARY:
          if (topProject || entry.isExported()) {
            final String libDir = resolveLibrary(javaProject, entry);
            if (libDir != null && !sonarLibraries.contains(libDir)) {
              LOG.debug("Library: {}", libDir);
              sonarLibraries.add(libDir);
            }
          }
          break;
        case IClasspathEntry.CPE_PROJECT:
          IJavaModel javaModel = javaProject.getJavaModel();
          IJavaProject referredProject = javaModel.getJavaProject(entry.getPath().segment(0));
          if(!context.getProjects().contains(referredProject)) {
	          LOG.debug("Adding project: {}", referredProject.getProject().getName());
	          addClassPathToSonarProject(referredProject, context, false);
	          context.getProjects().add(referredProject);
          }
          break;
        default:
          LOG.warn("Unhandled ClassPathEntry : {}", entry);
          break;
      }
    }

    processOutputDir(javaProject.getOutputLocation(), context, topProject);
  }

  private void processOutputDir(IPath outputDir, JavaProjectContext context, boolean topProject) throws JavaModelException {
    String outDir = getAbsolutePath(outputDir);
    if (outDir != null) {
      LOG.debug("Output directory: {}", outDir);
      if (topProject) {
        context.getBinaries().add(outDir);
      }
      else {
        // Output dir of dependents projects should be considered as libraries
        context.getLibraries().add(outDir);
      }
    }
    else {
      LOG.warn("Binary directory was not added because it was not found. Maybe should you enable auto build of your project.");
    }
  }

  private void processSourceEntry(IClasspathEntry entry, IJavaProject javaProject, JavaProjectContext context, boolean topProject) throws JavaModelException {
    String srcDir = getAbsolutePath(entry.getPath());
    if (srcDir == null) {
      LOG.warn("Skipping non existing source entry: {}", entry.getPath().toOSString());
      return;
    }
    String relativeDir = getRelativePath(javaProject, entry.getPath());
    if (relativeDir.toLowerCase().matches(TEST_PATTERN)) {
      if (topProject) {
        LOG.debug("Test directory: {}", srcDir);
        context.getTestDirs().add(srcDir);
      }
    }
    else {
      if (topProject) {
        LOG.debug("Source directory: {}", srcDir);
        context.getSourceDirs().add(srcDir);
      }
      if (entry.getOutputLocation() != null) {
        processOutputDir(entry.getOutputLocation(), context, topProject);
      }
    }
  }

  private String resolveLibrary(IJavaProject javaProject, IClasspathEntry entry) {
    final String libDir;
    IResource member = findPath(javaProject.getProject(), entry.getPath());
    if (member != null) {
      LOG.debug("Found member: {}", member.getLocation().toOSString());
      libDir = member.getLocation().toOSString();
    } else {
      libDir = entry.getPath().makeAbsolute().toOSString();
    }
    if (!new File(libDir).exists()) {
      return null;
    }
    return libDir;
  }

  private IResource findPath(IProject project, IPath path) {
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
  private boolean isSourceExcluded(IClasspathEntry entry) {
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

  private String getRelativePath(IJavaProject javaProject, IPath path) {
    return path.makeRelativeTo(javaProject.getPath()).toOSString();
  }

  private void putContextToProperties(Properties sonarProjectProperties, JavaProjectContext context) {
    appendProperty(sonarProjectProperties, SonarConfiguratorProperties.LIBRARIES_PROPERTY, concatenate(context.getLibraries()));
    appendProperty(sonarProjectProperties, SonarConfiguratorProperties.TEST_DIRS_PROPERTY, concatenate(context.getTestDirs()));
    appendProperty(sonarProjectProperties, SonarConfiguratorProperties.SOURCE_DIRS_PROPERTY, concatenate(context.getSourceDirs()));
    appendProperty(sonarProjectProperties, SonarConfiguratorProperties.BINARIES_PROPERTY, concatenate(context.getBinaries()));
  }
}
