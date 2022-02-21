/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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

import java.util.Map;
import java.util.function.BiConsumer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

public final class PlatformUtils {

  private PlatformUtils() {
  }

  /**
   * Opens editor for given file.
   */
  public static void openEditor(IFile file) {
    var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try {
      IDE.openEditor(page, file);
    } catch (PartInitException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  /**
   * See http://wiki.eclipse.org/FAQ_How_do_I_open_an_editor_on_a_file_in_the_workspace%3F
   */
  public static void openEditor(IFile file, @Nullable Integer line) {
    if (line == null) {
      openEditor(file);
      return;
    }

    var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try {
      var marker = file.createMarker(IMarker.TEXT);
      marker.setAttributes(Map.of(IMarker.LINE_NUMBER, line));
      IDE.openEditor(page, marker);
      marker.delete();
    } catch (CoreException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
  }

  @Nullable
  public static IEditorPart findEditor(ISonarLintFile file) {
    var workbench = PlatformUI.getWorkbench();
    if (workbench == null) {
      return null;
    }
    for (var win : workbench.getWorkbenchWindows()) {
      for (var page : win.getPages()) {
        // handle the common case where the editor input is a FileEditorInput and ISonarLintFile wrap an IFile
        var result = findInFileEditorInput(file.getResource(), page);
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

  @Nullable
  private static IEditorPart findInFileEditorInput(IResource resource, IWorkbenchPage page) {
    if (resource instanceof IFile) {
      // Don't use page.findEditor(IEditorInput) because it will try to restore the editor before returning it
      var references = page.findEditors(new FileEditorInput((IFile) resource), null, IWorkbenchPage.MATCH_INPUT);
      if (references.length == 0) {
        return null;
      }
      return references[0].getEditor(false);
    }
    return null;
  }

  @Nullable
  private static IEditorPart findInOtherEditors(ISonarLintFile file, IWorkbenchPage page) {
    // check for editors that have their own kind of input that adapts to IFile,
    // being careful not to force loading of the editor
    for (var ref : page.getEditorReferences()) {
      var part = ref.getEditor(false);
      if (part == null) {
        continue;
      }
      var editorFile = Adapters.adapt(part.getEditorInput(), IFile.class);
      if (editorFile != null) {
        var editorSlFile = Adapters.adapt(editorFile, ISonarLintFile.class);
        if (editorSlFile != null && editorSlFile.equals(file)) {
          return part;
        }
      }
    }
    return null;
  }

  public static void doIfSonarLintFileInEditor(IWorkbenchPartReference partRef, BiConsumer<ISonarLintFile, IEditorPart> consumer) {
    var part = partRef.getPart(true);
    doIfSonarLintFileInEditor(part, consumer);
  }

  public static void doIfSonarLintFileInEditor(IWorkbenchPart part, BiConsumer<ISonarLintFile, IEditorPart> consumer) {
    if (part instanceof IEditorPart) {
      var editorPart = (IEditorPart) part;
      var input = editorPart.getEditorInput();
      if (input instanceof IFileEditorInput) {
        var file = ((IFileEditorInput) input).getFile();
        ISonarLintFile slFile = Adapters.adapt(file, ISonarLintFile.class);
        if (slFile != null) {
          consumer.accept(slFile, editorPart);
        }
      }
    }
  }

}
