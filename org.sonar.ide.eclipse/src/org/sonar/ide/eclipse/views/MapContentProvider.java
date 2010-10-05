/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.views;

import java.util.Collection;
import java.util.Map;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * @author Evgeny Mandrikov
 */
public class MapContentProvider implements ITreeContentProvider {

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public Object[] getChildren(Object parentElement) {
    if (parentElement instanceof Map) {
      return ((Map) parentElement).entrySet().toArray();
    }
    if (parentElement instanceof Map.Entry) {
      Map.Entry entry = (Map.Entry) parentElement;
      Object value = entry.getValue();
      if (value instanceof Collection) {
        return ((Collection) value).toArray();
      }
      return new Object[] { value };
    }
    return new Object[0];
  }

  /**
   * {@inheritDoc}
   */
  public Object getParent(Object element) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  public boolean hasChildren(Object element) {
    return getChildren(element).length > 0;
  }

  /**
   * {@inheritDoc}
   */
  public Object[] getElements(Object inputElement) {
    return getChildren(inputElement);
  }

  /**
   * {@inheritDoc}
   */
  public void dispose() {
  }

  /**
   * {@inheritDoc}
   */
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
  }

}
