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
package org.sonarlint.eclipse.core.internal.resources;

import java.util.Set;
import java.util.stream.Collectors;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;

public class ProjectsProviderUtils {

  private ProjectsProviderUtils() {
    // Utility class
  }

  public enum WorkspaceProjectsBindingRatio {
    NONE_BOUND,
    SOME_BOUND,
    ALL_BOUND
  }

  /**
   *  Useful when we want SonarLint to behave differently when
   *  - no project bound (ret = 0)
   *  - at least one project bound (0 < ret < 1)
   *  - all projects bound (ret = 1)
   */
  public static WorkspaceProjectsBindingRatio boundToAllProjectsRatio() {
    var allProjects = SonarLintUtils.allProjects();
    var numberOfAllProjects = allProjects.size();
    if (numberOfAllProjects == 0) {
      return WorkspaceProjectsBindingRatio.NONE_BOUND;
    }

    var boundProjects = allProjects.stream()
      .filter(prj -> SonarLintCorePlugin.getConnectionManager().resolveBinding(prj).isPresent())
      .collect(Collectors.toSet());
    var ratio = boundProjects.size() / numberOfAllProjects;
    if (ratio == 0f) {
      return WorkspaceProjectsBindingRatio.NONE_BOUND;
    }

    return ratio == 1f
      ? WorkspaceProjectsBindingRatio.ALL_BOUND
      : WorkspaceProjectsBindingRatio.SOME_BOUND;
  }

  public static Set<String> allConfigurationScopeIds() {
    return SonarLintUtils.allProjects().stream().map(ConfigScopeSynchronizer::getConfigScopeId)
      .collect(Collectors.toSet());
  }
}
