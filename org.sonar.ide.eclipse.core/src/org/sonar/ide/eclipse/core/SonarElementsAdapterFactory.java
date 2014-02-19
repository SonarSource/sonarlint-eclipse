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
package org.sonar.ide.eclipse.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.AdapterUtils;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.SonarKeyUtils;
import org.sonar.ide.eclipse.core.internal.SonarNature;
import org.sonar.ide.eclipse.core.internal.resources.ISonarProject;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.resources.ISonarFile;
import org.sonar.ide.eclipse.core.resources.ISonarResource;
import org.sonar.ide.eclipse.wsclient.WSClientFactory;

/**
 * Adapter factory for Sonar elements.
 */
@SuppressWarnings("rawtypes")
public class SonarElementsAdapterFactory implements IAdapterFactory {

  private static final Class<?>[] ADAPTER_LIST = {ISonarResource.class, ISonarFile.class};

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (adapterType == ISonarResource.class) {
      return getSonarResource(adaptableObject);
    } else if (adapterType == ISonarFile.class) {
      ISonarResource res = getSonarResource(adaptableObject);
      return (res instanceof ISonarFile) ? res : null;
    }
    return null;
  }

  private ISonarResource getSonarResource(Object adaptableObject) {
    // Projects
    IProject project = AdapterUtils.adapt(adaptableObject, IProject.class);
    if (project != null && isConfigured(project)) {
      return SonarProject.getInstance(project);
    }

    // File && Other resources like folder and packages
    IResource resource = AdapterUtils.adapt(adaptableObject, IFile.class);
    if (resource == null) {
      resource = AdapterUtils.adapt(adaptableObject, IResource.class);
    }
    if (resource != null) {
      IProject parentProject = resource.getProject();
      if (!isConfigured(parentProject)) {
        return null;
      }
      ISonarProject sonarProject = SonarProject.getInstance(parentProject);
      ISonarServer sonarServer = SonarCorePlugin.getServersManager().findServer(sonarProject.getUrl());
      String serverVersion = WSClientFactory.getSonarClient(sonarServer).getServerVersion();
      String keyWithoutProject = ResourceUtils.getSonarResourcePartialKey(resource, serverVersion);
      if (keyWithoutProject != null) {
        return createSonarResource(resource, sonarProject, keyWithoutProject);
      }
    }
    return null;
  }

  private ISonarResource createSonarResource(IResource resource, ISonarProject sonarProject, String keyWithoutProject) {
    if (resource instanceof IFile) {
      return SonarCorePlugin.createSonarFile((IFile) resource, SonarKeyUtils.resourceKey(sonarProject, keyWithoutProject), resource.getName());
    }
    return SonarCorePlugin.createSonarResource(resource, SonarKeyUtils.resourceKey(sonarProject, keyWithoutProject), resource.getName());
  }

  private boolean isConfigured(IProject project) {
    return project.isAccessible() && SonarNature.hasSonarNature(project);
  }

  public Class[] getAdapterList() {
    return ADAPTER_LIST;
  }
}
