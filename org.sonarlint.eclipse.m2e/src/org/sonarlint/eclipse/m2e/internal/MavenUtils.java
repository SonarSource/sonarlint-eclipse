/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.m2e.internal;

import java.util.ArrayList;
import java.util.Collection;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.m2e.core.MavenPlugin;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class MavenUtils {
  private MavenUtils() {
  }

  private static MavenProject getRootMavenProject(MavenProject project) {
    var currentProject = project;
    while (currentProject.getParentFile() != null) {
      currentProject = currentProject.getParent();
    }

    return currentProject;
  }

  /**
   *  This checks if a project has a possible parent project: This is due to Maven parent projects can also have other
   *  parent projects themselves. For the project that is the possible parent project, this will evaluate to false!
   *
   *  @param project to check for its parents
   *  @param possibleParentProject what is searched
   *  @return true if it parent is in hierarchy, false otherwise
   */
  private static boolean checkIfPossibleParentProject(MavenProject project, MavenProject possibleParentProject) {
    var currentProject = project;

    while (currentProject.getParentFile() != null) {
      currentProject = currentProject.getParent();
      if (possibleParentProject.equals(currentProject)) {
        return true;
      }
    }

    return false;
  }

  /**
   *  As m2e creates IProject for every module we have to check via the integration as well as Maven itself if
   *  a project either contains sub-modules or if there is a parent project (that is not a dependency).
   */
  public static boolean isPartOfHierarchy(ISonarLintProject project) {
    var iProject = SonarLintUtils.adapt(project.getResource(), IProject.class,
      "[MavenUtils#isPartOfHierarchy] Try find Eclipse from '" + project.getName() + "'");
    if (!M2eUtils.checkIfMavenProject(iProject)) {
      return false;
    }

    var projectManager = MavenPlugin.getMavenProjectRegistry();
    var projectFacade = projectManager.create(iProject, null);
    if (projectFacade == null) {
      return false;
    }

    var modules = projectFacade.getMavenProjectModules();
    if (!modules.isEmpty()) {
      return true;
    }

    try {
      // This and the following method calls require the project to rely on the following bundle:
      // - org.eclipse.m2e.maven.runtime
      // -> The parent file is only present if the parent artifact is not inside a repository but an actual project!
      var mavenProject = projectFacade.getMavenProject(null);
      return mavenProject.getParentFile() != null;
    } catch (CoreException ex) {
      SonarLintLogger.get().error(ex.getMessage(), ex);
    }

    return false;
  }

  @Nullable
  public static ISonarLintProject getRootProjectInWorkspace(ISonarLintProject project) {
    var projectManager = MavenPlugin.getMavenProjectRegistry();

    // If an exception is thrown here due to the SonarLintUtils.adapt(...) returning null, something must be broken on
    // the IDE side as isPartOfHierarchy(...) already made that adaption and the contract is to call it prior to
    // calling this method!
    var slProject = SonarLintUtils.adapt(project.getResource(), IProject.class,
      "[MavenUtils#getRootProjectInWorkspace] Try find Eclipse from '" + project.getName() + "'");
    var projectFacade = projectManager.create(slProject, null);
    if (projectFacade == null) {
      return null;
    }

    try {
      var mavenProject = projectFacade.getMavenProject(null);
      var rootProject = getRootMavenProject(mavenProject);
      if (mavenProject.equals(rootProject)) {
        return project;
      }

      var rootProjectFacade = projectManager.getMavenProject(rootProject.getGroupId(), rootProject.getArtifactId(),
        rootProject.getVersion());
      if (rootProjectFacade != null) {
        return SonarLintUtils.adapt(rootProjectFacade.getProject(), ISonarLintProject.class,
          "[MavenUtils#getRootProjectInWorkspace] Try get SonarLint project from '" + rootProjectFacade.getFinalName()
            + "'");
      }
    } catch (CoreException ex) {
      SonarLintLogger.get().error(ex.getMessage(), ex);
    }

    return null;
  }

  public static Collection<ISonarLintProject> getProjectSubProjects(ISonarLintProject project) {
    var modules = new ArrayList<ISonarLintProject>();

    var projectManager = MavenPlugin.getMavenProjectRegistry();

    // If an exception is thrown here due to the SonarLintUtils.adapt(...) returning null, something must be broken on
    // the IDE side as isPartOfHierarchy(...) already made that adaption and the contract is to call it prior to
    // calling this method!
    var slProject = SonarLintUtils.adapt(project.getResource(), IProject.class,
      "[MavenUtils#getProjectSubProjects] Try find Eclipse from '" + project.getName() + "'");
    var projectFacade = projectManager.create(slProject, null);
    if (projectFacade == null) {
      return modules;
    }

    try {
      var parentProject = projectFacade.getMavenProject(null);

      for (var mavenProjectFacade : projectManager.getProjects()) {
        var mavenProject = mavenProjectFacade.getMavenProject(null);
        if (checkIfPossibleParentProject(mavenProject, parentProject)) {
          var possibleSlProject = SonarLintUtils.adapt(mavenProjectFacade.getProject(), ISonarLintProject.class,
            "[MavenUtils#getProjectSubProjects] Try get SonarLint project from '" + mavenProject.getName() + "'");
          if (possibleSlProject != null) {
            modules.add(possibleSlProject);
          }
        }
      }
    } catch (CoreException ex) {
      SonarLintLogger.get().error(ex.getMessage(), ex);
    }

    return modules;
  }
}
