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

import java.io.IOException;
import java.util.Collections;

import org.sonar.api.resources.Project;
import org.sonar.batch.ProjectTree;

public class EmbedderProjectTree extends ProjectTree {

  public EmbedderProjectTree(Project project) {
    super(Collections.singletonList(project));
  }

  @Override
  public void start() throws IOException {
  }

  @Override
  public Project getProjectByArtifactId(String artifactId) {
    throw new EmbedderUnsupportedOperationException("part of coupling with Maven - should be removed");
  }

}
