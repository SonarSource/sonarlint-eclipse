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

package org.sonar.ide.eclipse.internal.jdt;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.sonar.ide.eclipse.core.ISonarFile;
import org.sonar.ide.eclipse.core.ISonarProject;
import org.sonar.ide.eclipse.core.ISonarResource;
import org.sonar.ide.eclipse.core.SonarCorePlugin;
import org.sonar.ide.eclipse.core.SonarKeyUtils;
import org.sonar.ide.eclipse.core.SonarServer;
import org.sonar.ide.eclipse.ui.SonarUiPlugin;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

/**
 * @author Jérémie Lagarde
 */
public class JarElementsAdapterFactory implements IAdapterFactory {

  private static Class<?>[] ADAPTER_LIST = { ISonarResource.class, ISonarFile.class, ISonarProject.class };

  public Object getAdapter(Object adaptableObject, Class adapterType) {
    if (adaptableObject instanceof IPackageFragmentRoot) {
      IPackageFragmentRoot fragment = (IPackageFragmentRoot) adaptableObject;
      String projectKey = getProjectKey(fragment.getParent());
      String sonarProject = StringUtils.substringBeforeLast(fragment.getElementName(), "-");
      return SonarCorePlugin.createSonarResource(fragment.getJavaProject().getResource(), projectKey, sonarProject);

    } else if (adaptableObject instanceof IClassFile) {
      IClassFile file = (IClassFile) adaptableObject;
      IProject project = file.getJavaProject().getProject();
      String projectKey = getProjectKey(file.getParent());
      String packageName = getPackageName(file.getParent());
      String className = StringUtils.substringBeforeLast(file.getElementName(), "."); //$NON-NLS-1$

      return SonarCorePlugin.createSonarFile(project.getFile(file.getPath()), SonarKeyUtils.classKey(projectKey, packageName, className),
          className);

    }
    return null;
  }

  private String getPackageName(IJavaElement javaElement) {
    String packageName = null;
    if (javaElement instanceof IPackageFragmentRoot) {
      packageName = ""; //$NON-NLS-1$
    } else if (javaElement instanceof IPackageFragment) {
      IPackageFragment packageFragment = (IPackageFragment) javaElement;
      packageName = packageFragment.getElementName();
    }
    return packageName;
  }

  @SuppressWarnings("rawtypes")
  public Class[] getAdapterList() {
    return ADAPTER_LIST;
  }

  // TODO : Just for dev, need to find a better way!
  private static List<Resource> cachedResources = null;

  private String getProjectKey(IJavaElement javaElement) {
    if ( !(javaElement instanceof IPackageFragmentRoot))
      return getProjectKey(javaElement.getParent());
    IPackageFragmentRoot jarElement = (IPackageFragmentRoot) javaElement;
    IProject project = jarElement.getJavaProject().getProject();
    if (project.isAccessible() && jarElement.isArchive()) {
      String sonarProject = StringUtils.substringBeforeLast(jarElement.getElementName(), "-");
      ResourceQuery query = new ResourceQuery().setScopes(Resource.SCOPE_SET).setQualifiers(Resource.QUALIFIER_PROJECT,
          Resource.QUALIFIER_MODULE);
      Collection<SonarServer> servers = SonarCorePlugin.getServersManager().getServers();
      if (cachedResources == null) {
        for (SonarServer sonarServer : servers) {
          try {
            List<Resource> resources = cachedResources;
            Sonar sonar = SonarCorePlugin.getServersManager().getSonar(sonarServer.getUrl());
            resources = sonar.findAll(query);
            if (cachedResources == null) {
              cachedResources = resources;
            } else {
              cachedResources.addAll(resources);
            }
          } catch (Exception e) {
            // TODO: handle exception
          }
        }
      }
      for (Resource resource : cachedResources) {
        if (resource.getKey().endsWith(":" + sonarProject)) {
          return resource.getKey();
        }
      }
    }
    return null;
  }
}
