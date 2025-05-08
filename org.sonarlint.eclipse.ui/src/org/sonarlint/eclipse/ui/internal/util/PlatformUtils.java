/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public final class PlatformUtils {

  private PlatformUtils() {
  }

  /** To open the "Welcome" view, which is actually an Eclipse E4 part and not a Eclipse 3.x view! */
  public static void openWelcomePage() {
    Display.getDefault().asyncExec(() -> {
      try {
        // INFO: This information cannot be accessed from any constant, can be seen via "Plug-in Selection Spy"!
        showView("org.eclipse.ui.internal.introview");
      } catch (Exception err) {
        // Some Eclipse based applications do not provide the Welcome view at all (which would throw a
        // PartInitException) and some do not come with the default intro pages we extend and that will be opened by
        // default by the Welcome view (which will throw a NullPointerException). There might be more cases, and for
        // that very reason we catch all kinds of exceptions here due to it being out of our scope and just log a debug
        // message, no need for a confusing error.
        SonarLintLogger.get().debug("Cannot open the 'Welcome' view", err);
      }
    });
  }

  /** Show a specific view (open it if not already in the workspace, otherwise bring to front) */
  public static IViewPart showView(String id) throws PartInitException {
    return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(id);
  }

  /** Show a specific preference dialog */
  public static PreferenceDialog showPreferenceDialog(String id) {
    return PreferencesUtil.createPreferenceDialogOn(Display.getCurrent().getActiveShell(), id, null, null);
  }

  /**
   * Opens editor for given file.
   */
  @Nullable
  public static IEditorPart openEditor(IFile file) {
    IEditorPart part = null;
    var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try {
      part = IDE.openEditor(page, file);
    } catch (PartInitException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
    return part;
  }

  /**
   *  Opens editor for given marker.
   */
  @Nullable
  public static IEditorPart openEditor(IMarker marker) {
    IEditorPart part = null;
    var page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    try {
      part = IDE.openEditor(page, marker);
    } catch (PartInitException e) {
      SonarLintLogger.get().error(e.getMessage(), e);
    }
    return part;
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

  public static IDocument getDocumentFromEditorOrFile(ISonarLintFile file) {
    IDocument doc;
    var editorPart = findEditor(file);
    if (editorPart instanceof ITextEditor) {
      doc = ((ITextEditor) editorPart).getDocumentProvider().getDocument(editorPart.getEditorInput());
    } else {
      doc = file.getDocument();
    }
    return doc;
  }

  @Nullable
  public static IEditorPart findEditor(ISonarLintFile file) {
    if (!PlatformUI.isWorkbenchRunning()) {
      // Likely in headless tests
      return null;
    }
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
      var editorFile = SonarLintUtils.adapt(part.getEditorInput(), IFile.class,
        "[PlatformUtils#findInOtherEditors] Try get Eclipse file of editor input '" + part.getTitle() + "'");
      if (editorFile != null) {
        var editorSlFile = SonarLintUtils.adapt(editorFile, ISonarLintFile.class,
          "[PlatformUtils#findInOtherEditors] Try get file of Eclipse file '" + editorFile.getName() + "'");
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
        var slFile = SonarLintUtils.adapt(file, ISonarLintFile.class,
          "[PlatformUtils#doIfSonarLintFileInEditor] Try get file of editor input '" + file.getName() + "'");

        // We have to check the charset (and internally the encoding is also checked for XML files) here as well as
        // for unsupported charsets the Eclipse builtin editor already fails to open the file but will trigger the
        // "IPartListener2#partOpened" nevertheless. When Eclipse (and internally the JVM) cannot even handle the file,
        // we don't even try to do so.
        if (slFile != null && SonarLintUtils.hasSupportedCharset(slFile)) {
          consumer.accept(slFile, editorPart);
        }
      }
    }
  }

  public static Map<ISonarLintProject, List<FileWithDocument>> collectOpenedFiles(@Nullable ISonarLintProject project, Predicate<ISonarLintFile> filter) {
    if (!PlatformUI.isWorkbenchRunning()) {
      // headless tests
      return Map.of();
    }
    var filesByProject = new HashMap<ISonarLintProject, List<FileWithDocument>>();
    for (var win : PlatformUI.getWorkbench().getWorkbenchWindows()) {
      for (var page : win.getPages()) {
        for (var ref : page.getEditorReferences()) {
          collectOpenedFile(project, filesByProject, ref, filter);
        }
      }
    }
    return filesByProject;
  }

  private static void collectOpenedFile(@Nullable ISonarLintProject project, Map<ISonarLintProject, List<FileWithDocument>> filesByProject,
    IEditorReference ref, Predicate<ISonarLintFile> filter) {
    // Be careful to not trigger editor activation
    var editor = ref.getEditor(false);
    if (editor == null) {
      return;
    }
    var input = editor.getEditorInput();
    if (input instanceof IFileEditorInput) {
      var file = ((IFileEditorInput) input).getFile();
      var sonarFile = SonarLintUtils.adapt(file, ISonarLintFile.class,
        "[PlatformUtils#collectOpenedFile] Try get file of editor input '" + file.getName() + "'");
      if (sonarFile != null && (project == null || sonarFile.getProject().equals(project)) && filter.test(sonarFile)) {
        filesByProject.putIfAbsent(sonarFile.getProject(), new ArrayList<>());
        if (editor instanceof ITextEditor) {
          var doc = ((ITextEditor) editor).getDocumentProvider().getDocument(input);
          filesByProject.get(sonarFile.getProject()).add(new FileWithDocument(sonarFile, doc));
        } else {
          filesByProject.get(sonarFile.getProject()).add(new FileWithDocument(sonarFile, null));
        }
      }
    }
  }

  /**
   * This is a copy of {@link org.eclipse.debug.internal.ui.SWTFactory} in order to create a separator
   * between two UI elements, mostly to be used in preference pages.
   */
  public static void createHorizontalSpacer(Composite comp, int numlines) {
    var lbl = new Label(comp, SWT.NONE);
    var gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = numlines;
    lbl.setLayoutData(gd);
  }
}
