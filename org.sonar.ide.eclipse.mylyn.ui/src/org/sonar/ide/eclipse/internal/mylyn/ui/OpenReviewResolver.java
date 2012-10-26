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
package org.sonar.ide.eclipse.internal.mylyn.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.core.resources.ProjectProperties;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarConnector;
import org.sonar.ide.eclipse.ui.ISonarResolver;

public class OpenReviewResolver implements ISonarResolver {

  private TaskRepository repository;
  private String reviewId;

  public String getDescription() {
    return "Open review #" + reviewId;
  }

  public String getLabel() {
    return "Open review #" + reviewId;
  }

  public boolean canResolve(IMarker marker) {
    try {
      if (SonarCorePlugin.MARKER_ID.equals(marker.getType())) {
        reviewId = marker.getAttribute("reviewId", "");
        if (!"".equals(reviewId)) {
          String sonarServerUrl = ProjectProperties.getInstance(marker.getResource()).getUrl();
          repository = TasksUi.getRepositoryManager().getRepository(SonarConnector.CONNECTOR_KIND, sonarServerUrl);
          return repository != null;
        }
      }
    } catch (final CoreException e) {
      return false;
    }
    return false;
  }

  public boolean resolve(IMarker marker, IFile file) {
    TasksUiUtil.openTask(repository, reviewId);
    return false;
  }

}
