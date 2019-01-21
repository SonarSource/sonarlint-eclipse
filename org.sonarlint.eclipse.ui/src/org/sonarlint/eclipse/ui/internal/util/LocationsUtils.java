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

import javax.annotation.CheckForNull;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;

public class LocationsUtils {

  private LocationsUtils() {
    // Utility class
  }

  @CheckForNull
  public static ITextEditor findOpenEditorFor(IMarker sonarlintMarker) {
    // Find IFile and open Editor
    // Super defensing programming because we don't really understand what is initialized at startup (SLE-122)
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    if (window == null) {
      return null;
    }
    IWorkbenchPage page = window.getActivePage();
    if (page == null) {
      return null;
    }
    for (IEditorReference editor : page.getEditorReferences()) {
      IEditorInput editorInput;
      try {
        editorInput = editor.getEditorInput();
      } catch (PartInitException e) {
        SonarLintLogger.get().error("Unable to restore editor", e);
        continue;
      }
      if (editorInput instanceof IFileEditorInput && ((IFileEditorInput) editorInput).getFile().equals(sonarlintMarker.getResource())) {
        IEditorPart editorPart = editor.getEditor(false);
        if (editorPart instanceof ITextEditor) {
          return (ITextEditor) editorPart;
        }
      }
    }
    return null;
  }
}
