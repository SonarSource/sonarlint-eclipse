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
package org.sonarlint.eclipse.core.internal.adapter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintFileAdapter;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintFileAdapterParticipant;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectAdapterParticipant;

public class DefaultSonarLintAdapterFactory implements IAdapterFactory {

  @Override
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if (!(adaptableObject instanceof IAdaptable)) {
      return null;
    }
    if (ISonarLintProject.class.equals(adapterType) || ISonarLintIssuable.class.equals(adapterType)) {
      IProject project = ((IAdaptable) adaptableObject).getAdapter(IProject.class);
      if (project != null) {
        return getProjectAdapter(adapterType, project);
      }
    }
    if (ISonarLintFile.class.equals(adapterType) || ISonarLintIssuable.class.equals(adapterType)) {
      IFile file = ((IAdaptable) adaptableObject).getAdapter(IFile.class);
      if (file != null) {
        return getFileAdapter(adapterType, file);
      }
      // Some objects may not have an adapter for IFile but only for IResource
      IResource resource = ((IAdaptable) adaptableObject).getAdapter(IResource.class);
      if (resource instanceof IFile) {
        return getFileAdapter(adapterType, (IFile) resource);
      }
    }
    return null;
  }

  private static <T> T getProjectAdapter(Class<T> adapterType, IProject project) {
    for (ISonarLintProjectAdapterParticipant projectAdapterParticipant : SonarLintCorePlugin.getExtensionTracker().getProjectAdapterParticipants()) {
      if (projectAdapterParticipant.exclude(project)) {
        SonarLintLogger.get().debug("Project '" + project.getName() + "' excluded by '" + projectAdapterParticipant.getClass().getSimpleName() + "'");
        return null;
      }
    }
    return adaptProject(adapterType, project);
  }

  private static <T> T adaptProject(Class<T> adapterType, IProject project) {
    for (ISonarLintProjectAdapterParticipant p : SonarLintCorePlugin.getExtensionTracker().getProjectAdapterParticipants()) {
      ISonarLintProject adapted = p.adapt(project);
      if (adapted != null) {
        return adapterType.cast(adapted);
      }
    }
    return adapterType.cast(new DefaultSonarLintProjectAdapter(project));
  }

  /**
   * Change this method with caution since it is critical for some Cobol IDEs integration
   */
  private static <T> T getFileAdapter(Class<T> adapterType, IFile file) {
    // First do some very cheap checks to see if we can exclude the physical file
    if (!SonarLintUtils.isSonarLintFileCandidate(file)) {
      return null;
    }
    // Not let's call the ISonarLintFileAdapterParticipant#exclude
    for (ISonarLintFileAdapterParticipant fileAdapterParticipant : SonarLintCorePlugin.getExtensionTracker().getFileAdapterParticipants()) {
      if (fileAdapterParticipant.exclude(file)) {
        SonarLintLogger.get().debug("File '" + file.getProjectRelativePath() + "' excluded by '" + fileAdapterParticipant.getClass().getSimpleName() + "'");
        return null;
      }
    }
    return adaptFile(adapterType, file);
  }

  private static <T> T adaptFile(Class<T> adapterType, IFile file) {
    // Try to find one ISonarLintFileAdapterParticipant that will adapt the IFile
    for (ISonarLintFileAdapterParticipant p : SonarLintCorePlugin.getExtensionTracker().getFileAdapterParticipants()) {
      ISonarLintFile adapted = p.adapt(file);
      if (adapted != null) {
        return adapterType.cast(adapted);
      }
    }
    // Fallback to our default behavior
    ISonarLintProject project = file.getProject().getAdapter(ISonarLintProject.class);
    if (project == null) {
      // IProject was likely excluded by a ISonarLintProjectAdapterParticipant, so don't try to adapt the file
      return null;
    }
    return adapterType.cast(new DefaultSonarLintFileAdapter(project, file));
  }

  @Override
  public Class<?>[] getAdapterList() {
    return new Class[] {ISonarLintProject.class, ISonarLintFile.class, ISonarLintIssuable.class};
  }

}
