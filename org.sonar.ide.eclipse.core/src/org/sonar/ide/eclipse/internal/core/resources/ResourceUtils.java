/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.ide.eclipse.internal.core.resources;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.ResourceResolver;

import java.util.ArrayList;
import java.util.List;

public final class ResourceUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceUtils.class);

  private ResourceUtils() {
  }

  private static List<ResourceResolver> resolvers;

  public synchronized static String getSonarKey(IResource resource, IProgressMonitor monitor) {
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

    for (ResourceResolver resolver : resolvers) {
      String sonarKey = resolver.resolve(resource, monitor);
      if (sonarKey != null) {
        return sonarKey;
      }
    }
    return null;
  }

}
