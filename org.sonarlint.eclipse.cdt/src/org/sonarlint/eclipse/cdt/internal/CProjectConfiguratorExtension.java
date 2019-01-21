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
package org.sonarlint.eclipse.cdt.internal;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IAnalysisConfigurator;
import org.sonarlint.eclipse.core.analysis.IFileLanguageProvider;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/**
 * Responsible for checking at runtime if CDT plugin is installed.
 */
public class CProjectConfiguratorExtension implements IAnalysisConfigurator, IFileLanguageProvider {

  private final CdtUtils cdtUtils;

  public CProjectConfiguratorExtension() {
    cdtUtils = isCdtPresent() ? new CdtUtils() : null;
  }

  private static boolean isCdtPresent() {
    try {
      Class.forName("org.eclipse.cdt.core.CCorePlugin");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  @Override
  public boolean canConfigure(ISonarLintProject project) {
    try {
      IProject underlyingProject = project.getResource() instanceof IProject ? (IProject) project.getResource() : null;
      // Constants are inlined so this should not cause ClassNotFound
      return cdtUtils != null &&
        underlyingProject != null &&
        (underlyingProject.hasNature(CProjectNature.C_NATURE_ID) || underlyingProject.hasNature(CCProjectNature.CC_NATURE_ID));
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
      return false;
    }
  }

  @Override
  public void configure(IPreAnalysisContext context, IProgressMonitor monitor) {
    cdtUtils.configure(context, monitor);
  }

  @Override
  public String language(ISonarLintFile file) {
    IFile iFile = file.getResource() instanceof IFile ? (IFile) file.getResource() : null;
    if (cdtUtils != null && iFile != null) {
      return cdtUtils.language(iFile);
    }
    return null;
  }

}
