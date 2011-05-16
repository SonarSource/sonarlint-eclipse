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
package org.sonar.ide.eclipse.core.jobs;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.events.DecoratorExecutionHandler;
import org.sonar.api.batch.events.SensorExecutionHandler;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.batch.CustomProjectComponentsModule;
import org.sonar.batch.EmbeddedSonarPlugin;
import org.sonar.batch.SonarEclipseRuntime;
import org.sonar.batch.bootstrapper.ProjectDefinition;
import org.sonar.batch.components.EmbedderIndex;
import org.sonar.ide.eclipse.core.ISonarMeasure;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.core.SonarEclipseException;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurator;
import org.sonar.ide.eclipse.internal.core.Messages;
import org.sonar.ide.eclipse.internal.core.markers.MarkerUtils;
import org.sonar.ide.eclipse.internal.core.resources.ResourceUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class AnalyseProjectJob extends Job {

  private static final Logger LOG = LoggerFactory.getLogger(AnalyseProjectJob.class);

  private IProject project;

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
    Properties properties = new Properties();
    ProjectDefinition sonarProject = new ProjectDefinition(baseDir, workDir, properties);
    ProjectConfigurationRequest request = new ProjectConfigurationRequest(project, sonarProject);
    for (ProjectConfigurator configurator : getConfigurators()) {
      LOG.debug("Project configurator: {}", configurator);
      configurator.configure(request, monitor);
    }

    // Analyse
    CustomProjectComponentsModule customizer = EmbeddedSonarPlugin.getDefault().getSonarCustomizer();
    customizer.bind(SonarProgressMonitor.class, new SonarProgressMonitor(monitor));
    SonarEclipseRuntime runtime = new SonarEclipseRuntime(EmbeddedSonarPlugin.getDefault().getPlugins());
    runtime.start();
    runtime.analyse(sonarProject);
    final EmbedderIndex index = runtime.getIndex();

    // Create markers and save measures
    try {
      project.accept(new IResourceVisitor() {
        public boolean visit(IResource resource) throws CoreException {
          MarkerUtils.deleteViolationsMarkers(resource);
          String sonarKey = ResourceUtils.getSonarKey(resource, monitor);
          if (sonarKey != null) {
            MarkerUtils.createMarkersForViolations(resource, index.getViolations(sonarKey));

            // Save measures
            List<ISonarMeasure> measures = new ArrayList<ISonarMeasure>();
            ISonarResource sonarResource = SonarCorePlugin.createSonarResource(resource, sonarKey, resource.getName());

            for (Measure measure : index.getMeasures(sonarKey)) {
              if (!measure.getMetric().isHidden() && measure.getMetric().isNumericType()) {
                org.sonar.wsclient.services.Metric metricModel = new org.sonar.wsclient.services.Metric();

                Metric metric = measure.getMetric();
                metricModel.setKey(metric.getKey());
                metricModel.setName(metric.getName());
                metricModel.setDescription(metric.getDescription());
                metricModel.setDomain(metric.getDomain());

                org.sonar.wsclient.services.Measure measureModel = new org.sonar.wsclient.services.Measure();
                measureModel.setMetricKey(measure.getMetricKey());
                measureModel.setMetricName(measure.getMetric().getName());
                measureModel.setFormattedValue(Double.toString(measure.getValue()));

                measures.add(SonarCorePlugin.createSonarMeasure(sonarResource, metricModel, measureModel));
              }
            }
            resource.setSessionProperty(SonarCorePlugin.SESSION_PROPERY_MEASURES, measures);
          }
          // don't go deeper than file
          return resource instanceof IFile ? false : true;
        }
      });
    } catch (CoreException e) {
      LOG.error(e.getMessage(), e);
    }

    runtime.stop();

    monitor.done();
    return Status.OK_STATUS;
  }

  private static class SonarProgressMonitor implements SensorExecutionHandler, DecoratorExecutionHandler {
    private IProgressMonitor monitor;

    public SonarProgressMonitor(IProgressMonitor monitor) {
      this.monitor = monitor;
    }

    public void onSensorExecution(SensorExecutionEvent event) {
      checkCanceled();
      if (event.isStart()) {
        monitor.subTask(NLS.bind(Messages.AnalyseProjectJob_sutask_sensor, event.getSensor()));
      }
    }

    public void onDecoratorExecution(DecoratorExecutionEvent event) {
      checkCanceled();
      if (event.isStart()) {
        monitor.subTask(NLS.bind(Messages.AnalyseProjectJob_sutask_decorator, event.getDecorator()));
      }
    }

    private void checkCanceled() {
      if (monitor.isCanceled()) {
        throw new SonarEclipseException("Interrupted");
      }
    }
  }

}
