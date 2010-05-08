/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.views.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.sonar.ide.eclipse.Messages;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.ResourceQuery;

import java.util.List;

/**
 * @author Jérémie Lagarde
 */
public class TreeRoot extends TreeParent implements IDeferredWorkbenchAdapter {

  public TreeRoot() {
    super(null);
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public Sonar getServer() {
    return null;
  }

  @Override
  protected ResourceQuery createResourceQuery() {
    return null;
  }

  @Override
  public void fetchDeferredChildren(Object object,
                                    IElementCollector collector, IProgressMonitor monitor) {
    if (!(object instanceof TreeRoot)) {
      return;
    }
    TreeRoot node = (TreeRoot) object;
    List<Host> servers = SonarPlugin.getServerManager().getServers();
    monitor.beginTask(Messages.getString("pending"), servers.size()); //$NON-NLS-1$
    for (Host server : servers) {
      TreeServer treeServer = new TreeServer(server);
      node.addChild(treeServer);
    }
    monitor.done();
  }

  @Override
  public String getRemoteURL() {
    return null;
  }


}
