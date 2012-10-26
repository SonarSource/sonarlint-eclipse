/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.batch;

import org.sonar.batch.bootstrapper.ProjectDefinition;
import org.sonar.batch.components.EmbedderIndex;

public class SonarEclipseRuntime {

  private final org.sonar.api.Plugin[] plugins;

  private GlobalModule globalModule;

  public SonarEclipseRuntime(org.sonar.api.Plugin[] plugins) {
    this.plugins = plugins;
  }

  public void analyse(ProjectDefinition projectDefinition) {
    Module projectModule = globalModule.installChild(new ProjectModule(projectDefinition))
        .install(EmbeddedSonarPlugin.getDefault().getSonarCustomizer());
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

  public EmbedderIndex getIndex() {
    return globalModule.getComponent(EmbedderIndex.class);
  }

}
