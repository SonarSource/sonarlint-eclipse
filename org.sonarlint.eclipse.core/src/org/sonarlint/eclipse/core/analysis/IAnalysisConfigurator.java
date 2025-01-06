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
package org.sonarlint.eclipse.core.analysis;

import java.util.Collections;
import java.util.Set;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/**
 * Implemented by components that configure the analysis on certain environments.
 * For example, we have configurators for C/C++ projects in Eclipse CDT and for Java projects in JDT.
 *
 * @since 3.0
 */
public interface IAnalysisConfigurator {

  /**
   * Tell if this analysis configurator can configure the given project.
   */
  boolean canConfigure(ISonarLintProject project);

  /**
   * Configures SonarLint analysis, using information from Eclipse project.
   */
  void configure(IPreAnalysisContext context, IProgressMonitor monitor);

  /**
   * This method is called after analysis is finished. Can be used to perform some cleanup.
   */
  default void analysisComplete(IPostAnalysisContext context, IProgressMonitor monitor) {
    // Do nothing by default
  }

  /**
   * By default SonarLint Eclipse will disable some languages that are not working out of the box without some requirements.
   * If this IAnalysisConfigurator is fulfilling some requirements, then you can enable the associated language.
   * @return List of languages that should be enabled (e.g. {@link SonarLintLanguage#JAVA}, {@link SonarLintLanguage#CPP}, {@link SonarLintLanguage#COBOL}, ...)
   * @see SonarLintLanguage
   * @since 9.4
   */
  default Set<SonarLintLanguage> enableLanguages() {
    return Collections.emptySet();
  }
}
