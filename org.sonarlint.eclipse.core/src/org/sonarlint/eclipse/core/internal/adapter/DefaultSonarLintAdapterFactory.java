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
package org.sonarlint.eclipse.core.internal.adapter;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintFileAdapter;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class DefaultSonarLintAdapterFactory implements IAdapterFactory {

  @Nullable
  @Override
  public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
    if (!(adaptableObject instanceof IAdaptable)) {
      return null;
    }
    if (ISonarLintProject.class.equals(adapterType) || ISonarLintIssuable.class.equals(adapterType)) {
      var project = ((IAdaptable) adaptableObject).getAdapter(IProject.class);
      if (project != null) {
        return getProjectAdapter(adapterType, project);
      }
    }
    if (ISonarLintFile.class.equals(adapterType) || ISonarLintIssuable.class.equals(adapterType)) {
      var file = ((IAdaptable) adaptableObject).getAdapter(IFile.class);
      if (file != null) {
        return getFileAdapter(adapterType, file);
      }
      // Some objects may not have an adapter for IFile but only for IResource
      var resource = ((IAdaptable) adaptableObject).getAdapter(IResource.class);
      if (resource instanceof IFile) {
        return getFileAdapter(adapterType, (IFile) resource);
      }
    }
    return null;
  }

  @Nullable
  private static <T> T getProjectAdapter(Class<T> adapterType, IProject project) {
    if (isRseTempProject(project)) {
      return null;
    }
    for (var projectAdapterParticipant : SonarLintExtensionTracker.getInstance().getProjectAdapterParticipants()) {
      if (projectAdapterParticipant.exclude(project)) {
        SonarLintLogger.get().traceIdeMessage("Project '" + project.getName() + "' excluded by '" + projectAdapterParticipant.getClass().getSimpleName() + "'");
        return null;
      }
    }
    return adaptProject(adapterType, project);
  }

  private static boolean isRseTempProject(IProject project) {
    try {
      return project.hasNature("org.eclipse.rse.ui.remoteSystemsTempNature");
    } catch (CoreException e) {
      return false;
    }
  }

  private static <T> T adaptProject(Class<T> adapterType, IProject project) {
    var defaultSonarLintProjectAdapter = new DefaultSonarLintProjectAdapter(project);
    for (var p : SonarLintExtensionTracker.getInstance().getProjectAdapterParticipants()) {
      var adapted = p.adapt(project, defaultSonarLintProjectAdapter);
      if (adapted != null) {
        return adapterType.cast(adapted);
      }
    }
    return adapterType.cast(defaultSonarLintProjectAdapter);
  }

  /**
   * Change this method with caution since it is critical for some Cobol IDEs integration
   */
  @Nullable
  private static <T> T getFileAdapter(Class<T> adapterType, IFile file) {
    // First do some very cheap checks to see if we can exclude the physical file
    if (!SonarLintUtils.isSonarLintFileCandidate(file)) {
      return null;
    }
    // Not let's call the ISonarLintFileAdapterParticipant#exclude
    for (var fileAdapterParticipant : SonarLintExtensionTracker.getInstance().getFileAdapterParticipants()) {
      if (fileAdapterParticipant.exclude(file)) {
        SonarLintLogger.get().traceIdeMessage("File '" + file.getProjectRelativePath() + "' excluded by '" + fileAdapterParticipant.getClass().getSimpleName() + "'");
        return null;
      }
    }
    return adaptFile(adapterType, file);
  }

  @Nullable
  private static <T> T adaptFile(Class<T> adapterType, IFile file) {
    // Try to find one ISonarLintFileAdapterParticipant that will adapt the IFile
    for (var p : SonarLintExtensionTracker.getInstance().getFileAdapterParticipants()) {
      var adapted = p.adapt(file);
      if (adapted != null) {
        return adapterType.cast(adapted);
      }
    }
    // Fallback to our default behavior
    var project = file.getProject().getAdapter(ISonarLintProject.class);
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
