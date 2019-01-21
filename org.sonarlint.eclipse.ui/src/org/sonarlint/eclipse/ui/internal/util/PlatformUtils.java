/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.util;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

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
      SonarLintLogger.get().error(e.getMessage(), e);
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
      Map<String, Object> map = new HashMap<>(1);
      map.put(IMarker.LINE_NUMBER, Integer.valueOf(line));
      IMarker marker = file.createMarker(IMarker.TEXT);
      marker.setAttributes(map);
      IDE.openEditor(page, marker);
      marker.delete();
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  @CheckForNull
  public static IEditorPart findEditor(ISonarLintFile file) {
    IWorkbench workbench = PlatformUI.getWorkbench();
    if (workbench == null) {
      return null;
    }
    for (IWorkbenchWindow win : workbench.getWorkbenchWindows()) {
      for (IWorkbenchPage page : win.getPages()) {
        // handle the common case where the editor input is a FileEditorInput and ISonarLintFile wrap an IFile
        IEditorPart result = findInFileEditorInput(file, page);
        if (result != null) {
          return result;
        }

        result = findInOtherEditors(file, page);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  @CheckForNull
  private static IEditorPart findInFileEditorInput(ISonarLintFile file, IWorkbenchPage page) {
    if (file.getResource() instanceof IFile) {
      // Don't use page.findEditor(IEditorInput) because it will try to restore the editor before returning it
      IEditorReference[] references = page.findEditors(new FileEditorInput((IFile) file.getResource()), null, IWorkbenchPage.MATCH_INPUT);
      if (references.length == 0) {
        return null;
      }
      return references[0].getEditor(false);
    }
    return null;
  }

  @CheckForNull
  private static IEditorPart findInOtherEditors(ISonarLintFile file, IWorkbenchPage page) {
    // check for editors that have their own kind of input that adapts to IFile,
    // being careful not to force loading of the editor
    for (IEditorReference ref : page.getEditorReferences()) {
      IEditorPart part = ref.getEditor(false);
      if (part == null) {
        continue;
      }
      IFile editorFile = Adapters.adapt(part.getEditorInput(), IFile.class);
      if (editorFile != null) {
        ISonarLintFile editorSlFile = Adapters.adapt(editorFile, ISonarLintFile.class);
        if (editorSlFile != null && editorSlFile.equals(file)) {
          return part;
        }
      }
    }
    return null;
  }
}
