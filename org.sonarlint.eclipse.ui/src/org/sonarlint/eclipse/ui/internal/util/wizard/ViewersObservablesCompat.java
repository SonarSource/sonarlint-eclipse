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
package org.sonarlint.eclipse.ui.internal.util.wizard;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.viewers.IViewerObservableValue;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;

public class ViewersObservablesCompat {
  public static <S extends ISelectionProvider, E> IViewerObservableValue<E> observeSingleSelection(Viewer viewer) {
    if (JFaceUtils.IS_TYPED_API_SUPPORTED) {
      return ViewerProperties.<S, E>singleSelection().observe(viewer);
    }
    try {
      var viewersObservablesClass = Class.forName("org.eclipse.jface.databinding.viewers.ViewersObservables");
      var observeSingleSelectionMethod = viewersObservablesClass.getMethod("observeSingleSelection", Viewer.class);
      return (IViewerObservableValue<E>) observeSingleSelectionMethod.invoke(null, viewer);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to call deprecated method", e);
    }
  }

  public static IObservableValue<Object> observeInput(Viewer viewer) {
    if (JFaceUtils.IS_TYPED_API_SUPPORTED) {
      return ViewerProperties.<StructuredViewer, Object>input().observe(viewer);
    }
    try {
      var viewersObservablesClass = Class.forName("org.eclipse.jface.databinding.viewers.ViewersObservables");
      var observeInputMethod = viewersObservablesClass.getMethod("observeInput", Viewer.class);
      return (IObservableValue<Object>) observeInputMethod.invoke(null, viewer);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to call deprecated method", e);
    }
  }

  private ViewersObservablesCompat() {
    // utility class
  }
}
