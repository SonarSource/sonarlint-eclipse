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

import org.sonar.batch.components.EmbedderProfileProvider;
import org.sonar.batch.components.Mocks;

import org.sonar.api.batch.BatchExtensionDictionnary;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.DefaultProjectFileSystem;
import org.sonar.api.resources.Languages;
import org.sonar.api.resources.Project;
import org.sonar.batch.DefaultDecoratorContext;
import org.sonar.batch.DefaultSensorContext;
import org.sonar.batch.Module;
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

    addComponent(DefaultSensorContext.class);
    addComponent(DefaultDecoratorContext.class);
    addComponent(DefaultProjectFileSystem.class);
    addComponent(ProjectClasspath.class);

    addComponent(Mocks.createProjectTree(project)); // required for DefaultIndex
    addComponent(DefaultIndex.class);

    addAdapter(new EmbedderProfileProvider());

    // Required for BatchExtensionDictionnary, otherwise it can't pick up formulas
    for (Metric metric : CoreMetrics.getMetrics()) {
      addComponent(metric.getKey(), metric);
    }
  }

  @Override
  public Module start() {
    // Prepare project
    project.setFileSystem(getComponent(DefaultProjectFileSystem.class));
    return super.start();
  }

}
