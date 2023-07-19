/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.resources;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Platform;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectsProvider;

import static java.util.function.Predicate.not;

public class DefaultSonarLintProjectsProvider implements ISonarLintProjectsProvider {
  private static final String TECHNICAL_PROJECT_PDE = "External Plug-in Libraries";
  private static final String TECHNICAL_PROJECT_PDT = "Servers";
  private static final String TECHNICAL_PROJECT_RSE_1 = "RemoteSystemsTempFiles";
  private static final String TECHNICAL_PROJECT_RSE_2 = "RemoteSystemsConnections";
  
  @Override
  public Collection<ISonarLintProject> get() {
    return Stream.of(ResourcesPlugin.getWorkspace().getRoot().getProjects())
      .filter(prj -> !isTechnicalProject(prj.getName()))
      .filter(IProject::isAccessible)
      .map(p -> Adapters.adapt(p, ISonarLintProject.class))
      .filter(not(Objects::isNull))
      .collect(Collectors.toList());
  }
  
  /** This should be extended with further checks from other plug-ins providing technical projects */
  private static boolean isTechnicalProject(String projectName) {
    // check for Eclipse PDE technical projects
    var isTechnicalProject = TECHNICAL_PROJECT_PDE.equals(projectName) && Platform.getBundle("org.eclipse.pde.ui") != null;
    
    // check for Eclipse PDT technical projects
    isTechnicalProject = isTechnicalProject ||
      (TECHNICAL_PROJECT_PDT.equals(projectName) && Platform.getBundle("org.eclipse.php.ui") != null);
    
    // check for Eclipse RSE technical projects
    isTechnicalProject = isTechnicalProject ||
      (TECHNICAL_PROJECT_RSE_1.equals(projectName) && Platform.getBundle("org.eclipse.rse.ui") != null);
    isTechnicalProject = isTechnicalProject ||
      (TECHNICAL_PROJECT_RSE_2.equals(projectName) && Platform.getBundle("org.eclipse.rse.ui") != null);
    
    return isTechnicalProject;
  }
}
