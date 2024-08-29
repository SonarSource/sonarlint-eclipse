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
package org.sonarlint.eclipse.core.resource;

import java.util.Collections;
import java.util.Set;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

/**
 *  By default SonarLint will take into account all files in a project except a few excluded by a
 *  light "filtering". In order to narrow down the focus even more and not include files irrelevant
 *  to the analysis (e.g. compiled/binary files) this can be used to rely on other Eclipse plug-ins
 *  to determine which resources should be excluded.
 *
 *  E.g. Output directories marked by JDT ("bin", "target", "build" - based on the system used) or
 *       Python virtual environments that are present, Node.js "node_modules" folder, etc.
 *
 *  INFO: This is independent from {@link ISonarLintProject} as it is used in SonarLint's own
 *        internal implementation of the interface!
 *
 *  @since 10.6
 */
public interface IProjectScopeProvider {
  /**
   *  This is used to provide exclusions, can be both files and folders denoted by their path. It
   *  is also used for traversing a project's resources in order to stop and not visit a specific
   *  resource. As an example, JDT will provide the output directory of the source entries on the
   *  classpath, used for the compiled sources, as exclusions as it will only contain binary data.
   *
   *  @param project to check
   *  @return exclusions if there are any, an empty set otherwise
   */
  default Set<IPath> getExclusions(IProject project) {
    return Collections.emptySet();
  }
}
