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
package org.sonar.ide.eclipse.erlide.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.erlide.core.internal.services.builder.ErlangNature;
import org.erlide.core.model.util.ErlideUtil;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonar.ide.eclipse.core.configurator.ProjectConfigurator;
import org.sonar.ide.eclipse.core.configurator.SonarConfiguratorProperties;

import java.util.Properties;

public class ErlangProjectConfigurator extends ProjectConfigurator {

  @Override
  public boolean canConfigure(IProject project) {
    return ErlideUtil.hasErlangNature(project);
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    IProject project = request.getProject();
    ErlangNature erlProject = ErlangNature.getErlangNature(project);
    configureErlangProject(erlProject, request.getSonarProjectProperties());
  }

  private void configureErlangProject(ErlangNature erlProject, Properties sonarProjectProperties) {
    sonarProjectProperties.setProperty(SonarConfiguratorProperties.PROJECT_LANGUAGE_PROPERTY, "erlang");
    for (String pathStr : SonarErlIdePlugin.getSourceFolders(erlProject)) {
      IPath path = new Path(pathStr);
      appendProperty(sonarProjectProperties, SonarConfiguratorProperties.SOURCE_DIRS_PROPERTY, getAbsolutePath(path));
    }
  }
}
