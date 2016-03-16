/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.server.ServersManager;

public class SonarLintProjectDecorator implements ILightweightLabelDecorator {

  @Override
  public void decorate(Object element, IDecoration decoration) {
    IProject project = null;
    if (element instanceof IProject) {
      project = (IProject) element;
    } else if (element instanceof IAdaptable) {
      project = ((IAdaptable) element).getAdapter(IProject.class);
    }
    if (project != null) {
      SonarLintProject p = SonarLintProject.getInstance(project);
      if (!p.isBuilderEnabled()) {
        return;
      }
      if (p.getServerId() != null && ServersManager.getInstance().getServer(p.getServerId()) != null) {
        decoration.addOverlay(SonarLintImages.LABEL_DECORATOR);
      }
    }
  }

  @Override
  public void dispose() {
    // Nothing to do
  }

  @Override
  public void addListener(ILabelProviderListener listener) {
    // Nothing to do
  }

  @Override
  public boolean isLabelProperty(Object element, String property) {
    return false;
  }

  @Override
  public void removeListener(ILabelProviderListener listener) {
    // Nothing to do
  }

}
