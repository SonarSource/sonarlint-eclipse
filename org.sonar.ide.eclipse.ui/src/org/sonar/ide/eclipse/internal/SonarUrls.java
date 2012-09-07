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
package org.sonar.ide.eclipse.internal;

import org.eclipse.core.resources.IFile;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.internal.core.resources.ProjectProperties;

public class SonarUrls {
  public String resourceUrl(ISonarResource resource) {
    String urlTemplate = urlTemplate(resource);

    String serverUrl = properties(resource).getUrl();
    String key = resource.getKey();

    return String.format(urlTemplate, serverUrl, key);
  }

  private String urlTemplate(ISonarResource resource) {
    if (resource.getResource() instanceof IFile) {
      return "%s/resource/index/%s";
    }
    return "%s/project/index/%s";
  }

  private ProjectProperties properties(ISonarResource resource) {
    return ProjectProperties.getInstance(resource.getProject());
  }
}
