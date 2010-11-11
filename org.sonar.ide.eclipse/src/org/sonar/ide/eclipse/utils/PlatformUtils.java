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

package org.sonar.ide.eclipse.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.sonar.ide.eclipse.core.SonarLogger;

@SuppressWarnings("unchecked")
public class PlatformUtils {

  public static <T> T adapt(Object object, Class<T> cls) {
    if (cls.isInstance(object)) {
      return (T) object;
    }
    T result = null;
    if (object instanceof IAdaptable) {
      result = (T) ((IAdaptable) object).getAdapter(cls);
    }
    if (result == null) {
      // From IAdapterManager :
      // this method should be used judiciously, in order to avoid unnecessary plug-in activations
      result = (T) Platform.getAdapterManager().loadAdapter(object, cls.getName());
    }
    return result;
  }

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
