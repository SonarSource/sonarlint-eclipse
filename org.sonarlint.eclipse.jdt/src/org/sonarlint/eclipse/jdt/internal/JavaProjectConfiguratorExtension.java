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
package org.sonarlint.eclipse.jdt.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurationRequest;
import org.sonarlint.eclipse.core.configurator.ProjectConfigurator;

public class JavaProjectConfiguratorExtension extends ProjectConfigurator {

  private final JavaProjectConfigurator javaProjectConfigurator;
  private boolean jdtPresent;

  public JavaProjectConfiguratorExtension() {
    jdtPresent = isJdtPresent();
    javaProjectConfigurator = jdtPresent ? new JavaProjectConfigurator() : null;
  }

  private static boolean isJdtPresent() {
    try {
      Class.forName("org.eclipse.jdt.core.JavaCore");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean canConfigure(IProject project) {
    return jdtPresent;
  }

  @Override
  public void configure(ProjectConfigurationRequest request, IProgressMonitor monitor) {
    javaProjectConfigurator.configure(request, monitor);
  }

}
