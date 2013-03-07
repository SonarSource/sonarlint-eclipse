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
package org.sonar.ide.eclipse.ui.internal.jobs;

import com.google.common.collect.ArrayListMultimap;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.core.internal.SonarNature;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.ui.internal.EclipseSonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.Violation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RefreshAllViolationsJob extends RefreshViolationsJob {

  public static void createAndSchedule() {
    List<IResource> resources = new ArrayList<IResource>();
    Collections.addAll(resources, ResourcesPlugin.getWorkspace().getRoot().getProjects());
    new RefreshAllViolationsJob(resources).schedule();
  }

  public static void createAndSchedule(IResource resource) {
    new RefreshAllViolationsJob(Collections.singletonList(resource)).schedule();
  }

  protected RefreshAllViolationsJob(List<IResource> resources) {
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

      MarkerUtils.deleteViolationsMarkers(project);
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
    List<Violation> violations = sourceCode.getViolations2();
    // Split violations by resource
    ArrayListMultimap<String, Violation> mm = ArrayListMultimap.create();
    for (Violation violation : violations) {
      mm.put(violation.getResourceKey(), violation);
    }
    // Associate violations with resources
    for (String resourceKey : mm.keySet()) {
      Resource sonarResource = new Resource().setKey(resourceKey);
      IResource eclipseResource = ResourceUtils.findResource(sonarProject, sonarResource.getKey());
      if (eclipseResource instanceof IFile) {
        for (Violation violation : mm.get(resourceKey)) {
          MarkerUtils.createMarkerForWSViolation(eclipseResource, violation, false);
        }
      }
    }
  }
}
