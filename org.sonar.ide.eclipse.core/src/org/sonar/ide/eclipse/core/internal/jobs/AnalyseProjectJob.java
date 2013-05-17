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
package org.sonar.ide.eclipse.core.internal.jobs;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.SonarEclipseException;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;
import org.sonar.ide.eclipse.wsclient.SonarVersionTester;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;
import org.sonar.runner.api.ForkedRunner;
import org.sonar.runner.api.StreamConsumer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class AnalyseProjectJob extends Job {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyseProjectJob.class);

  private final IProject project;
  private final boolean debugEnabled;
  private final SonarProject sonarProject;

  private List<SonarProperty> extraProps;

  private String jvmArgs;

  private ISonarServer sonarServer;

  public AnalyseProjectJob(IProject project, boolean debugEnabled) {
    this(project, debugEnabled, Collections.<SonarProperty> emptyList(), null);
  }

  public AnalyseProjectJob(IProject project, boolean debugEnabled, List<SonarProperty> extraProps, String jvmArgs) {
    super(Messages.AnalyseProjectJob_title);
    this.project = project;
    this.debugEnabled = debugEnabled;
    this.extraProps = extraProps;
    this.jvmArgs = jvmArgs != null ? jvmArgs : "";
    this.sonarProject = SonarProject.getInstance(project);
    this.sonarServer = SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
    // Prevent modifications of project during analysis
    setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    monitor.beginTask(NLS.bind(Messages.AnalyseProjectJob_task_analysing, project.getName()), IProgressMonitor.UNKNOWN);

    // Verify Host
    if (getSonarServer() == null) {
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
      result = run(project, properties, debugEnabled, monitor);
    } catch (Exception e) {
      return new Status(Status.ERROR, SonarCorePlugin.PLUGIN_ID, "Error when executing Sonar runner", e);
    }
    if (result != Status.OK_STATUS) {
      return result;
    }

    MarkerUtils.deleteIssuesMarkers(project);
    createMarkersFromReportOutput(monitor, outputFile);

    // Update analysis date
    sonarProject.setLastAnalysisDate(Calendar.getInstance().getTime());
    sonarProject.save();

    monitor.done();
    return Status.OK_STATUS;
  }

  private String getServerVersion() {
    return WSClientFactory.getSonarClient(sonarServer).getServerVersion();
  }

  private ISonarServer getSonarServer() {
    return sonarServer;
  }

  @VisibleForTesting
  public void createMarkersFromReportOutput(final IProgressMonitor monitor, File outputFile) {
    FileReader fileReader = null;
    try {
      fileReader = new FileReader(outputFile);
      Object obj = JSONValue.parse(fileReader);
      JSONObject sonarResult = (JSONObject) obj;
      // Start by resolving all components in a cache
      Map<String, IResource> resourcesByKey = Maps.newHashMap();
      final JSONArray components = (JSONArray) sonarResult.get("components");
      for (Object component : components) {
        String key = ObjectUtils.toString(((JSONObject) component).get("key"));
        IResource resource = ResourceUtils.findResource(sonarProject, key);
        if (resource != null) {
          resourcesByKey.put(key, resource);
        }
      }
      // Now read all rules nane in a cache
      Map<String, String> ruleByKey = Maps.newHashMap();
      final JSONArray rules = (JSONArray) sonarResult.get("rules");
      for (Object rule : rules) {
        String key = ObjectUtils.toString(((JSONObject) rule).get("key"));
        String name = ObjectUtils.toString(((JSONObject) rule).get("name"));
        ruleByKey.put(key, name);
      }
      // Now iterate over all issues and create markers
      MarkerUtils.createMarkersForJSONIssues(resourcesByKey, ruleByKey, (JSONArray) sonarResult.get("issues"));
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
    File outputFile = new File(projectSpecificWorkDir.toFile(), "sonar-report.json");

    // First start by appending workspace and project properties
    for (SonarProperty sonarProperty : extraProps) {
      properties.put(sonarProperty.getName(), sonarProperty.getValue());
    }

    // Configuration by configurators (common and language specific)
    ConfiguratorUtils.configure(project, properties, monitor);

    // Global configuration
    properties.setProperty(SonarProperties.SONAR_URL, getSonarServer().getUrl());
    if (StringUtils.isNotBlank(getSonarServer().getUsername())) {
      properties.setProperty(SonarProperties.SONAR_LOGIN, getSonarServer().getUsername());
      properties.setProperty(SonarProperties.SONAR_PASSWORD, getSonarServer().getPassword());
    }
    properties.setProperty(SonarProperties.PROJECT_BASEDIR, baseDir.toString());
    properties.setProperty(SonarProperties.WORK_DIR, projectSpecificWorkDir.toString());
    properties.setProperty(SonarProperties.DRY_RUN_PROPERTY, "true");
    // Output file is relative to working dir
    properties.setProperty(SonarProperties.REPORT_OUTPUT_PROPERTY, outputFile.getName());
    if (debugEnabled) {
      properties.setProperty(SonarProperties.VERBOSE_PROPERTY, "true");
    }
    return outputFile;
  }

  /** Run the action. Display the Hello World message
   * @throws InterruptedException
   * @throws CoreException
   * @throws IOException
   */
  public IStatus run(IProject project, Properties props, boolean debugEnabled, final IProgressMonitor monitor) throws InterruptedException,
      CoreException, IOException {

    try {

      if (debugEnabled) {
        SonarCorePlugin.getDefault().info("Start sonar-runner with args:\n" + propsToString(props));
      }

      ForkedRunner.create()
          .setApp("Eclipse", SonarCorePlugin.getDefault().getBundle().getVersion().toString())
          .addProperties(props)
          .addJvmArguments(jvmArgs.trim().split("\\s+"))
          .setStdOut(new StreamConsumer() {
            public void consumeLine(String text) {
              SonarCorePlugin.getDefault().info(text + "\n");
            }
          })
          .setStdErr(new StreamConsumer() {
            public void consumeLine(String text) {
              SonarCorePlugin.getDefault().error(text + "\n");
            }
          })
          .execute();

      return Status.OK_STATUS;
    } catch (Exception e) {
      return new Status(Status.ERROR, SonarCorePlugin.PLUGIN_ID, "Error during execution of Sonar", e);
    }

  }

  private static String propsToString(Properties props) {
    StringBuilder builder = new StringBuilder();
    for (Object key : props.keySet()) {
      builder.append(key).append("=").append(props.getProperty(key.toString())).append("\n");
    }
    return builder.toString();
  }

}
