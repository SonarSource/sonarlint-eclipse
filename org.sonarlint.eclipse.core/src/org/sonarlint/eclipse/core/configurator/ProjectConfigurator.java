/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.configurator;

import java.util.Collection;
import java.util.Map;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.internal.resources.ResourceUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

/**
 * Implemented by components that configure the analysis on certain environments.
 * For example, we have configurators for C/C++ projects in Eclipse CDT and for Java projects.
 * 
 * Other products also use this interface, so <b>it should be kept stable</b>.
 */
public abstract class ProjectConfigurator {

  public static final String SEPARATOR = ",";

  /**
   * Tell if this project configurator can configure the given project.
   */
  public abstract boolean canConfigure(IProject project);

  /**
   * Configures SonarLint analysis, using information from Eclipse project.
   */
  public abstract void configure(ProjectConfigurationRequest request, IProgressMonitor monitor);

  /**
   * This method is called after analysis is finished
   * @param analysisProperties Properties used during the analysis
   */
  public void analysisComplete(Map<String, String> analysisProperties, IProgressMonitor monitor) {
    // Do nothing by default
  }

  @Override
  public String toString() {
    return getClass().getName();
  }

  protected static String getAbsolutePath(IPath path) {
    IPath absolutePath = ResourceUtils.getAbsolutePath(path);
    return absolutePath != null ? absolutePath.toString() : null;
  }

  public static void appendProperty(Map<String, String> properties, String key, String value) {
    if (value == null) {
      return;
    }
    String newValue = properties.get(key);
    if (newValue != null) {
      newValue += SEPARATOR + value;
    } else {
      newValue = value;
    }
    properties.put(key, newValue);
  }

  protected static String getRelativePath(IPath root, IPath path) {
    IPath absoluteRoot = ResourceUtils.getAbsolutePath(root);
    IPath absolutePath = ResourceUtils.getAbsolutePath(path);
    String relativePath = absolutePath != null ? absolutePath.makeRelativeTo(absoluteRoot).toOSString() : null;
    if ("".equals(relativePath)) {
      relativePath = ".";
    }
    return relativePath;
  }

  public static void setPropertyList(Map<String, String> properties, String key, Collection<String> values) {
    properties.put(key, StringUtils.joinSkipNull(values, SEPARATOR));
  }

  public static void appendPropertyList(Map<String, String> properties, String key, Collection<String> values) {
    appendProperty(properties, key, StringUtils.joinSkipNull(values, SEPARATOR));
  }

}
