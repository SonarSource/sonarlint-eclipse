/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurator;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.PreferencesUtils;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;
import org.sonar.ide.eclipse.core.internal.servers.SonarServer;
import org.sonar.runner.api.Issue;
import org.sonar.runner.api.IssueListener;

public class AnalyzeProjectJob extends Job {

  private final SonarProject sonarProject;

  private List<SonarProperty> extraProps;

  private SonarServer sonarServer;
  private final AnalyzeProjectRequest request;

  private static final ISchedulingRule SONAR_ANALYSIS_RULE = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();

  public AnalyzeProjectJob(AnalyzeProjectRequest request) {
    super(jobTitle(request));
    this.request = request;
    this.extraProps = PreferencesUtils.getExtraPropertiesForLocalAnalysis(request.getProject());
    this.sonarProject = SonarProject.getInstance(request.getProject());
    this.sonarServer = (SonarServer) SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
    setPriority(request.isQuick() ? Job.SHORT : Job.LONG);
    // Prevent concurrent SQ analysis
    setRule(SONAR_ANALYSIS_RULE);
  }

  private static String jobTitle(AnalyzeProjectRequest request) {
    if (request.getOnlyOnFiles() == null) {
      return "SonarQube analysis of project " + request.getProject().getName();
    }
    if (request.getOnlyOnFiles().size() == 1) {
      return "SonarQube analysis of file " + request.getOnlyOnFiles().iterator().next().getProjectRelativePath().toString() + "(Project " + request.getProject().getName() + ")";
    }
    return "SonarQube analysis of project " + request.getProject().getName() + " (" + request.getOnlyOnFiles().size() + " files)";
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    // Verify Host
    if (getSonarServer() == null) {
      SonarCorePlugin.getDefault().error(NLS.bind(Messages.No_matching_server_in_configuration_for_project, request.getProject().getName(), sonarProject.getUrl()) + "\n");
      return Status.OK_STATUS;
    }
    // Verify version and server is reachable
    if (getSonarServer().disabled()) {
      SonarCorePlugin.getDefault().info("SonarQube server " + sonarProject.getUrl() + " is disabled");
      return Status.OK_STATUS;
    }

    // Configure
    Properties properties = new Properties();
    File outputFile = configureAnalysis(monitor, properties, extraProps);

    // Analyze
    // To be sure to not reuse something from a previous analysis
    try {
      Files.deleteIfExists(outputFile.toPath());
    } catch (IOException e) {
      return new Status(Status.WARNING, SonarCorePlugin.PLUGIN_ID, "Unable to delete", e);
    }
    long start = System.currentTimeMillis();
    SonarCorePlugin.getDefault().info("Start SonarQube analysis on " + request.getProject().getName() + "...\n");
    try {
      run(request.getProject(), properties, request.isDebugEnabled(), monitor);
    } catch (Exception e) {
      SonarCorePlugin.getDefault().error("Error during execution of SonarQube analysis", e);
      return new Status(Status.WARNING, SonarCorePlugin.PLUGIN_ID, "Error when executing SonarQube analysis", e);
    }
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    SonarCorePlugin.getDefault().debug("Done in " + (System.currentTimeMillis() - start) + "ms\n");

    // Create markers
    long startMarker = System.currentTimeMillis();
    try {
      SonarCorePlugin.getDefault().debug("Create markers on project " + request.getProject().getName() + " resources...\n");
      createMarkersFromReportOutput(monitor, outputFile);
    } catch (Exception e) {
      SonarCorePlugin.getDefault().error("Error during creation of markers", e);
      return new Status(Status.WARNING, SonarCorePlugin.PLUGIN_ID, "Error during creation of markers", e);
    }
    SonarCorePlugin.getDefault().debug("Done in " + (System.currentTimeMillis() - startMarker) + "ms\n");
    return Status.OK_STATUS;
  }

  private String getServerVersion() {
    return sonarServer.getVersion();
  }

  private ISonarServer getSonarServer() {
    return sonarServer;
  }

