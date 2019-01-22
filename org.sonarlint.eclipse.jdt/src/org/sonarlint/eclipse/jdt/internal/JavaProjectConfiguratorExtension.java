/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.analysis.IAnalysisConfigurator;
import org.sonarlint.eclipse.core.analysis.IFileTypeProvider;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintFileAdapterParticipant;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class JavaProjectConfiguratorExtension implements IAnalysisConfigurator, ISonarLintFileAdapterParticipant, IFileTypeProvider {

  private final JdtUtils javaProjectConfigurator;
  private final boolean jdtPresent;

  public JavaProjectConfiguratorExtension() {
    jdtPresent = isJdtPresent();
    javaProjectConfigurator = jdtPresent ? new JdtUtils() : null;
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
  public boolean canConfigure(ISonarLintProject project) {
    return jdtPresent && project.getResource() instanceof IProject
      && JdtUtils.hasJavaNature((IProject) project.getResource());
  }

  @Override
  public void configure(IPreAnalysisContext context, IProgressMonitor monitor) {
    javaProjectConfigurator.configure(context, monitor);
  }

  @Override
  public boolean exclude(IFile file) {
    if (jdtPresent) {
      return JdtUtils.shouldExclude(file);
    }
    return false;
  }

  @Override
  public ISonarLintFileType qualify(ISonarLintFile file) {
    if (jdtPresent) {
      return JdtUtils.qualify(file);
    }
    return ISonarLintFileType.UNKNOWN;
  }

}
