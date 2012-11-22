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
package org.sonar.ide.eclipse.internal.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.sonar.ide.eclipse.core.ISonarFile;
import org.sonar.ide.eclipse.core.ISonarProject;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.core.ResourceUtils;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.internal.core.resources.SonarProject;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.wsclient.services.Resource;

/**
 * Adapter factory for Java elements.
 */
@SuppressWarnings("rawtypes")
public class SonarElementsAdapterFactory implements IAdapterFactory {

  private static final Class<?>[] ADAPTER_LIST = {ISonarResource.class, ISonarFile.class, ISonarProject.class, Resource.class};

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (ISonarResource.class.equals(adapterType) || ISonarFile.class.equals(adapterType) || ISonarProject.class.equals(adapterType)) {
      return getSonarResource(adaptableObject);
    }

    if (adapterType == Resource.class) {
      IProject project;
      if (adaptableObject instanceof IProject) {
        project = (IProject) adaptableObject;
      }
      else {
        project = AdapterUtils.adapt(adaptableObject, IProject.class);
      }
      if (project != null) {
        String key = SonarProject.getInstance(project).getKey();
        return new Resource().setKey(key);
      }
    }
    return null;
  }

  private ISonarResource getSonarResource(Object adaptableObject) {
    // Projects
    IProject project = null;
    if (adaptableObject instanceof IProject) {
      project = (IProject) adaptableObject;
    }
    else {
      project = AdapterUtils.adapt(adaptableObject, IProject.class);
    }
    if (project != null && isConfigured(project)) {
      return SonarProject.getInstance(project);
    }

    // File
    IFile file = null;
    if (adaptableObject instanceof IFile) {
      file = (IFile) adaptableObject;
    }
    else {
      file = AdapterUtils.adapt(adaptableObject, IFile.class);
    }
    if (file != null) {
      IProject parentProject = file.getProject();
      if (!isConfigured(parentProject)) {
        return null;
      }
      ISonarProject sonarProject = SonarProject.getInstance(parentProject);
      String keyWithoutProject = ResourceUtils.getSonarKey(file);
      if (keyWithoutProject != null) {
        return SonarCorePlugin.createSonarFile(file, SonarKeyUtils.resourceKey(sonarProject, keyWithoutProject), file.getName());
      }
    }

    // Other resources like folder and packages
    IResource resource = null;
    if (adaptableObject instanceof IResource) {
      resource = (IResource) adaptableObject;
    }
    else {
      resource = AdapterUtils.adapt(adaptableObject, IResource.class);
    }
    if (resource != null) {
      IProject parentProject = resource.getProject();
      if (!isConfigured(parentProject)) {
        return null;
      }
      ISonarProject sonarProject = SonarProject.getInstance(parentProject);
      String keyWithoutProject = ResourceUtils.getSonarKey(resource);
      if (keyWithoutProject != null) {
        if (resource instanceof IFile) {
          return SonarCorePlugin.createSonarFile((IFile) resource, SonarKeyUtils.resourceKey(sonarProject, keyWithoutProject), resource.getName());
        }
        return SonarCorePlugin.createSonarResource(resource, SonarKeyUtils.resourceKey(sonarProject, keyWithoutProject), resource.getName());
      }
    }
    return null;
  }

  private boolean isConfigured(IProject project) {
    return project.isAccessible() && SonarUiPlugin.hasSonarNature(project);
  }

  public Class[] getAdapterList() {
    return ADAPTER_LIST;
  }
}
