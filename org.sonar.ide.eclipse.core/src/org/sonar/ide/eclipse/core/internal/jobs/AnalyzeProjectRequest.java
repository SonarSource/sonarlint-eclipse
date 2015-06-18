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

import org.eclipse.core.resources.IProject;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;

import java.util.Collections;
import java.util.List;

public class AnalyzeProjectRequest {

  private IProject project;
  private boolean debugEnabled;
  private List<SonarProperty> extraProps = Collections.emptyList();
  private String jvmArgs = "";
  private boolean forceFullPreview;

  public AnalyzeProjectRequest(IProject project) {
    this.project = project;
  }

  public IProject getProject() {
    return project;
  }

  public boolean isDebugEnabled() {
    return debugEnabled;
  }

  public AnalyzeProjectRequest setDebugEnabled(boolean debugEnabled) {
    this.debugEnabled = debugEnabled;
    return this;
  }

  public List<SonarProperty> getExtraProps() {
    return extraProps;
  }

  public AnalyzeProjectRequest setExtraProps(List<SonarProperty> extraProps) {
    this.extraProps = extraProps;
    return this;
  }

  public String getJvmArgs() {
    return jvmArgs;
  }

  public AnalyzeProjectRequest setJvmArgs(String jvmArgs) {
    this.jvmArgs = jvmArgs;
    return this;
  }

  public boolean isForceFullPreview() {
    return forceFullPreview;
  }

  public AnalyzeProjectRequest setForceFullPreview(boolean forceFullPreview) {
    this.forceFullPreview = forceFullPreview;
    return this;
  }

}
