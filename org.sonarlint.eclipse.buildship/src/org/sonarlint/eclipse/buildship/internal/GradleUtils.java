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
package org.sonarlint.eclipse.buildship.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.Nullable;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.utils.FileUtils;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class GradleUtils {
  // Copied in order to not have the dependency on one bundle/plug-in just for one constant! Never changed in the last
  // years and probably will never change again!
  // -> bundle "org.eclipse.buildship.core",
  // -> class "org.eclipse.buildship.core.internal.configuration.GradleProjectNature#ID"
  private static final String GRADLE_PROJECT_NATURE = "org.eclipse.buildship.core.gradleprojectnature";

  private GradleUtils() {
    // utility class
  }

  /** We check if it is a relevant project only via the nature, not via the Gradle Tooling API as it is too slow */
  public static boolean checkIfGradleProject(IProject project) {
    try {
      return project.hasNature(GRADLE_PROJECT_NATURE);
    } catch (CoreException err) {
      SonarLintLogger.get().error(err.getMessage(), err);
    }
    return false;
  }

  /**
   *  Because the Gradle Tooling API is completely autonomous from Eclipse bundles/plug-ins, we cannot simply adapt a
   *  Gradle project to a ISonarLintProject. We have to actually match the project directories as there cannot be two
   *  Gradle projects in the same directory.
   *
   *  @param projects all the available projects we can try to match against
   *  @param gradleProject the Gradle project that should be found in the workspace
   *  @return a project if there is a match, null otherwise
   */
  @Nullable
  private static ISonarLintProject matchGradleProject(Collection<ISonarLintProject> projects,
    BasicGradleProject gradleProject) {
    var projectDirectory = gradleProject.getProjectDirectory();
    for (var project : projects) {
      var localFile = FileUtils.toLocalFile(project.getResource());
      if (localFile != null && projectDirectory.equals(localFile)) {
        return project;
      }
    }
    return null;
  }

  @Nullable
  public static ISonarLintProject getRootProjectInWorkspace(ISonarLintProject project) {
    var iProject = (IProject) project.getResource();

    // Try to get cached project connection as it is way faster!
    var connection = getProjectConnection(iProject);
    if (connection == null) {
      return null;
    }

    // Check whether this is already the root project, we don't have to check the others then!
    var gradleBuild = connection.model(GradleBuild.class).get();
    var rootProject = gradleBuild.getRootProject();
    if (rootProject.getProjectDirectory().equals(FileUtils.toLocalFile(iProject))) {
      return project;
    }

    // Check other ISonarLintProject instances in the workspace when it is not this project
    var allProjects = SonarLintUtils.allProjects();
    var possibleMatchedProject = matchGradleProject(allProjects, rootProject);
    if (possibleMatchedProject == null) {
      SonarLintLogger.get().traceIdeMessage("[GradleUtils#getRootProjectInWorkspace] For the project '"
        + project.getName() + "' a Gradle root project was found ('" + rootProject.getName()
        + "') but cannot be matched to any project in the workspace!");
    }
    return possibleMatchedProject;
  }

  public static Collection<ISonarLintProject> getProjectSubProjects(ISonarLintProject project) {
    var iProject = (IProject) project.getResource();
    var subProjects = new ArrayList<ISonarLintProject>();

    // Try to get cached project connection as it is way faster!
    var connection = getProjectConnection(iProject);
    if (connection == null) {
      return subProjects;
    }

    var allProjects = SonarLintUtils.allProjects();
    try {
      var localFile = FileUtils.toLocalFile(iProject);
      if (localFile == null) {
        return subProjects;
      }
      var projectPath = localFile.toPath().toRealPath().toString() + "/";

      // Iterate over all the projects of the build and try to find the child-projects
      for (var child : connection.model(GradleBuild.class).get().getProjects()) {
        var childPath = child.getProjectDirectory().toPath().toRealPath().toString();
        if (childPath.startsWith(projectPath) && !childPath.equals(projectPath)) {
          var possibleMatchedProject = matchGradleProject(allProjects, child);
          if (possibleMatchedProject == null) {
            SonarLintLogger.get().traceIdeMessage("[GradleUtils#getProjectSubProjects] Gradle project '"
              + child.getName() + "' cannot be mantched to any project in the workspace!");
          } else {
            subProjects.add(possibleMatchedProject);
          }
        }
      }
    } catch (IOException err) {
      SonarLintLogger.get().error("Cannot rely on Gradle Tooling API for sub-projects of project '"
        + project.getName() + "' based on Buildship!", err);
    }
    return subProjects;
  }

  /**
   *  All the exclusions that are coming from Gradle (via the Tooling API) and not from the JDT integration itself that
   *  is created when the project is imported.
   *
   *  - buildSrc/build as the output directory
   *  - .gradle + buildSrc/.gradle as the wrapper cache
   *  - output directory in Eclipse, maybe a fallback
   *  - build directory of Gradle
   *  - every child projects folder relative to this project
   *
   *  Why the trailing space for the paths? E.g. "sonar-orchestrator-junit4" starts with "sonar-orchestrator", this is
   *  just in case of sub-projects are named very similar to root projects.
   */
  public static Set<IPath> getExclusions(IProject project) {
    var exclusions = new HashSet<IPath>();
    var projectPath = project.getFullPath().makeAbsolute().toOSString();

    // 1) The Gradle Tooling API has no access to `buildSrc`, therefore add it manually, even if not present
    exclusions.add(Path.fromOSString("/" + project.getName() + "/buildSrc/build"));
    exclusions.add(Path.fromOSString("/" + project.getName() + "/buildSrc/.gradle"));

    // 2) The Gradle Tooling API has no access to the wrapper, therefore add it manually, even if not present
    exclusions.add(Path.fromOSString("/" + project.getName() + "/.gradle"));

    // 3) The Gradle Tooling API can only access the build directory with very expensive calls
    exclusions.add(Path.fromOSString("/" + project.getName() + "/build"));

    // 4) Try to get cached project connection as it is way faster!
    var connection = getProjectConnection(project);
    if (connection == null) {
      traceExclusions(exclusions, projectPath);
      return exclusions;
    }

    try {
      var localFile = FileUtils.toLocalFile(project);
      if (localFile == null) {
        return exclusions;
      }
      var localPath = localFile.toPath().toRealPath().toString() + "/";

      // 5) Iterate over all the projects of the build and try to find the child-projects
      for (var child : connection.model(GradleBuild.class).get().getProjects()) {
        var childPath = child.getProjectDirectory().toPath().toRealPath().toString();
        if (childPath.startsWith(localPath) && !childPath.equals(localPath)) {
          var relativePath = childPath.replace(localPath, "/" + project.getName() + "/");
          exclusions.add(Path.fromOSString(relativePath));
        }
      }
    } catch (Exception err) {
      SonarLintLogger.get().error("Cannot rely on Gradle Tooling API for exclusions of project '"
        + project.getName() + "' based on Buildship!", err);
    }

    traceExclusions(exclusions, projectPath);
    return exclusions;
  }

  private static void traceExclusions(Set<IPath> exclusions, String projectPath) {
    SonarLintLogger.get().traceIdeMessage("[GradleUtils#getExclusions] The following paths have been excluded from "
      + "indexing for the project at '" + projectPath + "': "
      + String.join(", ", exclusions.stream().map(Object::toString).collect(Collectors.toList())));
  }

  @Nullable
  private static ProjectConnection getProjectConnection(IProject project) {
    var projectConnection = ProjectConnectionCache.getConnection(SonarLintUtils.getConfigScopeId(project));
    if (projectConnection != null) {
      return projectConnection;
    }

    try {
      projectConnection = GradleConnector.newConnector()
        .forProjectDirectory(FileUtils.toLocalFile(project))
        .connect();

      var configScopeIds = new ArrayList<String>();
      var sonarLintProjects = SonarLintUtils.allProjects();
      for (var gradleProject : projectConnection.model(GradleBuild.class).get().getProjects()) {
        var sonarLintProject = matchGradleProject(sonarLintProjects, gradleProject);
        if (sonarLintProject != null) {
          configScopeIds.add(SonarLintUtils.getConfigScopeId((IProject) sonarLintProject.getResource()));
        }
      }
      ProjectConnectionCache.putConnection(configScopeIds, projectConnection);
      return projectConnection;
    } catch (Exception err) {
      SonarLintLogger.get().error("Project '" + project.getName()
        + "' cannot be interacted with from the Gradle Tooling API.", err);
    }

    return null;
  }
}
