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
package org.sonar.ide.eclipse.core.jobs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurator;
import org.sonar.ide.eclipse.internal.core.Messages;
import org.sonar.ide.eclipse.internal.core.markers.MarkerUtils;
import org.sonar.ide.eclipse.internal.core.resources.ProjectProperties;
import org.sonar.ide.eclipse.internal.core.resources.ResourceUtils;
import org.sonar.ide.eclipse.runner.SonarEclipseRunner;
import org.sonar.ide.eclipse.runner.SonarProperties;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class AnalyseProjectJob extends Job {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyseProjectJob.class);

  private final IProject project;

  public AnalyseProjectJob(IProject project) {
    super(Messages.AnalyseProjectJob_title);
    this.project = project;
    setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule()); // Prevent modifications of project during analysis
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
  protected IStatus run(final IProgressMonitor monitor) {
    monitor.beginTask(NLS.bind(Messages.AnalyseProjectJob_task_analysing, project.getName()), IProgressMonitor.UNKNOWN);

    // Configure
    File baseDir = project.getLocation().toFile();
    File workDir = new File(baseDir, "target/sonar-embedder-work"); // TODO hard-coded value
    File outputFile = new File(workDir, "dryRun.json");
    Properties properties = new Properties();
    ProjectConfigurationRequest request = new ProjectConfigurationRequest(project, properties);
    for (ProjectConfigurator configurator : getConfigurators()) {
      LOG.debug("Project configurator: {}", configurator);
      configurator.configure(request, monitor);
    }

    ProjectProperties projectProperties = ProjectProperties.getInstance(project);
    properties.setProperty(SonarProperties.SONAR_URL, projectProperties.getUrl());
    properties.setProperty(SonarProperties.PROJECT_BASEDIR, baseDir.toString());
    properties.setProperty(SonarProperties.WORK_DIR, workDir.toString());
    properties.setProperty(SonarProperties.DRY_RUN_PROPERTY, "true");
    properties.setProperty(SonarProperties.DRY_RUN_OUTPUT_PROPERTY, outputFile.getName()); // Output file is relative to working dir
    if (isDebugEnabled()) {
      properties.setProperty(SonarProperties.VERBOSE_PROPERTY, "true");
    }

    // Analyse
    IStatus result = SonarEclipseRunner.run(project, properties, monitor);
    if (result != Status.OK_STATUS) {
      return result;
    }

    // Create markers and save measures
    try {
      Object obj = JSONValue.parse(new FileReader(outputFile));
      JSONObject sonarResult = (JSONObject) obj;
      final JSONObject violationByResources = (JSONObject) sonarResult.get("violations_per_resource");
      project.accept(new IResourceVisitor() {
        public boolean visit(IResource resource) throws CoreException {
          MarkerUtils.deleteViolationsMarkers(resource);
          String sonarKey = ResourceUtils.getSonarKey(resource, monitor);
          if (sonarKey != null && violationByResources.get(sonarKey) != null) {
            MarkerUtils.createMarkersForViolations(resource, (JSONArray) violationByResources.get(sonarKey));
          }
          // don't go deeper than file
          return resource instanceof IFile ? false : true;
        }
      });
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }

    monitor.done();
    return Status.OK_STATUS;
  }

  private boolean isDebugEnabled() {
    return Platform.getPreferencesService().getBoolean(SonarProperties.UI_PLUGIN_ID, SonarProperties.P_DEBUG_OUTPUT, false, null);
  }

}
