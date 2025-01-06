/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
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
      for (var mavenProjectFacade : getProjects(projectManager)) {
        var mavenProject = mavenProjectFacade.getMavenProject(null);
        if (checkIfPossibleParentProject(mavenProject, parentProject)) {
          var possibleSlProject = SonarLintUtils.adapt(mavenProjectFacade.getProject(), ISonarLintProject.class,
            "[MavenUtils#getProjectSubProjects] Try get SonarLint project from '" + mavenProject.getName() + "'");
          if (possibleSlProject != null) {
            modules.add(possibleSlProject);
          }
        }
      }
    } catch (Exception ex) {
      SonarLintLogger.get().error(ex.getMessage(), ex);
    }

    return modules;
  }

  /**
   *  All exclusions that are coming from Maven (via m2e) and not from the JDT integration itself that is created when
   *  the project is imported.
   *
   *  - output directory of Maven, including the one for production and test sources
   *  - every child modules folder relative to this project
   *
   *  Why the trailing space for the paths? E.g. "sonar-orchestrator-junit4" starts with "sonar-orchestrator", this is
   *  just in case of sub-projects are named very similar to root projects.
   */
  public static Set<IPath> getExclusions(IProject project) {
    var exclusions = new HashSet<IPath>();
    var projectPath = project.getFullPath().makeAbsolute().toOSString();

    // 1) Add the target directory
    // This is due us not being able to access "IMavenProjectFacade#getBuildOutputLocation" as it is not available in
    // all versions of m2e for the Eclipse IDE versions we support!
    exclusions.add(Path.fromOSString("/" + project.getName() + "/target"));

    var projectManager = MavenPlugin.getMavenProjectRegistry();
    var facade = projectManager.create(project, null);
    if (facade == null) {
      traceExclusions(exclusions, projectPath);
      return exclusions;
    }

    // 2) Add the output directory for the production sources (can differ from default output directory)
    var outputLocation = facade.getOutputLocation();
    if (outputLocation != null) {
      exclusions.add(outputLocation);
    }

    // 3) Add the output directory for the test sources (can differ from default output directory)
    var testOutputLocation = facade.getTestOutputLocation();
    if (testOutputLocation != null) {
      exclusions.add(testOutputLocation);
    }

    // 4) For every module and its project directory
    // Compared to "getProjectSubProjects" this will find every Maven module / project even the ones that are not
    // direct children of the parent. But this is no problem in this case!
    var parentPath = project.getLocationURI().getPath() + "/";
    try {
      for (var projectFacade : getProjects(projectManager)) {
        var projectFacadePath = projectFacade.getProject().getLocationURI().getPath();
        if (!projectFacadePath.equals(parentPath) && projectFacadePath.startsWith(parentPath)) {
          var relativePath = projectFacadePath.replace(parentPath, "/" + project.getName() + "/");
          exclusions.add(Path.fromOSString(relativePath));
        }
      }
    } catch (Exception ex) {
      SonarLintLogger.get().error(ex.getMessage(), ex);
    }

    traceExclusions(exclusions, projectPath);
    return exclusions;
  }

  private static void traceExclusions(Set<IPath> exclusions, String projectPath) {
    SonarLintLogger.get().traceIdeMessage("[MavenUtils#getExclusions] The following paths have been excluded from "
      + "indexing for the project at '" + projectPath + "': "
      + String.join(", ", exclusions.stream().map(Object::toString).collect(Collectors.toList())));
  }

  /**
   *  In order to stay compatible with old and new versions of m2e we have to run this via reflection. The oldest
   *  Eclipse IDE versions come bundled with m2e where the following signature is present:
   *
   *  org.eclipse.m2e.core.project.IMavenProjectFacade[] org.eclipse.m2e.core.project.IMavenProjectRegistry#getProjects()
   *
   *  While newer versions changed it to be:
   *
   *  java.util.List<org.eclipse.m2e.core.project.IMavenProjectFacade> org.eclipse.m2e.core.project.IMavenProjectRegistry#getProjects()
   *
   *  @see https://github.com/eclipse-m2e/m2e-core/issues/1820
   */
  private static List<IMavenProjectFacade> getProjects(IMavenProjectRegistry registry) {
    List<IMavenProjectFacade> projects = new ArrayList<>();

    try {
      var mavenProjectRegistry = registry.getClass();
      var getProjects = mavenProjectRegistry.getMethod("getProjects");
      var returnType = getProjects.getReturnType();
      if (returnType == IMavenProjectFacade[].class) {
        IMavenProjectFacade[] facades = (IMavenProjectFacade[]) getProjects.invoke(registry);
        projects = Arrays.asList(facades);
      } else {
        projects = (List<IMavenProjectFacade>) getProjects.invoke(registry);
      }
    } catch (Exception err) {
      SonarLintLogger.get().error("", err);
    }

    return projects;
  }
}
