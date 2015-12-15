/*
 * SonarLint for Eclipse
 * Copyright (C) 2015 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.eclipse.core.internal.jobs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.SonarEclipseException;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.configurator.SonarConfiguratorProperties;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;

public class SonarLintAnalysisConfigurator {

  private SonarLintAnalysisConfigurator() {
  }

  /**
   * Populate properties with everything required for the SonarLint analysis in issues mode.
   */
  public static Collection<ProjectConfigurator> configureAnalysis(AnalyzeProjectRequest request, Properties properties, List<SonarLintProperty> extraProps,
    final IProgressMonitor monitor) {
    IProject project = request.getProject();
    final File baseDir = project.getLocation().toFile();
    IPath projectSpecificWorkDir = project.getWorkingLocation(SonarLintCorePlugin.PLUGIN_ID);

    // Preview mode by default
    properties.setProperty(SonarLintProperties.ANALYSIS_MODE, SonarLintProperties.ANALYSIS_MODE_ISSUES);
    properties.setProperty(SonarLintProperties.USE_WS_CACHE, "true");

    // Configuration by configurators (common and language specific)
    Collection<ProjectConfigurator> usedConfigurators = configure(project, request.getOnlyOnFiles(), properties, monitor);

    // Append workspace and project properties
    for (SonarLintProperty sonarProperty : extraProps) {
      properties.put(sonarProperty.getName(), sonarProperty.getValue());
    }
    if (request.getOnlyOnFiles() != null) {
      Collection<String> paths = new ArrayList<>(request.getOnlyOnFiles().size());
      for (IFile file : request.getOnlyOnFiles()) {
        paths.add(file.getProjectRelativePath().toString());
      }
      ProjectConfigurator.setPropertyList(properties, "sonar.tests", paths);
      ProjectConfigurator.setPropertyList(properties, "sonar.sources", paths);
    }

    properties.setProperty(SonarLintProperties.PROJECT_BASEDIR, baseDir.toString());
    properties.setProperty(SonarLintProperties.WORK_DIR, projectSpecificWorkDir.toString());

    return usedConfigurators;
  }

  private static Collection<ProjectConfigurator> configure(final IProject project, Collection<IFile> filesToAnalyze, final Properties properties, final IProgressMonitor monitor) {
    String projectName = project.getName();
    String encoding;
    try {
      encoding = project.getDefaultCharset();
    } catch (CoreException e) {
      throw new SonarEclipseException("Unable to get charset from project", e);
    }

    properties.setProperty(SonarLintProperties.PROJECT_NAME_PROPERTY, projectName);
    properties.setProperty(SonarLintProperties.PROJECT_VERSION_PROPERTY, "0.1-SNAPSHOT");
    properties.setProperty(SonarLintProperties.ENCODING_PROPERTY, encoding);

    ProjectConfigurationRequest configuratorRequest = new ProjectConfigurationRequest(project, filesToAnalyze, properties);
    Collection<ProjectConfigurator> configurators = ConfiguratorUtils.getConfigurators();
    Collection<ProjectConfigurator> usedConfigurators = new ArrayList<>();
    for (ProjectConfigurator configurator : configurators) {
      if (configurator.canConfigure(project)) {
        configurator.configure(configuratorRequest, monitor);
        usedConfigurators.add(configurator);
      }
    }

    configureSourcesAndTestsProps(project, properties);
    return usedConfigurators;
  }

  private static void configureSourcesAndTestsProps(final IProject project, final Properties properties) {
    ProjectConfigurator.appendProperty(properties, SonarConfiguratorProperties.TEST_INCLUSIONS_PROPERTY, PreferencesUtils.getTestFileRegexps());
    if (!properties.containsKey(SonarConfiguratorProperties.SOURCE_DIRS_PROPERTY) && !properties.containsKey(SonarConfiguratorProperties.TEST_DIRS_PROPERTY)) {
      // Try to analyze all files
      properties.setProperty(SonarConfiguratorProperties.SOURCE_DIRS_PROPERTY, ".");
      properties.setProperty(SonarConfiguratorProperties.TEST_DIRS_PROPERTY, ".");
      // Try to exclude derived folders
      IResource[] members;
      try {
        members = project.members();
      } catch (CoreException e) {
        throw new IllegalStateException("Unable to list members of " + project, e);
      }
      for (IResource member : members) {
        if (member.isDerived()) {
          ProjectConfigurator.appendProperty(properties, SonarConfiguratorProperties.SOURCE_EXCLUSIONS_PROPERTY, member.getName() + "/**/*");
          ProjectConfigurator.appendProperty(properties, SonarConfiguratorProperties.TEST_EXCLUSIONS_PROPERTY, member.getName() + "/**/*");
        }
      }
    }
  }

}
