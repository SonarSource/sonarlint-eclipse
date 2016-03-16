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
package org.sonarlint.eclipse.ui.internal.server;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.core.internal.server.ServersManager;

public class ServerContentProvider extends BaseContentProvider implements ITreeContentProvider {

  @Override
  public Object[] getElements(Object element) {
    return ServersManager.getInstance().getServers().toArray();
  }

  @Override
  public Object[] getChildren(Object element) {
    if (element instanceof IServer) {
      List<IProject> projects = new ArrayList<>();
      for (SonarLintProject p : ((IServer) element).getBoundProjects()) {
        projects.add(p.getProject());
      }
      return projects.toArray();
    }
    return new Object[0];
  }

  @Override
  public Object getParent(Object element) {
    if (element instanceof IProject) {
      String serverId = SonarLintProject.getInstance((IProject) element).getServerId();
      return ServersManager.getInstance().getServer(serverId);
    }
    return null;
  }

  @Override
  public boolean hasChildren(Object element) {
    if (element instanceof IServer) {
      return !((IServer) element).getBoundProjects().isEmpty();
    }
    return false;
  }
}
