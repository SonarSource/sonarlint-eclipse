/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class SonarLintProjectEventListener implements IResourceChangeListener {

  @Override
  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
      try {
        event.getDelta().accept(this::visitDelta);
      } catch (CoreException e) {
        SonarLintLogger.get().error(e.getMessage(), e);
      }
    }
  }

  private boolean visitDelta(IResourceDelta delta) {
    IResource resource = delta.getResource();
    if ((resource.getType() & IResource.PROJECT) != 0 && (delta.getFlags() & IResourceDelta.OPEN) != 0) {
      ISonarLintProject project = Adapters.adapt(delta.getResource(), ISonarLintProject.class);
      if (project != null && project.isBound()) {
        if (project.isOpen()) {
          SonarLintUiPlugin.subscribeToNotifications(project);
        } else {
          SonarLintCorePlugin.getInstance().notificationsManager().unsubscribe(project);
        }
      }
      return false;
    }
    return true;
  }
}
