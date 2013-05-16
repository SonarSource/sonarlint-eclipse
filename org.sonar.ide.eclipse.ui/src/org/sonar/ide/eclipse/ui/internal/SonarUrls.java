/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
package org.sonar.ide.eclipse.ui.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.resources.ISonarResource;

public class SonarUrls {
  public String resourceUrl(ISonarResource resource) {
    String urlTemplate = resourcesUrlTemplate(resource);

    String serverUrl = properties(resource).getUrl();
    String key = resource.getKey();

    return String.format(urlTemplate, serverUrl, key);
  }

  public String issueUrl(String issueId, IResource resource) {
    String urlTemplate = "%s/issue/show/%s";

    String serverUrl = SonarProject.getInstance(resource.getProject()).getUrl();

    return String.format(urlTemplate, serverUrl, issueId);
  }

  private String resourcesUrlTemplate(ISonarResource resource) {
    if (resource.getResource() instanceof IFile) {
      return "%s/resource/index/%s";
    }
    return "%s/project/index/%s";
  }

  private SonarProject properties(ISonarResource resource) {
    return SonarProject.getInstance(resource.getProject());
  }
}
