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

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
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
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurator;
import org.sonar.ide.eclipse.internal.core.Messages;
import org.sonar.ide.eclipse.internal.core.markers.MarkerUtils;
import org.sonar.ide.eclipse.internal.core.resources.ProjectProperties;
import org.sonar.ide.eclipse.internal.core.resources.ResourceUtils;
import org.sonar.ide.eclipse.runner.SonarEclipseRunner;
import org.sonar.ide.eclipse.runner.SonarProperties;
import org.sonar.wsclient.Host;

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
    Properties properties = new Properties();
    File outputFile = configureAnalysis(monitor, properties);

    // Analyse
    FileUtils.deleteQuietly(outputFile); // To be sure to not reuse something from a previous analysis
    IStatus result = SonarEclipseRunner.run(project, properties, monitor);
    if (result != Status.OK_STATUS) {
      return result;
    }

    // Create markers and save measures
    createMarkers(monitor, outputFile);

    monitor.done();
    return Status.OK_STATUS;
  }

  @VisibleForTesting
  public void createMarkers(final IProgressMonitor monitor, File outputFile) {
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
  }

  /**
   * Populate properties with everything required for the Sonar analysis in dryRun mode.
   * @param monitor
   * @param properties
   * @return
   */
  @VisibleForTesting
  public File configureAnalysis(final IProgressMonitor monitor, Properties properties) {
    File baseDir = project.getLocation().toFile();
    IPath projectSpecificWorkDir = project.getWorkingLocation(SonarCorePlugin.PLUGIN_ID);
    // FIXME For now outputFile should be relative to work dir IPath pluginWorkDir = SonarCorePlugin.getDefault().getStateLocation();
    IPath pluginWorkDir = projectSpecificWorkDir;
    File outputFile = new File(projectSpecificWorkDir.toFile(), "dryRun.json");

    ProjectConfigurationRequest request = new ProjectConfigurationRequest(project, properties);
    for (ProjectConfigurator configurator : getConfigurators()) {
      LOG.debug("Project configurator: {}", configurator);
      configurator.configure(request, monitor);
    }

    ProjectProperties projectProperties = ProjectProperties.getInstance(project);
    Host host = SonarCorePlugin.getServersManager().findServer(projectProperties.getUrl());
    properties.setProperty(SonarProperties.SONAR_URL, host.getHost());
    if (StringUtils.isNotBlank(host.getUsername())) {
      properties.setProperty(SonarProperties.SONAR_LOGIN, host.getUsername());
      properties.setProperty(SonarProperties.SONAR_PASSWORD, host.getPassword());
    }
    properties.setProperty(SonarProperties.PROJECT_BASEDIR, baseDir.toString());
    properties.setProperty(SonarProperties.WORK_DIR, pluginWorkDir.toString());
    properties.setProperty(SonarProperties.DRY_RUN_PROPERTY, "true");
    properties.setProperty(SonarProperties.DRY_RUN_OUTPUT_PROPERTY, outputFile.getName()); // Output file is relative to working dir
    if (isDebugEnabled()) {
      properties.setProperty(SonarProperties.VERBOSE_PROPERTY, "true");
    }
    return outputFile;
  }

  private boolean isDebugEnabled() {
    return Platform.getPreferencesService().getBoolean(SonarProperties.UI_PLUGIN_ID, SonarProperties.P_DEBUG_OUTPUT, false, null);
  }

}