  @VisibleForTesting
  public void createMarkersFromReportOutput(final IProgressMonitor monitor, File outputFile) throws Exception {
    try (FileReader fileReader = new FileReader(outputFile)) {
      Object obj = JSONValue.parse(fileReader);
      JSONObject sonarResult = (JSONObject) obj;
      // Start by resolving all components in a cache
      Map<String, IResource> resourcesByKey = Maps.newHashMap();
      final JSONArray components = (JSONArray) sonarResult.get("components");
      for (Object component : components) {
        String key = ObjectUtils.toString(((JSONObject) component).get("key"));
        String status = ObjectUtils.toString(((JSONObject) component).get("status"));
        IResource resource = ResourceUtils.findResource(sonarProject, key);
        if (resource != null) {
          resourcesByKey.put(key, resource);
          // Status is blank for modules
          if (StringUtils.isNotBlank(status)) {
            MarkerUtils.deleteIssuesMarkers(resource);
          }
        }
      }
      // Now read all rules name in a cache
      Map<String, String> ruleByKey = readRules(sonarResult);
      // Now read all users name in a cache
      Map<String, String> userNameByLogin = readUserNameByLogin(sonarResult);
      // Now iterate over all issues and create markers
      MarkerUtils.createMarkersForJSONIssues(resourcesByKey, ruleByKey, userNameByLogin, (JSONArray) sonarResult.get("issues"));
    }
  }

  private static Map<String, String> readRules(JSONObject sonarResult) {
    Map<String, String> ruleByKey = Maps.newHashMap();
    final JSONArray rules = (JSONArray) sonarResult.get("rules");
    for (Object rule : rules) {
      String key = ObjectUtils.toString(((JSONObject) rule).get("key"));
      String name = ObjectUtils.toString(((JSONObject) rule).get("name"));
      ruleByKey.put(key, name);
    }
    return ruleByKey;
  }

  private static Map<String, String> readUserNameByLogin(JSONObject sonarResult) {
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
    IProject project = request.getProject();
    File baseDir = project.getLocation().toFile();
    IPath projectSpecificWorkDir = project.getWorkingLocation(SonarCorePlugin.PLUGIN_ID);
    File outputFile = new File(projectSpecificWorkDir.toFile(), "sonar-report.json");

    // Preview mode by default
    properties.setProperty(SonarProperties.ANALYSIS_MODE, request.isQuick() ? SonarProperties.ANALYSIS_MODE_QUICK : SonarProperties.ANALYSIS_MODE_PREVIEW);
    properties.setProperty(SonarProperties.ENABLE_OFFLINE_PROPERTY, "true");

    // Configuration by configurators (common and language specific)
    ConfiguratorUtils.configure(project, properties, getServerVersion(), monitor);

    // Append workspace and project properties
    for (SonarProperty sonarProperty : extraProps) {
      properties.put(sonarProperty.getName(), sonarProperty.getValue());
    }
    System.out.println("Foo");

    if (this.request.getOnlyOnFiles() != null) {
      Collection<String> paths = Collections2.transform(request.getOnlyOnFiles(), new Function<IFile, String>() {
        public String apply(IFile file) {
          return file.getProjectRelativePath().toString();
        };
      });
      ProjectConfigurator.setPropertyList(properties, "sonar.tests", paths);
      ProjectConfigurator.setPropertyList(properties, "sonar.sources", paths);
    }

    properties.setProperty(SonarProperties.PROJECT_BASEDIR, baseDir.toString());
    properties.setProperty(SonarProperties.WORK_DIR, projectSpecificWorkDir.toString());

    // Output file is relative to working dir
    properties.setProperty(SonarProperties.REPORT_OUTPUT_PROPERTY, outputFile.getName());
    if (request.isDebugEnabled()) {
      properties.setProperty(SonarProperties.VERBOSE_PROPERTY, "true");
    }
    return outputFile;
  }

  public void run(IProject project, Properties props, boolean debugEnabled, final IProgressMonitor monitor) {
    if (debugEnabled) {
      SonarCorePlugin.getDefault().info("Start sonar-runner with args:\n" + propsToString(props));
    }
    sonarServer.startAnalysis(props, debugEnabled, new IssueListener() {

      @Override
      public void handle(Issue issue) {
        // TODO
      }
    });
  }

  private static String propsToString(Properties props) {
    StringBuilder builder = new StringBuilder();
    for (Object key : props.keySet()) {
      builder.append(key).append("=").append(props.getProperty(key.toString())).append("\n");
    }
    return builder.toString();
  }

}
