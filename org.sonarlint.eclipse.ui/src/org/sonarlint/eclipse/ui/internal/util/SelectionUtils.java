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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.core.resource.ISonarLintProjectContainer;

import static java.util.stream.Collectors.toList;

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
  @Nullable
  public static Object getSingleElement(ISelection s) {
    if (!(s instanceof IStructuredSelection)) {
      return null;
    }
    var selection = (IStructuredSelection) s;
    if (selection.size() != 1) {
      return null;
    }
    return selection.getFirstElement();
  }

  public static Set<ISonarLintProject> allSelectedProjects(ExecutionEvent event, boolean onlyIfProjectSupportsFullAnalysis) throws ExecutionException {
    var selection = HandlerUtil.getCurrentSelectionChecked(event);

    final var selectedProjects = new HashSet<ISonarLintProject>();

    if (selection instanceof IStructuredSelection) {
      var elems = ((IStructuredSelection) selection).toList();
      for (var elem : elems) {
        collectProjects(selectedProjects, elem, onlyIfProjectSupportsFullAnalysis);
      }
    }

    return selectedProjects;

  }

  private static void collectProjects(Set<ISonarLintProject> selectedProjects, Object elem, boolean onlyIfProjectSupportsFullAnalysis) {
    var container = Adapters.adapt(elem, ISonarLintProjectContainer.class);
    if (container != null) {
      selectedProjects.addAll(container.projects().stream()
        .filter(ISonarLintProject::isOpen)
        .filter(p -> !onlyIfProjectSupportsFullAnalysis || p.supportsFullAnalysis())
        .collect(toList()));
      return;
    }
    var project = Adapters.adapt(elem, ISonarLintProject.class);
    if (project != null && project.isOpen() && (!onlyIfProjectSupportsFullAnalysis || project.supportsFullAnalysis())) {
      selectedProjects.add(project);
    }
  }

  /**
   * Return all {@link ISonarLintFile} based on current selection.
   */
  public static Set<ISonarLintFile> allSelectedFiles(ISelection selection, boolean onlyIfProjectSupportsFullAnalysis) {
    final var selectedFiles = new HashSet<ISonarLintFile>();

    if (selection instanceof IStructuredSelection) {
      var elems = ((IStructuredSelection) selection).toList();
      for (var elem : elems) {
        collectFiles(selectedFiles, elem, onlyIfProjectSupportsFullAnalysis);
      }
    }

    return selectedFiles;

  }

  private static void collectFiles(Set<ISonarLintFile> selectedFiles, Object elem, boolean onlyIfProjectSupportsFullAnalysis) {
    // SLE-503 Start with the more specific to the more generic
    var file = Adapters.adapt(elem, ISonarLintFile.class);
    if (file != null) {
      selectedFiles.add(file);
      return;
    }
    var project = Adapters.adapt(elem, ISonarLintProject.class);
    if (project != null && project.isOpen() && (!onlyIfProjectSupportsFullAnalysis || project.supportsFullAnalysis())) {
      selectedFiles.addAll(project.files());
      return;
    }
    var container = Adapters.adapt(elem, ISonarLintProjectContainer.class);
    if (container != null) {
      container.projects().stream()
        .filter(ISonarLintProject::isOpen)
        .filter(p -> !onlyIfProjectSupportsFullAnalysis || p.supportsFullAnalysis())
        .forEach(p -> selectedFiles.addAll(p.files()));
    }
  }

  public static @Nullable IMarker findSelectedSonarLintMarker(ISelection selection) {
    try {
      if (selection instanceof IStructuredSelection) {
        var selectedSonarMarkers = new ArrayList<IMarker>();

        var elems = ((IStructuredSelection) selection).toList();
        for (var elem : elems) {
          processElement(selectedSonarMarkers, elem);
        }

        if (!selectedSonarMarkers.isEmpty()) {
          return selectedSonarMarkers.get(0);
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  private static void processElement(List<IMarker> selectedSonarMarkers, Object elem) throws CoreException {
    var marker = Adapters.adapt(elem, IMarker.class);
    if (marker != null && isSonarLintMarker(marker)) {
      selectedSonarMarkers.add(marker);
    }
  }

  private static boolean isSonarLintMarker(IMarker marker) throws CoreException {
    return MarkerUtils.SONARLINT_PRIMARY_MARKER_IDS.contains(marker.getType());
  }

}
