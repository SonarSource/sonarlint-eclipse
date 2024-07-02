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
package org.sonarlint.eclipse.buildship.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject;
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

  private static boolean checkIfGradleProject(IProject project) {
    try {
      return project.hasNature(GRADLE_PROJECT_NATURE);
    } catch (CoreException err) {
      SonarLintLogger.get().error(err.getMessage(), err);
    }
    return false;
  }

  private static HierarchicalEclipseProject getRootGradleProject(HierarchicalEclipseProject project) {
    var currentProject = project;
    while (currentProject.getParent() != null) {
      currentProject = currentProject.getParent();
    }
    return currentProject;
  }

  /**
   *  Because the Gradle Tooling API is completely autonomous from Eclipse bundles/plug-ins, we cannot simply adapt a
   *  Gradle project to a ISonarLintProject. We have to actually match the project directories as there cannot be two
   *  Gradle proejcts in the same directory.
   *
   *  @param projects all the available projects we can try to match against
   *  @param gradleEclipseProject the Gradle project that should be found in the workspace
   *  @return a project if there is a match, null otherwise
   */
  @Nullable
  private static ISonarLintProject matchGradleProject(Collection<ISonarLintProject> projects,
    HierarchicalEclipseProject gradleEclipseProject) {
    var projectDirectory = gradleEclipseProject.getProjectDirectory();
    for (var project : projects) {
      var localFile = FileUtils.toLocalFile(project.getResource());
      if (localFile != null && projectDirectory.equals(localFile)) {
        return project;
      }
    }
    return null;
  }

  private static HashSet<HierarchicalEclipseProject> getChildGradleProjects(HierarchicalEclipseProject project) {
    var projects = new HashSet<HierarchicalEclipseProject>();
    for (var child : project.getChildren()) {
      projects.add(child);
      projects.addAll(getChildGradleProjects(child));
    }
    return projects;
  }

  public static boolean isPartOfHierarchy(ISonarLintProject project) {
    var iProject = SonarLintUtils.adapt(project.getResource(), IProject.class,
      "[GradleUtils#isPartOfHierarchy] Try find Eclipse from '" + project.getName() + "'");
    // This fails in case the adaption returns null. We want this to fail here as it means there is an issue with the
    // adaption logic or ISonarLintProject!
    if (!checkIfGradleProject(iProject)) {
      return false;
    }

    // Maybe the project is not local, in that case we cannot access that information
    var localFile = FileUtils.toLocalFile(project.getResource());
    if (localFile == null) {
      return false;
    }

    // The Gradle Tooling API isn't really informative about the behavior when there is no "correct" Gradle project,
    // e.g. the Eclipse ".project" file has the correct nature but the project is not based on Gradle anymore. That's
    // the reason for the more general catch block.
    ProjectConnection connection = null;
    try {
      connection = GradleConnector.newConnector().forProjectDirectory(localFile).connect();
      var gradleEclipseProject = connection.model(HierarchicalEclipseProject.class).get();
      return gradleEclipseProject.getName() != null;
    } catch (Exception err) {
      SonarLintLogger.get().debug("Project '" + project.getName()
        + "' cannot be interacted with from the Gradle Tooling API.", err);
    } finally {
      if (connection != null) {
        connection.close();
      }
    }

    return false;
  }

  @Nullable
  public static ISonarLintProject getRootProjectInWorkspace(ISonarLintProject project) {
    // If an exception is thrown here due to the toLocalFile(...) returning null or due to the GradleConnector,
    // something must be broken on the Eclipse Buildship plug-in side as isPartOfHierarchy(...) already interacted with
    // it and the contract is to call it prior to calling this method!
    var connection = GradleConnector.newConnector()
      .forProjectDirectory(FileUtils.toLocalFile(project.getResource()))
      .connect();

    var gradleEclipseProject = connection.model(HierarchicalEclipseProject.class).get();
    var rootProject = getRootGradleProject(gradleEclipseProject);
    if (gradleEclipseProject.equals(rootProject)) {
      return project;
    }

    var allProjects = SonarLintUtils.allProjects();
    var possibleMatchedProject = matchGradleProject(allProjects, rootProject);
    if (possibleMatchedProject == null) {
      SonarLintLogger.get().traceIdeMessage("[GradleUtils#getRootProjectInWorkspace] For the project '"
        + project.getName() + "' a Gradle root project was found ('" + rootProject.getName()
        + "') but cannot be matched to any project in the workspace!");
    }
    connection.close();
    return possibleMatchedProject;
  }

  public static Collection<ISonarLintProject> getProjectSubProjects(ISonarLintProject project) {
    var subProjects = new ArrayList<ISonarLintProject>();

    var connection = GradleConnector.newConnector()
      .forProjectDirectory(FileUtils.toLocalFile(project.getResource()))
      .connect();

    var allProjects = SonarLintUtils.allProjects();
    var gradleEclipseProject = connection.model(HierarchicalEclipseProject.class).get();
    var gradleEclipseChildProjects = getChildGradleProjects(gradleEclipseProject);
    for (var child : gradleEclipseChildProjects) {
      var possibleMatchedProject = matchGradleProject(allProjects, child);
      if (possibleMatchedProject == null) {
        SonarLintLogger.get().traceIdeMessage("[GradleUtils#getProjectSubProjects] Gradle project '"
          + child.getName() + "' cannot be mantched to any project in the workspace!");
      } else {
        subProjects.add(possibleMatchedProject);
      }
    }

    return subProjects;
  }
}
