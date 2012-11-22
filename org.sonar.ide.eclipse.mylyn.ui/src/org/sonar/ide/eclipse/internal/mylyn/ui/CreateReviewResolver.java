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
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.core.resources.SonarProject;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarConnector;
import org.sonar.ide.eclipse.ui.ISonarResolver;

public class CreateReviewResolver implements ISonarResolver {

  private TaskRepository repository;
  private String rulename;
  private String violationId;

  public String getDescription() {
    return getLabel();
  }

  public String getLabel() {
    return "Create review (" + rulename + ")";
  }

  public boolean canResolve(IMarker marker) {
    try {
      if (SonarCorePlugin.MARKER_ID.equals(marker.getType())) {
        rulename = marker.getAttribute("rulename", "");
        violationId = marker.getAttribute("violationId", "");
        String reviewId = marker.getAttribute("reviewId", "");
        if (!"".equals(violationId) && !"".equals(rulename) && "".equals(reviewId)) {
          String sonarServerUrl = SonarProject.getInstance(marker.getResource()).getUrl();
          repository = TasksUi.getRepositoryManager().getRepository(SonarConnector.CONNECTOR_KIND, sonarServerUrl);
          return repository != null;
        }
      }
    } catch (CoreException e) {
      return false;
    }
    return false;
  }

  public boolean resolve(IMarker marker, IFile file) {
    try {
      SonarMylynUiPlugin.createAndOpen(repository, Long.parseLong(violationId), rulename);
    } catch (CoreException e) {
      // TODO handle
    }
    return false;
  }
}
