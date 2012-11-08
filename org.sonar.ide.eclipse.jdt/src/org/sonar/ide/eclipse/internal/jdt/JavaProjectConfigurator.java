/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.ide.eclipse.internal.jdt;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
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
import org.sonar.ide.eclipse.runner.SonarProperties;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;

import java.util.Properties;

public class JavaProjectConfigurator extends ProjectConfigurator {

  private static final Logger LOG = LoggerFactory.getLogger(JavaProjectConfigurator.class);
  private static final String TEST_PATTERN = ".*test.*"; // TODO Allow to configure this

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    IProject project = request.getProject();
    if (SonarUiPlugin.hasJavaNature(project)) {
      IJavaProject javaProject = JavaCore.create(project);
      configureJavaProject(javaProject, request.getSonarProjectProperties());
    }
  }

  private void appendProperty(Properties properties, String key, String value) {
    String newValue = properties.getProperty(key, "") + SonarProperties.SEPARATOR + value;
    properties.put(key, newValue);
  }

  private void configureJavaProject(IJavaProject javaProject, Properties sonarProjectProperties) {
    String javaSource = javaProject.getOption(JavaCore.COMPILER_SOURCE, true);
    String javaTarget = javaProject.getOption(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, true);

    sonarProjectProperties.setProperty(SonarProperties.PROJECT_LANGUAGE_PROPERTY, "java");
    sonarProjectProperties.setProperty("sonar.java.source", javaSource);
    LOG.info("Source Java version: {}", javaSource);
    sonarProjectProperties.setProperty("sonar.java.target", javaTarget);
    LOG.info("Target Java version: {}", javaTarget);

    try {
      addClassPathToSonarProject(javaProject, sonarProjectProperties, true);
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
   * @param topProject indicate we are working on the project to be analysed and not on a dependent project
   * @throws JavaModelException see {@link IJavaProject#getResolvedClasspath(boolean)}
   */
  private void addClassPathToSonarProject(IJavaProject javaProject, Properties sonarProjectProperties, boolean topProject)
      throws JavaModelException {
    IClasspathEntry[] classPath = javaProject.getResolvedClasspath(true);
    for (IClasspathEntry entry : classPath) {
      switch (entry.getEntryKind()) {
        case IClasspathEntry.CPE_SOURCE:
          if (isSourceExcluded(entry)) {
            break;
          }
          String srcDir = getAbsolutePath(javaProject, entry.getPath());
          String relativeDir = getRelativePath(javaProject, entry.getPath());
          if (relativeDir.toLowerCase().matches(TEST_PATTERN)) {
            if (topProject) {
              LOG.debug("Test directory: {}", srcDir);
              appendProperty(sonarProjectProperties, SonarProperties.TEST_DIRS_PROPERTY, srcDir);
            }
          }
          else {
            if (topProject) {
              LOG.debug("Source directory: {}", srcDir);
              appendProperty(sonarProjectProperties, SonarProperties.SOURCE_DIRS_PROPERTY, srcDir);
            }
            if (entry.getOutputLocation() != null) {
              String binDir = getAbsolutePath(javaProject, entry.getOutputLocation());
              LOG.debug("Binary directory: {}", binDir);
              appendProperty(sonarProjectProperties, SonarProperties.BINARIES_PROPERTY, binDir);
            }
          }
          break;

        case IClasspathEntry.CPE_LIBRARY:
          if (topProject || entry.isExported()) {
            final String libDir = resolveLibrary(javaProject, entry);
            LOG.debug("Library: {}", libDir);
            appendProperty(sonarProjectProperties, SonarProperties.LIBRARIES_PROPERTY, libDir);
          }
          break;
        case IClasspathEntry.CPE_PROJECT:
          IJavaModel javaModel = javaProject.getJavaModel();
          IJavaProject referredProject = javaModel.getJavaProject(entry.getPath().segment(0));
          LOG.debug("Adding project: {}", referredProject.getProject().getName());
          addClassPathToSonarProject(referredProject, sonarProjectProperties, false);
          break;
        default:
          LOG.warn("Unhandled ClassPathEntry : {}", entry);
          break;
      }
    }

    String binDir = getAbsolutePath(javaProject, javaProject.getOutputLocation());
    LOG.debug("Default binary directory: {}", binDir);
    appendProperty(sonarProjectProperties, SonarProperties.BINARIES_PROPERTY, binDir);
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

  private String getAbsolutePath(IJavaProject javaProject, IPath path) {
    // IPath should be resolved this way in order to handle linked resources (SONARIDE-271)
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IResource res = root.findMember(path);
    return res.getLocation().toString();
  }

  private String getRelativePath(IJavaProject javaProject, IPath path) {
    return path.makeRelativeTo(javaProject.getPath()).toOSString();
  }
}
