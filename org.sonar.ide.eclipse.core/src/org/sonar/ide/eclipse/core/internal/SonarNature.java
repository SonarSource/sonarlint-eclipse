/*
 * SonarQube Eclipse
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
package org.sonar.ide.eclipse.core.internal;

import com.google.common.collect.Lists;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SonarNature implements IProjectNature {

  public static final String NATURE_ID = SonarCorePlugin.PLUGIN_ID + ".sonarNature";

  private IProject project;

  public void configure() throws CoreException {
  }

  public void deconfigure() throws CoreException {
  }

  public IProject getProject() {
    return project;
  }

  public void setProject(IProject project) {
    this.project = project;
  }

  public static boolean hasSonarNature(IProject project) {
    try {
      return project.hasNature(NATURE_ID);
    } catch (CoreException e) {
      LoggerFactory.getLogger(SonarCorePlugin.class).error(e.getMessage(), e);
      return false;
    }
  }

  public static void enableNature(IProject project) throws CoreException {
    IProjectDescription description = project.getDescription();
    String[] prevNatures = description.getNatureIds();
    String[] newNatures = new String[prevNatures.length + 1];
    System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
    newNatures[0] = NATURE_ID;
    description.setNatureIds(newNatures);
    project.setDescription(description, null);
  }

  public static void disableNature(IProject project) throws CoreException {
    project.deleteMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);

    IProjectDescription description = project.getDescription();
    List<String> newNatures = Lists.newArrayList();
    for (String natureId : description.getNatureIds()) {
      if (!NATURE_ID.equals(natureId)) {
        newNatures.add(natureId);
      }
    }
    description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
    project.setDescription(description, null);
  }

}
