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

package org.sonar.ide.eclipse.views;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.views.model.TreeObject;
import org.sonar.ide.eclipse.views.model.TreeParent;
import org.sonar.ide.eclipse.views.model.TreeRoot;
import org.sonar.ide.eclipse.views.model.TreeServer;
import org.sonar.wsclient.Host;

import java.util.List;

/**
 * @author Jérémie Lagarde
 */
public class NavigatorContentProvider implements IStructuredContentProvider, ITreeContentProvider {

  private TreeRoot invisibleRoot;
  private DeferredTreeContentManager manager;
  private AbstractTreeViewer viewer;

  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (viewer instanceof AbstractTreeViewer) {
      this.viewer = (AbstractTreeViewer) viewer;
      this.manager = new DeferredTreeContentManager(this.viewer);
    }
  }

  public void dispose() {
  }

  public Object[] getElements(Object parent) {
    if (parent.equals(viewer)) {
      if (invisibleRoot == null)
        initialize();
      return getChildren(invisibleRoot);
    }
    return getChildren(parent);
  }

  public Object getParent(Object child) {
    if (child instanceof TreeObject) {
      return ((TreeObject) child).getParent();
    }
    return null;
  }

  public Object[] getChildren(Object parent) {
    if (parent == invisibleRoot) {
      return ((TreeParent) invisibleRoot).getChildren();
    } else if (parent instanceof TreeParent) {
      return manager.getChildren(parent);
    }
    return new Object[0];
  }

  public boolean hasChildren(Object parent) {
    if (parent instanceof TreeParent) {
      return manager.mayHaveChildren(parent);
    }
    return false;
  }

  private void initialize() {
    invisibleRoot = new TreeRoot();
    List<Host> servers = SonarPlugin.getServerManager().getServers();
    for (Host server : servers) {
      TreeServer treeServer = new TreeServer(server);
      invisibleRoot.addChild(treeServer);
    }

  }

  //
  // public ImageDescriptor getImageDescriptor(Object arg0) {
  // return null;
  // }
  //
  // public String getLabel(Object arg0) {
  // return null;
  // }

  public TreeObject find(String name) {
    TreeObject object = find(this.invisibleRoot, name);
    if (object == null) {
      TreeObject[] chidren = invisibleRoot.getChildren();
      for (int i = 0; i < chidren.length; i++) {
        find(((TreeServer) chidren[i]), name.substring(0, name.length() - 5));
      }
    }
    return object;
  }

  public TreeObject find(TreeParent parent, String name) {
    if (parent == null || name == null)
      return null;
    TreeObject[] children = parent.getChildren();
    for (int i = 0; i < children.length; i++) {
      if (children[i].getResource() != null && name.equals(children[i].getResource().getName() + ".java"))
        return children[i];
      if (children[i] instanceof TreeParent) {
        TreeObject object = find((TreeParent) children[i], name);
        if (object != null)
          return object;
      }
    }
    return null;
  }

}
