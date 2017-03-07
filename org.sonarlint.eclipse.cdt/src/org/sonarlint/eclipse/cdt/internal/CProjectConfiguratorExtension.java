/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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
package org.sonarlint.eclipse.cdt.internal;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;

/**
 * Responsible for checking at runtime if CDT plugin is installed.
 */
public class CProjectConfiguratorExtension extends ProjectConfigurator {

  private final CProjectConfigurator cProjectConfigurator;

  public CProjectConfiguratorExtension() {
    cProjectConfigurator = isCdtPresent() ? new CProjectConfigurator() : null;
  }

  private static boolean isCdtPresent() {
    try {
      Class.forName("org.eclipse.cdt.core.CCorePlugin");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean canConfigure(IProject project) {
    try {
      // Constants are inlined so this is not causing ClassNotFound
      return cProjectConfigurator != null && (project.hasNature(CProjectNature.C_NATURE_ID) || project.hasNature(CCProjectNature.CC_NATURE_ID));
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
      return false;
    }
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    cProjectConfigurator.configure(request, monitor);
  }

}
