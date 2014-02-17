/*
 * SonarQube Eclipse
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
import org.sonar.runner.api.ProcessMonitor;
import org.sonar.runner.api.StreamConsumer;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
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

  private boolean incremental;

  private String serverVersion;

  public AnalyseProjectJob(AnalyseProjectRequest request) {
    super(Messages.AnalyseProjectJob_title);
    this.project = request.getProject();
    this.debugEnabled = request.isDebugEnabled();
    this.incremental = !request.isForceFullPreview();
    this.extraProps = request.getExtraProps();
    this.jvmArgs = StringUtils.defaultIfBlank(request.getJvmArgs(), "");
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
    if (incremental && !SonarVersionTester.isServerVersionSupported(SonarCorePlugin.INCREMENTAL_MODE_MINIMAL_SONAR_VERSION, getServerVersion())) {
      SonarCorePlugin.getDefault().info(NLS.bind(Messages.AnalyseProjectJob_unsupported_version, SonarCorePlugin.INCREMENTAL_MODE_MINIMAL_SONAR_VERSION));
      this.incremental = false;
    }

    // Configure
    Properties properties = new Properties();
    File outputFile = configureAnalysis(monitor, properties, extraProps);

    // Analyse
    // To be sure to not reuse something from a previous analysis
    FileUtils.deleteQuietly(outputFile);
    IStatus result;
    long start = System.currentTimeMillis();
    SonarCorePlugin.getDefault().info("Start SonarQube analysis on " + project.getName() + "...\n");
    try {
      result = run(project, properties, debugEnabled, monitor);
    } catch (Exception e) {
      return new Status(Status.ERROR, SonarCorePlugin.PLUGIN_ID, "Error when executing SonarQube runner", e);
    }
    if (result != Status.OK_STATUS) {
      return result;
    }
    SonarCorePlugin.getDefault().debug("Done in " + (System.currentTimeMillis() - start) + "ms\n");

    // Create markers
    long startMarker = System.currentTimeMillis();
    SonarCorePlugin.getDefault().debug("Create markers on project " + project.getName() + " resources...\n");
    if (SonarVersionTester.isServerVersionSupported(SonarCorePlugin.PATH_RESOURCE_MINIMAL_SONAR_VERSION, getServerVersion())) {
      createMarkersFromReportOutput(monitor, outputFile);
    } else {
      createMarkersFromReportOutputBefore4_2(monitor, outputFile);
    }
    SonarCorePlugin.getDefault().debug("Done in " + (System.currentTimeMillis() - startMarker) + "ms\n");

    // Update analysis date
    sonarProject.setLastAnalysisDate(Calendar.getInstance().getTime());
    sonarProject.save();

    monitor.done();
    return Status.OK_STATUS;
  }

  @VisibleForTesting
  public void setIncremental(boolean incremental) {
    this.incremental = incremental;
  }

  private String getServerVersion() {
    if (serverVersion == null) {
      serverVersion = WSClientFactory.getSonarClient(sonarServer).getServerVersion();
    }
    return serverVersion;
  }

  @VisibleForTesting
  public void setServerVersion(String serverVersion) {
    this.serverVersion = serverVersion;
  }

  private ISonarServer getSonarServer() {
    return sonarServer;
  }

  @VisibleForTesting
  public void createMarkersFromReportOutputBefore4_2(final IProgressMonitor monitor, File outputFile) {
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
          if (incremental) {
            MarkerUtils.deleteIssuesMarkers(resource);
          }
          MarkerUtils.markResourceAsLocallyAnalysed(resource);
        }
      }
      // Now read all rules name in a cache
      Map<String, String> ruleByKey = readRules(sonarResult);
      // Now read all users name in a cache
      Map<String, String> userNameByLogin = readUserNameByLogin(sonarResult);
      // Now iterate over all issues and create markers
      MarkerUtils.createMarkersForJSONIssues(resourcesByKey, ruleByKey, userNameByLogin, (JSONArray) sonarResult.get("issues"));
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new SonarEclipseException("Unable to create markers", e);
    } finally {
      IOUtils.closeQuietly(fileReader);
    }
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
        String path = ObjectUtils.toString(((JSONObject) component).get("path"));
        String moduleKey = ObjectUtils.toString(((JSONObject) component).get("moduleKey"));
        String status = ObjectUtils.toString(((JSONObject) component).get("status"));
        IResource resource = ResourceUtils.findResource(sonarProject, key, moduleKey, path);
        if (resource != null) {
          resourcesByKey.put(key, resource);
          if (incremental && !"SAME".equals(status)) {
            MarkerUtils.deleteIssuesMarkers(resource);
          }
          MarkerUtils.markResourceAsLocallyAnalysed(resource);
        }
      }
      // Now read all rules name in a cache
      Map<String, String> ruleByKey = readRules(sonarResult);
      // Now read all users name in a cache
      Map<String, String> userNameByLogin = readUserNameByLogin(sonarResult);
      // Now iterate over all issues and create markers
      MarkerUtils.createMarkersForJSONIssues(resourcesByKey, ruleByKey, userNameByLogin, (JSONArray) sonarResult.get("issues"));
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      throw new SonarEclipseException("Unable to create markers", e);
    } finally {
      IOUtils.closeQuietly(fileReader);
    }
  }

  private Map<String, String> readRules(JSONObject sonarResult) {
    Map<String, String> ruleByKey = Maps.newHashMap();
    final JSONArray rules = (JSONArray) sonarResult.get("rules");
    for (Object rule : rules) {
      String key = ObjectUtils.toString(((JSONObject) rule).get("key"));
      String name = ObjectUtils.toString(((JSONObject) rule).get("name"));
      ruleByKey.put(key, name);
    }
    return ruleByKey;
  }

  private Map<String, String> readUserNameByLogin(JSONObject sonarResult) {
    Map<String, String> userNameByLogin = Maps.newHashMap();
    final JSONArray users = (JSONArray) sonarResult.get("users");
    if (users != null) {
      for (Object user : users) {
        String login = ObjectUtils.toString(((JSONObject) user).get("login"));
        String name = ObjectUtils.toString(((JSONObject) user).get("name"));
        userNameByLogin.put(login, name);
      }
    }
    return userNameByLogin;
  }

  /**
   * Populate properties with everything required for the SonarQube analysis in dryRun mode.
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
    ConfiguratorUtils.configure(project, properties, getServerVersion(), monitor);

    // Global configuration
    properties.setProperty(SonarProperties.SONAR_URL, getSonarServer().getUrl());
    if (StringUtils.isNotBlank(getSonarServer().getUsername())) {
      properties.setProperty(SonarProperties.SONAR_LOGIN, getSonarServer().getUsername());
      properties.setProperty(SonarProperties.SONAR_PASSWORD, getSonarServer().getPassword());
    }
    properties.setProperty(SonarProperties.PROJECT_BASEDIR, baseDir.toString());
    properties.setProperty(SonarProperties.WORK_DIR, projectSpecificWorkDir.toString());
    if (incremental) {
      properties.setProperty(SonarProperties.ANALYSIS_MODE, SonarProperties.ANALYSIS_MODE_INCREMENTAL);
    } else {
      if (SonarVersionTester.isServerVersionSupported(SonarCorePlugin.INCREMENTAL_MODE_MINIMAL_SONAR_VERSION, getServerVersion())) {
        properties.setProperty(SonarProperties.ANALYSIS_MODE, SonarProperties.ANALYSIS_MODE_PREVIEW);
      } else {
        properties.setProperty(SonarProperties.DRY_RUN_PROPERTY, "true");
      }
    }
    // Output file is relative to working dir
    properties.setProperty(SonarProperties.REPORT_OUTPUT_PROPERTY, outputFile.getName());
    if (debugEnabled) {
      properties.setProperty(SonarProperties.VERBOSE_PROPERTY, "true");
    }
    return outputFile;
  }

  public IStatus run(IProject project, Properties props, boolean debugEnabled, final IProgressMonitor monitor) throws InterruptedException,
    CoreException, IOException {

    try {

      if (debugEnabled) {
        SonarCorePlugin.getDefault().info("Start sonar-runner with args:\n" + propsToString(props));
      }

      ForkedRunner.create(new ProcessMonitor() {
        @Override
        public boolean stop() {
          return monitor.isCanceled();
        }
      })
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

      return checkCancel(monitor);
    } catch (Exception e) {
      return handleException(monitor, e);
    }

  }

  private IStatus checkCancel(final IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }

  private IStatus handleException(final IProgressMonitor monitor, Exception e) {
    if (monitor.isCanceled()) {
      // On OSX it seems that cancelling produce an exception
      return Status.CANCEL_STATUS;
    }
    return new Status(Status.ERROR, SonarCorePlugin.PLUGIN_ID, "Error during execution of Sonar", e);
  }

  private static String propsToString(Properties props) {
    StringBuilder builder = new StringBuilder();
    for (Object key : props.keySet()) {
      builder.append(key).append("=").append(props.getProperty(key.toString())).append("\n");
    }
    return builder.toString();
  }

}
