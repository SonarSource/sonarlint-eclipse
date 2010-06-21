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
import org.eclipse.ui.progress.IElementCollector;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.wsclient.Host;

/**
 * @author Jérémie Lagarde
 */
public class TreeServer extends TreeParent {

  private final Host host;

  public TreeServer(Host host) {
    super(null);
    this.host = host;
  }

  @Override
  public String getName() {
    return host.getHost();
  }

  @Override
  public String getRemoteURL() {
    return host.getHost();
  }

  @Override
  public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
    if ( !(object instanceof TreeServer)) {
      return;
    }
    for (SourceCode child : new EclipseSonar(host).getProjects()) {
      TreeObject treeObject = new TreeProject(child);
      addChild(treeObject);
      // node.addChild(treeObject);
      collector.add(treeObject, monitor);
      monitor.worked(1);
    }
    monitor.done();
  }

}
