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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.sonar.runner.api.Issue;
import org.sonar.runner.api.IssueListener;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.configurator.ConfiguratorUtils;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.markers.SonarMarker;
import org.sonarlint.eclipse.core.internal.resources.ResourceUtils;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;

public class AnalyzeProjectJob extends AbstractSonarProjectJob {

  private List<SonarLintProperty> extraProps;

  private final AnalyzeProjectRequest request;

  static final ISchedulingRule SONAR_ANALYSIS_RULE = ResourcesPlugin.getWorkspace().getRuleFactory().buildRule();

  public AnalyzeProjectJob(AnalyzeProjectRequest request) {
    super(jobTitle(request), SonarLintProject.getInstance(request.getProject()));
    this.request = request;
    this.extraProps = PreferencesUtils.getExtraPropertiesForLocalAnalysis(request.getProject());
  }

  private static String jobTitle(AnalyzeProjectRequest request) {
    if (request.getOnlyOnFiles() == null) {
      return "SonarLint analysis of project " + request.getProject().getName();
    }
    if (request.getOnlyOnFiles().size() == 1) {
      return "SonarLint analysis of file " + request.getOnlyOnFiles().iterator().next().getProjectRelativePath().toString() + "(Project " + request.getProject().getName() + ")";
    }
    return "SonarLint analysis of project " + request.getProject().getName() + " (" + request.getOnlyOnFiles().size() + " files)";
  }

  @Override
  protected IStatus run(SonarRunnerFacade runner, final IProgressMonitor monitor) {
    // Configure
    Properties properties = configureAnalysis(monitor, extraProps);

    // Analyze
    // To be sure to not reuse something from a previous analysis
    try {
      run(request.getProject(), properties, runner, monitor);
    } catch (Exception e) {
      SonarLintCorePlugin.getDefault().error("Error during execution of SonarLint analysis" + System.lineSeparator(), e);
      return new Status(Status.WARNING, SonarLintCorePlugin.PLUGIN_ID, "Error when executing SonarLint analysis", e);
    }
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }

    return Status.OK_STATUS;
  }

  /**
   * Populate properties with everything required for the SonarLint analysis in issues mode.
   * @param monitor
   * @param properties
   * @return
   */
  @VisibleForTesting
  public Properties configureAnalysis(final IProgressMonitor monitor, List<SonarLintProperty> extraProps) {
    Properties properties = new Properties();
    IProject project = request.getProject();
    File baseDir = project.getLocation().toFile();
    IPath projectSpecificWorkDir = project.getWorkingLocation(SonarLintCorePlugin.PLUGIN_ID);

    // Preview mode by default
    properties.setProperty(SonarLintProperties.ANALYSIS_MODE, SonarLintProperties.ANALYSIS_MODE_ISSUES);

    // Configuration by configurators (common and language specific)
    ConfiguratorUtils.configure(project, this.request.getOnlyOnFiles(), properties, monitor);

    // Append workspace and project properties
    for (SonarLintProperty sonarProperty : extraProps) {
      properties.put(sonarProperty.getName(), sonarProperty.getValue());
    }
    if (this.request.getOnlyOnFiles() != null) {
      Collection<String> paths = Collections2.transform(request.getOnlyOnFiles(), new Function<IFile, String>() {
        @Override
        public String apply(IFile file) {
          MarkerUtils.deleteIssuesMarkers(file);
          return file.getProjectRelativePath().toString();
        }
      });
      ProjectConfigurator.setPropertyList(properties, "sonar.tests", paths);
      ProjectConfigurator.setPropertyList(properties, "sonar.sources", paths);
    } else {
      MarkerUtils.deleteIssuesMarkers(project);
    }

    properties.setProperty(SonarLintProperties.PROJECT_BASEDIR, baseDir.toString());
    properties.setProperty(SonarLintProperties.WORK_DIR, projectSpecificWorkDir.toString());

    return properties;
  }

  public void run(IProject project, final Properties props, final SonarRunnerFacade runner, final IProgressMonitor monitor) {
    if (SonarLintCorePlugin.getDefault().isDebugEnabled()) {
      SonarLintCorePlugin.getDefault().info("Start sonar-runner with args:\n" + propsToString(props) + System.lineSeparator());
    }
    Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread th, Throwable ex) {
        SonarLintCorePlugin.getDefault().error("Error during analysis", ex);
      }
    };
    Thread t = new Thread("SonarLint analysis") {
      @Override
      public void run() {
        runner.startAnalysis(props, new IssueListener() {

          @Override
          public void handle(Issue issue) {
            IResource r = ResourceUtils.findResource(request.getProject(), issue.getComponentKey());
            if (request.getOnlyOnFiles() == null || request.getOnlyOnFiles().contains(r)) {
              try {
                SonarMarker.create(r, issue);
              } catch (CoreException e) {
                SonarLintCorePlugin.getDefault().error(e.getMessage(), e);
              }
            }
          }
        });
      }
    };
    t.setDaemon(true);
    t.setUncaughtExceptionHandler(h);
    t.start();
    while (t.isAlive()) {
      if (monitor.isCanceled()) {
        t.interrupt();
        try {
          t.join(5000);
        } catch (InterruptedException e) {
          // just quit
        }
        if (t.isAlive()) {
          SonarLintCorePlugin.getDefault().error("Unable to properly terminate SonarLint analysis");
        }
        break;
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        // Here we don't care
      }
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
