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
package org.sonarlint.eclipse.ui.internal;

import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.internal.cache.DefaultSonarLintProjectAdapterCache;
import org.sonarlint.eclipse.core.internal.cache.IProjectScopeProviderCache;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/**
 *  This is used for cleaning all the caches linked to a project (via its configuration scope id) when it is closed.
 *  We want to remove the cache entries immediately instead of waiting for the caches to clean themselves.
 */
public class ConfigScopeIdCacheCleaner implements IResourceChangeListener {
  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
      try {
        event.getDelta().accept(delta -> visitDelta(delta));
      } catch (CoreException e) {
        SonarLintLogger.get().error(e.getMessage(), e);
      }
    }
  }

  private static boolean visitDelta(IResourceDelta delta) {
    if ((delta.getFlags() & IResourceDelta.OPEN) != 0) {
      var project = SonarLintUtils.adapt(delta.getResource(), ISonarLintProject.class,
        "[DefaultSonarLintProjectAdapterCacheCleaner#visitDelta] Try get project of event '" + delta.getResource()
          + "'");
      if (project != null && (!project.isOpen())) {
        var configScopeId = ConfigScopeSynchronizer.getConfigScopeId(project);

        DefaultSonarLintProjectAdapterCache.INSTANCE.removeEntry(configScopeId);
        IProjectScopeProviderCache.INSTANCE.removeEntry(configScopeId);
      }
      return false;
    }

    // We didn't close a project, maybe a container containing a project. Therefore, dig deeper!
    return true;
  }
}
