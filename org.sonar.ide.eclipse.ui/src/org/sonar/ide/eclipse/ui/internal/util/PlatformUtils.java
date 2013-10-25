/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.util;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public final class PlatformUtils {

  private PlatformUtils() {
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
      Map<String, Object> map = new HashMap<String, Object>(1);
      map.put(IMarker.LINE_NUMBER, Integer.valueOf(line));
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

}
