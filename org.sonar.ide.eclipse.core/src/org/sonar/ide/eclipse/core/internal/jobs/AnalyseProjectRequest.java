/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
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
package org.sonar.ide.eclipse.core.internal.jobs;

import java.util.Collections;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;

public class AnalyseProjectRequest {

  private final IProject project;
  private boolean debugEnabled;
  private List<SonarProperty> extraProps = Collections.emptyList();
  private String jvmArgs = "";
  private boolean forceFullPreview;

  public AnalyseProjectRequest(final IProject project) {
    this.project = project;
  }

  public IProject getProject() {
    return project;
  }

  public boolean isDebugEnabled() {
    return debugEnabled;
  }

  public AnalyseProjectRequest setDebugEnabled(final boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
    return this;
  }

  public List<SonarProperty> getExtraProps() {
    return extraProps;
  }

  public AnalyseProjectRequest setExtraProps(final List<SonarProperty> extraProps) {
    this.extraProps = extraProps;
    return this;
  }

  public String getJvmArgs() {
    return jvmArgs;
  }

  public AnalyseProjectRequest setJvmArgs(final String jvmArgs) {
    this.jvmArgs = jvmArgs;
    return this;
  }

  public boolean isForceFullPreview() {
    return forceFullPreview;
  }

  public AnalyseProjectRequest setForceFullPreview(final boolean forceFullPreview) {
    this.forceFullPreview = forceFullPreview;
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (debugEnabled ? 1231 : 1237);
    result = prime * result + (extraProps == null ? 0 : extraProps.hashCode());
    result = prime * result + (forceFullPreview ? 1231 : 1237);
    result = prime * result + (jvmArgs == null ? 0 : jvmArgs.hashCode());
    result = prime * result + (project == null ? 0 : project.hashCode());
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof AnalyseProjectRequest)) {
      return false;
    }
    final AnalyseProjectRequest other = (AnalyseProjectRequest) obj;
    if (debugEnabled != other.debugEnabled) {
      return false;
    }
    if (extraProps == null) {
      if (other.extraProps != null) {
        return false;
      }
    } else if (!extraProps.equals(other.extraProps)) {
      return false;
    }
    if (forceFullPreview != other.forceFullPreview) {
      return false;
    }
    if (jvmArgs == null) {
      if (other.jvmArgs != null) {
        return false;
      }
    } else if (!jvmArgs.equals(other.jvmArgs)) {
      return false;
    }
    if (project == null) {
      if (other.project != null) {
        return false;
      }
    } else if (!project.getName().equals(other.project.getName())) {
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("nls")
  @Override
  public String toString() {
    return "AnalyseProjectRequest [project=" + project + ", debugEnabled=" + debugEnabled + ", forceFullPreview=" + forceFullPreview + "]";
  }

}
