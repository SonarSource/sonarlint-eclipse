/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.batch.components;

import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.Logs;
import org.sonar.batch.ViolationFilters;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.phases.DecoratorsExecutor;
import org.sonar.batch.phases.InitializersExecutor;
import org.sonar.batch.phases.SensorsExecutor;

/**
 * Equivalent of {@link org.sonar.batch.phases.Phases}.
 */
public class EmbedderPhases {

  private final Project project;
  private final DefaultIndex index;
  private final RulesProfile rulesProfile;
  private final ViolationFilters violationFilters;
  private final InitializersExecutor initializersExecutor;
  private final SensorsExecutor sensorsExecutor;
  private final DecoratorsExecutor decoratorsExecutor;
  private final SensorContext sensorContext;

  public EmbedderPhases(Project project, DefaultIndex index, RulesProfile rulesProfile, ViolationFilters violationFilters,
      InitializersExecutor initializersExecutor, SensorsExecutor sensorsExecutor, DecoratorsExecutor decoratorsExecutor,
      SensorContext sensorContext) {
    this.project = project;
    this.index = index;
    this.rulesProfile = rulesProfile;
    this.violationFilters = violationFilters;
    this.initializersExecutor = initializersExecutor;
    this.sensorsExecutor = sensorsExecutor;
    this.decoratorsExecutor = decoratorsExecutor;
    this.sensorContext = sensorContext;
  }

  public void start() {
    // Prepare index
    index.setCurrentProject(project, null, violationFilters, rulesProfile);
    // Execute
    initializersExecutor.execute(project);
    sensorsExecutor.execute(project, sensorContext);
    decoratorsExecutor.execute(project);
    Logs.INFO.info("ANALYSIS SUCCESSFUL");
  }

}
