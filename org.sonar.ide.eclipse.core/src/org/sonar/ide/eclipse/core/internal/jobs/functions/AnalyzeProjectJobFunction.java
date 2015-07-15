/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
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
package org.sonar.ide.eclipse.core.internal.jobs.functions;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.SonarEclipseException;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonar.ide.eclipse.core.internal.jobs.AnalyseProjectRequest;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;
import org.sonar.runner.api.ForkedRunner;
import org.sonar.runner.api.ProcessMonitor;
import org.sonar.runner.api.StreamConsumer;

/**
 * Live preview analysis job.
 *
 */
@SuppressWarnings("nls")
public class AnalyzeProjectJobFunction implements IJobFunction {

  private final IProject project;
  private final boolean debugEnabled;
  private final SonarProject sonarProject;

  private final List<SonarProperty> extraProps;

  private final String jvmArgs;

  private final ISonarServer sonarServer;

  private boolean incremental;

  /**
   * @param request Job request
   */
  public AnalyzeProjectJobFunction(final AnalyseProjectRequest request) {
    this.project = request.getProject();
    this.debugEnabled = request.isDebugEnabled();
    this.incremental = !request.isForceFullPreview();
    this.extraProps = request.getExtraProps();
    this.jvmArgs = StringUtils.defaultIfBlank(request.getJvmArgs(), "");
    this.sonarProject = SonarProject.getInstance(project);
    this.sonarServer = SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
  }

  @Override
  public IStatus run(final IProgressMonitor monitor) {
    monitor.beginTask(NLS.bind(Messages.AnalyseProjectJob_task_analyzing, project.getName()), IProgressMonitor.UNKNOWN);

    // Verify Host
    if (getSonarServer() == null) {
      return new Status(IStatus.ERROR, SonarCorePlugin.PLUGIN_ID,
        NLS.bind(Messages.No_matching_server_in_configuration_for_project, project.getName(), sonarProject.getUrl()));
    }
    // Verify version and server is reachable
    if (getServerVersion() == null) {
      return new Status(IStatus.ERROR, SonarCorePlugin.PLUGIN_ID,
        NLS.bind(Messages.Unable_to_detect_server_version, sonarProject.getUrl()));
    }

    // Configure
    final Properties properties = new Properties();
    final File outputFile = configureAnalysis(monitor, properties, extraProps);

    // Analyze
    // To be sure to not reuse something from a previous analysis
    try {
      Files.deleteIfExists(outputFile.toPath());
    } catch (final IOException e) {
      return new Status(IStatus.ERROR, SonarCorePlugin.PLUGIN_ID, "Unable to delete", e);
    }
    IStatus result;
    final long start = System.currentTimeMillis();
    SonarCorePlugin.getDefault().info("\nStart SonarQube analysis on " + project.getName() + "...\n");
    try {
      result = runAnalysis(project, properties, debugEnabled, monitor);
    } catch (final Exception e) {
      return new Status(IStatus.ERROR, SonarCorePlugin.PLUGIN_ID, "Error when executing SonarQube runner", e);
    }
    if (result != Status.OK_STATUS) {
      return result;
    }
    SonarCorePlugin.getDefault().debug("Done in " + (System.currentTimeMillis() - start) + "ms\n");

    // Create markers
    final long startMarker = System.currentTimeMillis();
    SonarCorePlugin.getDefault().debug("Create markers on project " + project.getName() + " resources...\n");
    createMarkersFromReportOutput(outputFile);
    SonarCorePlugin.getDefault().debug("Done in " + (System.currentTimeMillis() - startMarker) + "ms\n");

    // Update analysis date
    sonarProject.setLastAnalysisDate(Calendar.getInstance().getTime());
    sonarProject.save();

    monitor.done();
    return Status.OK_STATUS;
  }

  /**
   * @param incremental
   */
  @VisibleForTesting
  public void setIncremental(final boolean incremental) {
    this.incremental = incremental;
  }

  private String getServerVersion() {
    return sonarServer.getVersion();
  }

  private ISonarServer getSonarServer() {
    return sonarServer;
  }

