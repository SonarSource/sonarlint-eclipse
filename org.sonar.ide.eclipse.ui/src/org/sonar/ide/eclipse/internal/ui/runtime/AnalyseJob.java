/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.internal.ui.runtime;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.batch.Activator;
import org.sonar.batch.SonarEclipseRuntime;
import org.sonar.batch.bootstrapper.ProjectDefinition;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurator;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurationRequest;

public class AnalyseJob extends Job {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyseJob.class);

  private IProject project;

  public AnalyseJob(IProject project) {
    super("Sonar local");
    this.project = project;
  }

  private Collection<ProjectConfigurator> getConfigurators() {
    List<ProjectConfigurator> result = new ArrayList<ProjectConfigurator>();
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IConfigurationElement[] config = registry.getConfigurationElementsFor("org.sonar.ide.eclipse.core.projectConfigurators");
    for (final IConfigurationElement element : config) {
      try {
        Object obj = element.createExecutableExtension(ProjectConfigurator.ATTR_CLASS);
        ProjectConfigurator configurator = (ProjectConfigurator) obj;
        result.add(configurator);
      } catch (CoreException e) {
        LOG.error(e.getMessage(), e);
      }
    }
    return result;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    File baseDir = project.getLocation().toFile();
    File workDir = new File(baseDir, "target/sonar-embedder-work"); // TODO hard-coded value
    Properties properties = new Properties();
    ProjectDefinition sonarProject = new ProjectDefinition(baseDir, workDir, properties);
    ProjectConfigurationRequest request = new ProjectConfigurationRequest(project, sonarProject);
    for (ProjectConfigurator configurator : getConfigurators()) {
      LOG.debug("Project configurator: {}", configurator);
      configurator.configure(request, monitor);
    }

    SonarEclipseRuntime runtime = new SonarEclipseRuntime(Activator.getDefault().getPlugins());
    runtime.start();
    runtime.analyse(sonarProject);

    try {
      project.accept(new MarkersCreator(monitor, runtime.getIndex()));
    } catch (CoreException e) {
      LOG.error(e.getMessage(), e);
    }

    runtime.stop();

    return Status.OK_STATUS;
  }
}
