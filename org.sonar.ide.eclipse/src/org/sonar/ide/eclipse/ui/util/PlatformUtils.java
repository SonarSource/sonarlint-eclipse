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

package org.sonar.ide.eclipse.ui.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.sonar.ide.eclipse.core.SonarLogger;

@SuppressWarnings("unchecked")
public final class PlatformUtils {

  /**
   * Returns an object that is an instance of the given class associated
   * with the given object. Returns <code>null</code> if no such object can
   * be found or if given object is <code>null</code>.
   */
  public static <T> T adapt(Object adaptable, Class<T> adapter) {
    if (adaptable == null) {
      return null;
    }
    if (adapter.isInstance(adaptable)) {
      return (T) adaptable;
    }
    Object result = null;
    if (adaptable instanceof IAdaptable) {
      result = ((IAdaptable) adaptable).getAdapter(adapter);
    }
    if (result == null) {
      // From IAdapterManager :
      // this method should be used judiciously, in order to avoid unnecessary plug-in activations
      result = Platform.getAdapterManager().loadAdapter(adaptable, adapter.getName());
    }
    return (T) result;
  }

  /**
   * Opens editor for given file.
   */
  public static void openEditor(IFile file) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try {
      IDE.openEditor(page, file);
    } catch (PartInitException e) {
      SonarLogger.log(e);
    }
  }

  private PlatformUtils() {
  }

}
