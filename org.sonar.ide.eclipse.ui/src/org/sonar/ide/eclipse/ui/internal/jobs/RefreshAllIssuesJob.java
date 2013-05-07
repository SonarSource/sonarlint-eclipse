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
package org.sonar.ide.eclipse.ui.internal.jobs;

import org.sonar.ide.eclipse.core.internal.remote.EclipseSonar;
import org.sonar.ide.eclipse.core.internal.remote.SourceCode;

import com.google.common.collect.ArrayListMultimap;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.sonar.ide.eclipse.common.issues.ISonarIssue;
import org.sonar.ide.eclipse.core.internal.SonarNature;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.markers.SonarMarker;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RefreshAllIssuesJob extends RefreshIssuesJob {

  public static void createAndSchedule() {
    List<IResource> resources = new ArrayList<IResource>();
    Collections.addAll(resources, ResourcesPlugin.getWorkspace().getRoot().getProjects());
    new RefreshAllIssuesJob(resources).schedule();
  }

  public static void createAndSchedule(IResource resource) {
    new RefreshAllIssuesJob(Collections.singletonList(resource)).schedule();
  }

  protected RefreshAllIssuesJob(List<IResource> resources) {
    super(resources);
  }

  @Override
  public boolean visit(final IResource resource) throws CoreException {
    if (resource instanceof IProject) {
      IProject project = (IProject) resource;
      if (!SonarNature.hasSonarNature(project)) {
        return false;
      }

      SonarProject sonarProject = SonarProject.getInstance(project);
      if (sonarProject.isAnalysedLocally()) {
        return false;
      }

      MarkerUtils.deleteIssuesMarkers(project);
      EclipseSonar sonar = EclipseSonar.getInstance(project);
      SourceCode sourceCode = sonar.search(project);
      if (sourceCode != null) {
        doRefreshViolation(sonarProject, sourceCode);
        sonarProject.setLastAnalysisDate(sourceCode.getAnalysisDate());
        sonarProject.save();
      }
      // do not visit members of this resource
      return false;
    }
    return true;
  }

  private void doRefreshViolation(SonarProject sonarProject, SourceCode sourceCode) throws CoreException {
    List<ISonarIssue> issues = sourceCode.getRemoteIssuesRecursively();
    // Split issues by resource
    ArrayListMultimap<String, ISonarIssue> mm = ArrayListMultimap.create();
    for (ISonarIssue issue : issues) {
      mm.put(issue.resourceKey(), issue);
    }
    // Associate issues with resources
    for (String resourceKey : mm.keySet()) {
      IResource eclipseResource = ResourceUtils.findResource(sonarProject, resourceKey);
      if (eclipseResource instanceof IFile) {
        for (ISonarIssue issue : mm.get(resourceKey)) {
          SonarMarker.create(eclipseResource, false, issue);
        }
      }
    }
  }
}
