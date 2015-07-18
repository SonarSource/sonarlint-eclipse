/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.ide.eclipse.core.configurator;

import java.util.Collection;
import java.util.Properties;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;

public abstract class ProjectConfigurator {

  /**
   * Tell if this project configurator can configure the given project. It is already assumed
   * that the project has the SonarQube nature.
   */
  public abstract boolean canConfigure(IProject project);

  /**
   * Configures SonarQube project, using information from Eclipse project.
   */
  public abstract void configure(ProjectConfigurationRequest request, IProgressMonitor monitor);

  @Override
  public String toString() {
    return getClass().getName();
  }

  @CheckForNull
  protected static String getAbsolutePath(IPath path) {
    IPath absolutePath = ResourceUtils.getAbsolutePath(path);
    return absolutePath != null ? absolutePath.toString() : null;
  }

  public static void appendProperty(Properties properties, String key, @Nullable String value) {
    if (value == null) {
      return;
    }
    String newValue = properties.getProperty(key, null);
    if (newValue != null) {
      newValue += SonarProperties.SEPARATOR + value;
    } else {
      newValue = value;
    }
    properties.put(key, newValue);
  }

  @CheckForNull
  protected String getRelativePath(IPath root, IPath path) {
    IPath absoluteRoot = ResourceUtils.getAbsolutePath(root);
    IPath absolutePath = ResourceUtils.getAbsolutePath(path);
    return absolutePath != null ? absolutePath.makeRelativeTo(absoluteRoot).toOSString() : null;
  }

  protected static void setPropertyList(Properties properties, String key, Collection<String> values) {
    properties.put(key, StringUtils.join(values, SonarProperties.SEPARATOR));
  }

  public static void appendPropertyList(Properties properties, String key, Collection<String> values) {
    appendProperty(properties, key, StringUtils.join(values, SonarProperties.SEPARATOR));
  }
}
