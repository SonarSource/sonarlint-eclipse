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
package org.sonar.ide.eclipse.core.internal.resources;

import org.sonar.ide.eclipse.core.resources.ISonarResource;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.ResourceResolver;
import org.sonar.ide.eclipse.core.internal.AdapterUtils;
import org.sonar.ide.eclipse.core.internal.SonarKeyUtils;

import java.util.ArrayList;
import java.util.List;

public final class ResourceUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceUtils.class);

  private ResourceUtils() {
  }

  private static List<ResourceResolver> resolvers;

  public static String getSonarResourcePartialKey(IResource resource) {
    for (ResourceResolver resolver : getResolvers()) {
      String sonarKey = resolver.getSonarPartialKey(resource);
      if (sonarKey != null) {
        return sonarKey;
      }
    }
    return null;
  }

  public static ISonarResource adapt(Object eclipseObject) {
    if (eclipseObject == null) {
      return null;
    }
    return AdapterUtils.adapt(eclipseObject, ISonarResource.class);
  }

  public static IResource getResource(String resourceKey) {
    IWorkspace root = ResourcesPlugin.getWorkspace();
    for (IProject project : root.getRoot().getProjects()) {
      if (project.isAccessible()) {
        ISonarProject sonarProject = SonarProject.getInstance(project);
        if (sonarProject != null && resourceKey.startsWith(sonarProject.getKey())) {
          String resourceKeyMinusProjectKey = resourceKey.substring(
              sonarProject.getKey().length() + 1); // +1 because ":"
          String[] parts = StringUtils.split(resourceKeyMinusProjectKey, SonarKeyUtils.PROJECT_DELIMITER);
          String partialResourceKey = parts.length > 0 ? parts[0] : "";
          for (ResourceResolver resolver : getResolvers()) {
            IResource resource = resolver.locate(project, partialResourceKey);
            if (resource != null) {
              return resource;
            }
          }
        }
      }
    }
    return null;
  }

  private synchronized static List<ResourceResolver> getResolvers() {
    if (resolvers == null) {
      resolvers = new ArrayList<ResourceResolver>();
      IExtensionRegistry registry = Platform.getExtensionRegistry();
      IConfigurationElement[] config = registry.getConfigurationElementsFor("org.sonar.ide.eclipse.core.resourceResolvers"); //$NON-NLS-1$
      for (final IConfigurationElement element : config) {
        try {
          Object obj = element.createExecutableExtension(ResourceResolver.ATTR_CLASS);
          resolvers.add((ResourceResolver) obj);
        } catch (CoreException e) {
          LOG.error(e.getMessage(), e);
        }
      }
    }
    return resolvers;
  }

}
