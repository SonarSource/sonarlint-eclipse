/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.ide.IDE;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;

@SuppressWarnings("unchecked")
public final class PlatformUtils {

  /**
   * Returns an object that is an instance of the given class associated with the given object. Returns <code>null</code> if no such object
   * can be found or if given object is <code>null</code>.
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
      LoggerFactory.getLogger(PlatformUtils.class).error(e.getMessage(), e);
    }
  }

  /**
   * See http://wiki.eclipse.org/FAQ_How_do_I_open_an_editor_on_a_file_in_the_workspace%3F
   */
  public static void openEditor(IFile file, Integer line) {
    if (line == null) {
      openEditor(file);
      return;
    }

    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try {
      HashMap map = new HashMap(2);
      map.put(IMarker.LINE_NUMBER, new Integer(line));
      IMarker marker = file.createMarker(IMarker.TEXT);
      marker.setAttributes(map);
      IDE.openEditor(page, marker);
      marker.delete();
    } catch (PartInitException e) {
      LoggerFactory.getLogger(PlatformUtils.class).error(e.getMessage(), e);
    } catch (CoreException e) {
      LoggerFactory.getLogger(PlatformUtils.class).error(e.getMessage(), e);
    }
  }

  public static String convertMementoToString(XMLMemento memento) {
    String result = null;
    try {
      StringWriter writer = new StringWriter();
      memento.save(writer);
      result = writer.getBuffer().toString();
    } catch (IOException e) {
      LoggerFactory.getLogger(PlatformUtils.class).error(e.getMessage(), e);
    }
    return result;
  }

  private PlatformUtils() {
  }

}
