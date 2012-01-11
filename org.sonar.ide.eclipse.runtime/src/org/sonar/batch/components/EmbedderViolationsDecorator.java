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

import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorBarriers;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Violation;

@DependsUpon(DecoratorBarriers.END_OF_VIOLATIONS_GENERATION)
public class EmbedderViolationsDecorator implements Decorator {

  private EmbedderPersistenceManager persistenceManager;

  public EmbedderViolationsDecorator(EmbedderPersistenceManager persistenceManager) {
    this.persistenceManager = persistenceManager;
  }

  public boolean shouldExecuteOnProject(Project project) {
    return true;
  }

  public void decorate(Resource resource, DecoratorContext context) {
    if (context.getViolations().isEmpty()) {
      return;
    }

    for (Violation violation : context.getViolations()) {
      persistenceManager.saveViolation(violation);
    }
  }

}
