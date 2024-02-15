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
package org.sonarlint.eclipse.ui.internal.binding;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.SonarProject;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class BindingsViewContentProvider extends BaseContentProvider implements ITreeContentProvider {

  @Override
  public Object[] getElements(Object element) {
    return SonarLintCorePlugin.getConnectionManager().getConnections().toArray();
  }

  @Override
  public Object[] getChildren(Object element) {
    if (element instanceof ConnectionFacade) {
      var connection = (ConnectionFacade) element;
      return connection.getBoundSonarProjects().toArray();
    }
    if (element instanceof SonarProject) {
      var project = (SonarProject) element;
      return ((ConnectionFacade) getParent(element)).getBoundProjects(project.getProjectKey()).toArray();
    }
    return new Object[0];
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof ISonarLintProject) {
      return SonarLintCorePlugin.getConnectionManager()
        .resolveBinding((ISonarLintProject) element)
        .flatMap(b -> b.getConnectionFacade().getCachedSonarProject(b.getProjectBinding().getProjectKey()))
        .orElse(null);
    }
    if (element instanceof SonarProject) {
      return SonarLintCorePlugin.getConnectionManager().findById(((SonarProject) element).getConnectionId()).orElse(null);
    }
    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    if (element instanceof ConnectionFacade) {
      return !((ConnectionFacade) element).getBoundProjects().isEmpty();
    }
    if (element instanceof SonarProject) {
      var project = (SonarProject) element;
      return !((ConnectionFacade) getParent(element)).getBoundProjects(project.getProjectKey()).isEmpty();
    }
    return false;
  }
}
