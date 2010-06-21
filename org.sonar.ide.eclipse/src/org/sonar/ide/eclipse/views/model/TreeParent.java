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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.sonar.ide.api.SourceCode;
import org.sonar.ide.eclipse.Messages;

/**
 * @author Jérémie Lagarde
 */
public abstract class TreeParent extends TreeObject implements IDeferredWorkbenchAdapter {

  private final List<TreeObject> children;

  public TreeParent(SourceCode sourceCode) {
    super(sourceCode);
    children = new ArrayList<TreeObject>();
  }

  public void addChild(TreeObject child) {
    children.add(child);
    child.setParent(this);
  }

  public void removeChild(TreeObject child) {
    children.remove(child);
    child.setParent(null);
  }

  public TreeObject[] getChildren() {
    return children.toArray(new TreeObject[children.size()]);
  }

  public boolean hasChildren() {
    return children.size() > 0;
  }

  /**
   * {@inheritDoc}
   */
  public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
    if ( !(object instanceof TreeParent)) {
      return;
    }
    monitor.beginTask(Messages.getString("pending"), 1); //$NON-NLS-1$
    monitor.worked(1);
    for (SourceCode child : sourceCode.getChildren()) {
      TreeObject treeObject = TreeElementFactory.create(child);
      this.addChild(treeObject);
      collector.add(treeObject, monitor);
      monitor.worked(1);
    }
    monitor.done();
  }

  public ISchedulingRule getRule(Object object) {
    return null;
  }

  public boolean isContainer() {
    return true;
  }

  public Object[] getChildren(Object element) {
    if (element instanceof TreeParent) {
      return ((TreeParent) element).getChildren();
    } else {
      return null;
    }
  }

  public ImageDescriptor getImageDescriptor(Object object) {
    return null;
  }

  public String getLabel(Object element) {
    if (element instanceof TreeParent) {
      return ((TreeParent) element).getName();
    } else {
      return element.toString();
    }
  }

  public Object getParent(Object element) {
    if (element instanceof TreeParent) {
      return ((TreeParent) element).getParent();
    } else {
      return null;
    }
  }

}
