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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;

/**
 * @deprecated no more used since SonarQube server 4.2
 */
@Deprecated
public abstract class ResourceResolver {

  public static final String ATTR_CLASS = "class"; //$NON-NLS-1$

  /**
   * @return Sonar resource key without project key prefix or null, if this resolver can't determine key
   */
  public abstract String getSonarPartialKey(IResource resource);

  /**
   * Find a resource in a project.
   * @param project
   * @param partialResourceKey The resource key (without the project prefix)
   * @param monitor
   */
  public abstract IResource locate(IProject project, String partialResourceKey);

  protected String getAbsolutePath(IPath path) {
    return ResourceUtils.getAbsolutePath(path);
  }

}
