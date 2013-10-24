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
package org.sonar.ide.eclipse.core.configurator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonar.ide.eclipse.core.internal.SonarProperties;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;

import java.util.Properties;
import java.util.Set;

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

  protected String getAbsolutePath(IPath path) {
    return ResourceUtils.getAbsolutePath(path);
  }

  protected void appendProperty(Properties properties, String key, String value) {
    String newValue = properties.getProperty(key, null);
    if (newValue != null) {
      newValue += SonarProperties.SEPARATOR + value;
    }
    else {
      newValue = value;
    }
    properties.put(key, newValue);
  }

  protected String concatenate(Set<String> sonarLibraries) {
  	StringBuilder sb = new StringBuilder();
  	boolean first = true;
  	for (String lib : sonarLibraries) {
  		if(!first) {
  			sb.append(SonarProperties.SEPARATOR);
  		}
  		first = false;
  		sb.append(lib);
  	}
  	return sb.toString();
  }

}
