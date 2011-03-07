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

import org.sonar.batch.components.EmbedderIndex;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.MapConfiguration;
import org.sonar.api.CoreProperties;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.batch.InMemoryPomCreator;
import org.sonar.batch.Module;
import org.sonar.batch.bootstrapper.ProjectDefinition;

/**
 * @TODO copied from module sonar-embedder
 */
public class SonarEclipseRuntime {

  private org.sonar.api.Plugin[] plugins;

  private GlobalModule globalModule;

  public SonarEclipseRuntime(org.sonar.api.Plugin[] plugins) {
    this.plugins = plugins;
  }

  public void analyse(ProjectDefinition projectDefinition) {
    Project project = createProject(projectDefinition);

    Module projectModule = globalModule.installChild(new ProjectModule(project))
        .install(new CorePluginModule());
    for (org.sonar.api.Plugin plugin : plugins) {
      projectModule.install(new PluginModule(plugin));
    }
    projectModule.start();

    projectModule.installChild(new ProjectPhasesModule())
        .start();

    projectModule.stop();
    globalModule.uninstallChild(projectModule);
  }

  public void start() {
    globalModule = (GlobalModule) new GlobalModule()
        .init()
        .start();
  }

  public void stop() {
    globalModule.stop();
  }

  private Project createProject(ProjectDefinition projectDefinition) {
    Configuration conf = new MapConfiguration(projectDefinition.getProperties());
    Project project = new Project(conf.getString(CoreProperties.PROJECT_KEY_PROPERTY));
    project.setLanguageKey(Java.KEY);
    project.setLanguage(Java.INSTANCE);
    project.setConfiguration(conf);
    project.setPom(new InMemoryPomCreator(projectDefinition).create());
    return project;
  }

  public EmbedderIndex getIndex() {
    return globalModule.getComponent(EmbedderIndex.class);
  }

}
