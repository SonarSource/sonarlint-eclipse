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
package org.sonarlint.eclipse.core.internal.configurator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.sonarlint.eclipse.core.SonarEclipseException;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.configurator.SonarConfiguratorProperties;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.jobs.SonarLintProperties;

/**
 * Utility class to deal with configurator extension point.
 * @author Julien Henry
 *
 */
public class ConfiguratorUtils {

  public static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  private ConfiguratorUtils() {
    // Utility class
  }

  private static Collection<ProjectConfigurator> getConfigurators() {
    List<ProjectConfigurator> result = new ArrayList<ProjectConfigurator>();
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IConfigurationElement[] config = registry.getConfigurationElementsFor("org.sonarlint.eclipse.core.projectConfigurators");
    for (final IConfigurationElement element : config) {
      try {
        Object obj = element.createExecutableExtension(ATTR_CLASS);
        ProjectConfigurator configurator = (ProjectConfigurator) obj;
        result.add(configurator);
      } catch (CoreException e) {
        SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
      }
    }
    return result;
  }

  public static void configure(final IProject project, Collection<IFile> filesToAnalyze, final Properties properties, final IProgressMonitor monitor) {
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

    ProjectConfigurationRequest request = new ProjectConfigurationRequest(project, filesToAnalyze, properties);
    for (ProjectConfigurator configurator : ConfiguratorUtils.getConfigurators()) {
      if (configurator.canConfigure(project)) {
        configurator.configure(request, monitor);
      }
    }

    ProjectConfigurator.appendProperty(properties, SonarConfiguratorProperties.TEST_INCLUSIONS_PROPERTY, PreferencesUtils.getTestFileRegexps());
    if (!properties.containsKey(SonarConfiguratorProperties.SOURCE_DIRS_PROPERTY) && !properties.containsKey(SonarConfiguratorProperties.TEST_DIRS_PROPERTY)) {
      // Try to analyze all files
      properties.setProperty(SonarConfiguratorProperties.SOURCE_DIRS_PROPERTY, ".");
      properties.setProperty(SonarConfiguratorProperties.TEST_DIRS_PROPERTY, ".");
      // Try to exclude derived folders
      try {
        for (IResource member : project.members()) {
          if (member.isDerived()) {
            ProjectConfigurator.appendProperty(properties, SonarConfiguratorProperties.SOURCE_EXCLUSIONS_PROPERTY, member.getName() + "/**/*");
            ProjectConfigurator.appendProperty(properties, SonarConfiguratorProperties.TEST_EXCLUSIONS_PROPERTY, member.getName() + "/**/*");
          }
        }
      } catch (CoreException e) {
        throw new IllegalStateException("Unable to list members of " + project, e);
      }
    }
  }

}
