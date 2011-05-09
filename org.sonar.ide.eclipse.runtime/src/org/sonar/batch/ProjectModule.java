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
package org.sonar.batch;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.resources.*;
import org.sonar.api.resources.Project.AnalysisType;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.XMLRuleParser;
import org.sonar.batch.bootstrapper.ProjectDefinition;
import org.sonar.batch.components.*;
import org.sonar.batch.index.DefaultIndex;

public class ProjectModule extends Module {

  private ProjectDefinition projectDefinition;
  private Project project;

  public ProjectModule(ProjectDefinition projectDefinition) {
    this.projectDefinition = projectDefinition;
    this.project = createProject(projectDefinition);
  }

  private Project createProject(ProjectDefinition projectDefinition) {
    Configuration conf = new MapConfiguration(projectDefinition.getProperties());
    Project project = new Project(conf.getString(CoreProperties.PROJECT_KEY_PROPERTY));
    project.setLanguageKey(Java.KEY);
    project.setLanguage(Java.INSTANCE);
    project.setConfiguration(conf);
    return project;
  }

  @Override
  protected void configure() {
    addComponent(projectDefinition);
    addComponent(project);
    addComponent(project.getConfiguration());

    addComponent(BatchExtensionDictionnary.class);

    addComponent(Languages.class);
    addComponent(ViolationFilters.class);

    bind(SensorContext.class, DefaultSensorContext.class);
    bind(DecoratorContext.class, DefaultDecoratorContext.class);
    bind(ProjectFileSystem.class, DefaultProjectFileSystem2.class);
    addComponent(DefaultProjectClasspath.class);

    addComponent(new EmbedderProjectTree(project)); // for DefaultIndex
    bind(SonarIndex.class, DefaultIndex.class);

    bind(ProfileLoader.class, RemoteProfileLoader.class); // for EmbedderProfileProvider
    addAdapter(new ProfileProvider()); // for RuleFinder
    bind(RuleFinder.class, EmbedderRuleFinder.class);

    bind(MetricFinder.class, EmbedderMetricFinder.class);

    addComponent(XMLRuleParser.class);
    addComponent(AnnotationRuleParser.class);
    bind(ServerFileSystem.class, EmbedderFileSystem.class);

    addComponent(EmbedderViolationsDecorator.class); // able to save violations

    // Required for BatchExtensionDictionnary, otherwise it can't pick up formulas
    for (Metric metric : CoreMetrics.getMetrics()) {
      addComponent(metric.getKey(), metric);
    }
  }

  private <T> void bind(Class<T> role, Class<? extends T> impl) {
    addComponent(role, impl);
  }

  @Override
  public Module start() {
    // Prepare project
    project.setFileSystem(getComponent(DefaultProjectFileSystem.class));
    project.setAnalysisType(AnalysisType.STATIC);
    return super.start();
  }

}