  @SuppressWarnings("javadoc")
  @VisibleForTesting
  public void createMarkersFromReportOutput(final File outputFile) {
    try (FileReader fileReader = new FileReader(outputFile)) {
      final Object obj = JSONValue.parse(fileReader);
      final JSONObject sonarResult = (JSONObject) obj;
      // Start by resolving all components in a cache
      final Map<String, IResource> resourcesByKey = Maps.newHashMap();
      final JSONArray components = (JSONArray) sonarResult.get("components");
      for (final Object component : components) {
        final String key = ObjectUtils.toString(((JSONObject) component).get("key"));
        final String status = ObjectUtils.toString(((JSONObject) component).get("status"));
        final IResource resource = ResourceUtils.findResource(sonarProject, key);
        if (resource != null) {
          resourcesByKey.put(key, resource);
          if (incremental
            // Status is blank for modules
            && StringUtils.isNotBlank(status)
            && !"SAME".equals(status)) {
            MarkerUtils.deleteIssuesMarkers(resource);
          }
          MarkerUtils.markResourceAsLocallyAnalysed(resource);
        }
      }
      // Now read all rules name in a cache
      final Map<String, String> ruleByKey = readRules(sonarResult);
      // Now read all users name in a cache
      final Map<String, String> userNameByLogin = readUserNameByLogin(sonarResult);
      // Now iterate over all issues and create markers
      MarkerUtils.createMarkersForJSONIssues(resourcesByKey, ruleByKey, userNameByLogin, (JSONArray) sonarResult.get("issues"));
    } catch (final Exception e) {
      throw new SonarEclipseException("Unable to create markers", e);
    }
  }

  private Map<String, String> readRules(final JSONObject sonarResult) {
    final Map<String, String> ruleByKey = Maps.newHashMap();
    final JSONArray rules = (JSONArray) sonarResult.get("rules");
    for (final Object rule : rules) {
      final String key = ObjectUtils.toString(((JSONObject) rule).get("key"));
      final String name = ObjectUtils.toString(((JSONObject) rule).get("name"));
      ruleByKey.put(key, name);
    }
    return ruleByKey;
  }

  private Map<String, String> readUserNameByLogin(final JSONObject sonarResult) {
    final Map<String, String> userNameByLogin = Maps.newHashMap();
    final JSONArray users = (JSONArray) sonarResult.get("users");
    if (users != null) {
      for (final Object user : users) {
        final String login = ObjectUtils.toString(((JSONObject) user).get("login"));
        final String name = ObjectUtils.toString(((JSONObject) user).get("name"));
        userNameByLogin.put(login, name);
      }
    }
    return userNameByLogin;
  }

  /**
   * Populate properties with everything required for the SonarQube analysis in dryRun mode.
   * @param monitor
   * @param properties
   * @param extraProperties
   * @return
   */
  @VisibleForTesting
  public File configureAnalysis(final IProgressMonitor monitor, final Properties properties, final List<SonarProperty> extraProperties) {
    final File baseDir = project.getLocation().toFile();
    final IPath projectSpecificWorkDir = project.getWorkingLocation(SonarCorePlugin.PLUGIN_ID);
    final File outputFile = new File(projectSpecificWorkDir.toFile(), "sonar-report.json");

    // Configuration by configurators (common and language specific)
    ConfiguratorUtils.configure(project, properties, getServerVersion(), monitor);

    // Append workspace and project properties
    for (final SonarProperty sonarProperty : extraProperties) {
      properties.put(sonarProperty.getName(), sonarProperty.getValue());
    }
    // Server configuration can't be overridden by user
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
      properties.setProperty(SonarProperties.ANALYSIS_MODE, SonarProperties.ANALYSIS_MODE_PREVIEW);
    }
    // Output file is relative to working dir
    properties.setProperty(SonarProperties.REPORT_OUTPUT_PROPERTY, outputFile.getName());
    if (debugEnabled) {
      properties.setProperty(SonarProperties.VERBOSE_PROPERTY, "true");
    }
    return outputFile;
  }

  @SuppressWarnings("javadoc")
  public IStatus runAnalysis(final IProject project, final Properties props, final boolean debugEnabled, final IProgressMonitor monitor)
    throws CoreException, IOException {

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
          @Override
          public void consumeLine(final String text) {
            SonarCorePlugin.getDefault().info(text + "\n");
          }
        })
        .setStdErr(new StreamConsumer() {
          @Override
          public void consumeLine(final String text) {
            SonarCorePlugin.getDefault().error(text + "\n");
          }
        })
        .execute();

      return checkCancel(monitor);
    } catch (final Exception e) {
      return handleException(monitor, e);
    }

  }

  private IStatus checkCancel(final IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }

  private IStatus handleException(final IProgressMonitor monitor, final Exception e) {
    if (monitor.isCanceled()) {
      // On OSX it seems that cancelling produce an exception
      return Status.CANCEL_STATUS;
    }
    return new Status(IStatus.ERROR, SonarCorePlugin.PLUGIN_ID, "Error during execution of Sonar", e);
  }

  private static String propsToString(final Properties props) {
    final StringBuilder builder = new StringBuilder();
    for (final Object key : props.keySet()) {
      builder.append(key).append("=").append(props.getProperty(key.toString())).append("\n");
    }
    return builder.toString();
  }

}
