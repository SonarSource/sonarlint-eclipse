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

package org.sonar.ide.eclipse.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.sonar.ide.eclipse.core.ISonarResource;

public class SonarResource implements ISonarResource {

  private IResource resource;
  private String key;

  public SonarResource(IResource resource, String key) {
    Assert.isNotNull(resource);
    Assert.isNotNull(key);
    this.key = key;
    this.resource = resource;
  }

  public String getKey() {
    return key;
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
