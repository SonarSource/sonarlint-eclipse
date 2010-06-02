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

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.sonar.wsclient.services.Metric;

/**
 * @author Jérémie Lagarde
 */
public class MetricsContentProvider implements IStructuredContentProvider {

  public Object[] getElements(final Object inputElement) {
    @SuppressWarnings("unchecked")
    final List<Metric> metrics = (List<Metric>) inputElement;
    return metrics.toArray();
  }

  public void dispose() {
  }

  public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
  }

}


