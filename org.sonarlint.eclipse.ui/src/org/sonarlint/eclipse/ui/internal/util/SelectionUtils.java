/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectContainer;

public final class SelectionUtils {

  private SelectionUtils() {
  }

  /**
   * Returns the selected element if the selection consists of a single
   * element only.
   * 
   * @param s the selection
   * @return the selected first element or null
   */
  public static Object getSingleElement(ISelection s) {
    if (!(s instanceof IStructuredSelection)) {
      return null;
    }
    IStructuredSelection selection = (IStructuredSelection) s;
    if (selection.size() != 1) {
      return null;
    }
    return selection.getFirstElement();
  }

  public static Set<ISonarLintProject> allSelectedProjects(ExecutionEvent event) throws ExecutionException {
    ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

    final Set<ISonarLintProject> selectedProjects = new HashSet<>();

    if (selection instanceof IStructuredSelection) {
      List elems = ((IStructuredSelection) selection).toList();
      for (Object elem : elems) {
        if (elem instanceof IAdaptable) {
          collectProjects(selectedProjects, (IAdaptable) elem);
        }
      }
    }

    return selectedProjects;

  }

  private static void collectProjects(Set<ISonarLintProject> selectedProjects, IAdaptable elem) {
    ISonarLintProjectContainer container = (ISonarLintProjectContainer) elem.getAdapter(ISonarLintProjectContainer.class);
    if (container != null) {
      selectedProjects.addAll(container.projects());
      return;
    }
    ISonarLintProject project = (ISonarLintProject) elem.getAdapter(ISonarLintProject.class);
    if (project != null) {
      selectedProjects.add(project);
      return;
    }
  }

  /** 
   * Return all {@link ISonarLintFile} based on current selection.
   */
  public static Set<ISonarLintFile> allSelectedFiles(ExecutionEvent event) throws ExecutionException {
    ISelection selection = HandlerUtil.getCurrentSelectionChecked(event);

    final Set<ISonarLintFile> selectedFiles = new HashSet<>();

    if (selection instanceof IStructuredSelection) {
      List elems = ((IStructuredSelection) selection).toList();
      for (Object elem : elems) {
        if (elem instanceof IAdaptable) {
          collectFiles(selectedFiles, (IAdaptable) elem);
        }
      }
    }

    return selectedFiles;

  }

  private static void collectFiles(Set<ISonarLintFile> selectedFiles, IAdaptable elem) {
    ISonarLintProjectContainer container = (ISonarLintProjectContainer) elem.getAdapter(ISonarLintProjectContainer.class);
    if (container != null) {
      container.projects().forEach(p -> selectedFiles.addAll(p.files()));
      return;
    }
    ISonarLintProject project = (ISonarLintProject) elem.getAdapter(ISonarLintProject.class);
    if (project != null) {
      selectedFiles.addAll(project.files());
      return;
    }
    ISonarLintFile file = (ISonarLintFile) elem.getAdapter(ISonarLintFile.class);
    if (file != null) {
      selectedFiles.add(file);
      return;
    }
  }

}
