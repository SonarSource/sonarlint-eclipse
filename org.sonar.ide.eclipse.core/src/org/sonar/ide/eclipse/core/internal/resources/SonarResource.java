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


import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;

public class SonarResource implements ISonarResource {

  private final IResource resource;
  private final String key;
  private final String name;

  public SonarResource(IResource resource, String key, String name) {
    Assert.isNotNull(resource);
    Assert.isNotNull(key);
    Assert.isNotNull(name);
    this.resource = resource;
    this.key = key;
    this.name = name;
  }

  public String getKey() {
    return key;
  }

  public String getName() {
    return name;
  }

  public IProject getProject() {
    return resource.getProject();
  }

  public IResource getResource() {
    return resource;
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof SonarResource) && (key.equals(((SonarResource) obj).key));
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + " [key=" + key + "]";
  }

}
