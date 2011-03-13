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

import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.platform.ServerFileSystem;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Project.AnalysisType;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.rules.AnnotationRuleParser;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.XMLRuleParser;
import org.sonar.batch.components.EmbedderFileSystem;
import org.sonar.batch.components.EmbedderMetricFinder;
import org.sonar.batch.components.EmbedderProjectTree;
import org.sonar.batch.components.EmbedderRuleFinder;
import org.sonar.batch.components.EmbedderViolationsDecorator;
import org.sonar.batch.components.RemoteProfileLoader;
import org.sonar.batch.index.DefaultIndex;

public class ProjectModule extends Module {

  private Project project;

  public ProjectModule(Project project) {
    this.project = project;
  }

  @Override
  protected void configure() {
    addComponent(project);
    addComponent(project.getPom());
    addComponent(project.getConfiguration());

    addComponent(BatchExtensionDictionnary.class);

    addComponent(Languages.class);

    bind(SensorContext.class, DefaultSensorContext.class);
    bind(DecoratorContext.class, DefaultDecoratorContext.class);
    bind(ProjectFileSystem.class, DefaultProjectFileSystem.class);
    addComponent(ProjectClasspath.class);

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
