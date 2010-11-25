/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

package org.sonar.ide.eclipse.internal.ui.jobs;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.eclipse.ui.util.PlatformUtils;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.Violation;

import com.google.common.collect.ArrayListMultimap;

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
      // TODO We will work only with Java projects
      if ( !project.hasNature("org.eclipse.jdt.core.javanature")) {
        return false;
      }

      EclipseSonar sonar = EclipseSonar.getInstance(project);
      SourceCode sourceCode = sonar.search(project);
      if (sourceCode != null) {
        List<Violation> violations = sourceCode.getViolations2();
        // Split violations by resource
        ArrayListMultimap<String, Violation> mm = ArrayListMultimap.create();
        for (Violation violation : violations) {
          if (violation.getLine() != null) { // TODO violation not associated with line
            mm.put(violation.getResourceKey(), violation);
          }
        }
        // Associate violations with resources
        for (String resourceKey : mm.keySet()) {
          Resource sonarResource = new Resource().setKey(resourceKey);
          // adapt org.sonar.wsclient.services.Resource to IFile
          IFile file = PlatformUtils.adapt(sonarResource, IFile.class);
          if (file != null) {
            cleanMarkers(file);
            for (Violation violation : mm.get(resourceKey)) {
              createMarker(file, violation);
            }
          }
        }
      }
      return false; // do not visit members of this resource
    }
    return true;
  }
}
