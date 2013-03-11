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
package org.sonar.ide.eclipse.core.internal.jobs;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.SonarEclipseException;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;
import org.sonar.ide.eclipse.runner.SonarEclipseRunner;
import org.sonar.ide.eclipse.wsclient.SonarVersionTester;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.ServerQuery;

import java.io.File;
import java.io.FileReader;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class AnalyseProjectJob extends Job {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyseProjectJob.class);

  private final IProject project;
  private final boolean debugEnabled;
  private final SonarProject sonarProject;

  private List<SonarProperty> extraProps;

  public AnalyseProjectJob(IProject project, boolean debugEnabled) {
    this(project, debugEnabled, Collections.<SonarProperty> emptyList());
  }

  public AnalyseProjectJob(IProject project, boolean debugEnabled, List<SonarProperty> extraProps) {
    super(Messages.AnalyseProjectJob_title);
    this.project = project;
    this.debugEnabled = debugEnabled;
    this.extraProps = extraProps;
    this.sonarProject = SonarProject.getInstance(project);
    // Prevent modifications of project during analysis
    setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    monitor.beginTask(NLS.bind(Messages.AnalyseProjectJob_task_analysing, project.getName()), IProgressMonitor.UNKNOWN);

    // Verify Host
    if (getHost() == null) {
      return new Status(Status.ERROR, SonarCorePlugin.PLUGIN_ID,
          NLS.bind(Messages.No_matching_server_in_configuration_for_project, project.getName(), sonarProject.getUrl()));
    }
    if (!SonarVersionTester.isServerVersionSupported(SonarCorePlugin.LOCAL_MODE_MINIMAL_SONAR_VERSION, getServerVersion())) {
      return new Status(Status.ERROR, SonarCorePlugin.PLUGIN_ID,
          NLS.bind(Messages.AnalyseProjectJob_unsupported_version, SonarCorePlugin.LOCAL_MODE_MINIMAL_SONAR_VERSION));
    }

    // Configure
    Properties properties = new Properties();
    File outputFile = configureAnalysis(monitor, properties, extraProps);

    // Analyse
    // To be sure to not reuse something from a previous analysis
    FileUtils.deleteQuietly(outputFile);
    IStatus result;
    try {
      result = SonarEclipseRunner.run(project, properties, debugEnabled, monitor);
    } catch (Exception e) {
      return new Status(Status.ERROR, SonarCorePlugin.PLUGIN_ID, "Error when executing Sonar runner", e);
    }
    if (result != Status.OK_STATUS) {
      return result;
    }

    // Create markers and save measures
    createMarkers(monitor, outputFile);

    // Update analysis date
    sonarProject.setLastAnalysisDate(Calendar.getInstance().getTime());
    sonarProject.save();

    monitor.done();
    return Status.OK_STATUS;
  }

  private Host getHost() {
    return SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
  }

  private Sonar create() {
    return WSClientFactory.create(getHost());
  }

  public String getServerVersion() {
    return create().find(new ServerQuery()).getVersion();
  }

  @VisibleForTesting
  public void createMarkers(final IProgressMonitor monitor, File outputFile) {
    FileReader fileReader = null;
    try {
      fileReader = new FileReader(outputFile);
      Object obj = JSONValue.parse(fileReader);
      JSONObject sonarResult = (JSONObject) obj;
      final JSONObject violationByResources = (JSONObject) sonarResult.get("violations_per_resource");
      MarkerUtils.deleteViolationsMarkers(project);
      project.accept(new IResourceVisitor() {
        public boolean visit(IResource resource) throws CoreException {
          String sonarKey = ResourceUtils.getSonarResourcePartialKey(resource);
          if (sonarKey != null && violationByResources.get(sonarKey) != null) {
            MarkerUtils.createMarkersForJSONViolations(resource, (JSONArray) violationByResources.get(sonarKey));
          }
          // don't go deeper than file
          return resource instanceof IFile ? false : true;
        }
      });
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new SonarEclipseException("Unable to create markers", e);
    } finally {
      IOUtils.closeQuietly(fileReader);
    }
  }

  /**
   * Populate properties with everything required for the Sonar analysis in dryRun mode.
   * @param monitor
   * @param properties
   * @return
   */
  @VisibleForTesting
  public File configureAnalysis(final IProgressMonitor monitor, Properties properties, List<SonarProperty> extraProps) {
    File baseDir = project.getLocation().toFile();
    IPath projectSpecificWorkDir = project.getWorkingLocation(SonarCorePlugin.PLUGIN_ID);
    File outputFile = new File(projectSpecificWorkDir.toFile(), "dryRun.json");

    // First start by appending workspace and project properties
    for (SonarProperty sonarProperty : extraProps) {
      properties.put(sonarProperty.getName(), sonarProperty.getValue());
    }

    // Configuration by configurators (common and language specific)
    ConfiguratorUtils.configure(project, properties, monitor);

    // Global configuration
    Host host = getHost();
    properties.setProperty(SonarProperties.SONAR_URL, host.getHost());
    if (StringUtils.isNotBlank(host.getUsername())) {
      properties.setProperty(SonarProperties.SONAR_LOGIN, host.getUsername());
      properties.setProperty(SonarProperties.SONAR_PASSWORD, host.getPassword());
    }
    properties.setProperty(SonarProperties.PROJECT_BASEDIR, baseDir.toString());
    properties.setProperty(SonarProperties.WORK_DIR, projectSpecificWorkDir.toString());
    properties.setProperty(SonarProperties.DRY_RUN_PROPERTY, "true");
    // Output file is relative to working dir
    properties.setProperty(SonarProperties.DRY_RUN_OUTPUT_PROPERTY, outputFile.getName());
    if (debugEnabled) {
      properties.setProperty(SonarProperties.VERBOSE_PROPERTY, "true");
    }
    return outputFile;
  }

}
