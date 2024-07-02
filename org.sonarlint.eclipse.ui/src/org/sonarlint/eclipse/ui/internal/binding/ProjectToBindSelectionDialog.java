/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public class ProjectToBindSelectionDialog extends ElementListSelectionDialog {

  private ProjectToBindSelectionDialog(Shell parent, String message, List<ISonarLintProject> projects) {
    super(parent, new SonarLintProjectLabelProvider());
    setElements(projects.toArray());
    setTitle("SonarLint - Project Selection");
    setMessage(message);
    setHelpAvailable(false);
  }

  @Nullable
  public static ISonarLintProject pickProject(String projectKey, String connectionId) {
    var projects = SonarLintUtils.allProjects()
      .stream()
      .sorted(Comparator.comparing(ISonarLintProject::getName))
      .collect(Collectors.toList());
    var dialog = new ProjectToBindSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
      "Select a project.\nThis Eclipse project will be bound to the Sonar project '" + projectKey + "' using connection '" + connectionId + "'", projects);
    if (dialog.open() == Window.OK) {
      return (ISonarLintProject) dialog.getResult()[0];
    }
    return null;
  }

  public static List<ISonarLintProject> selectProjectsToAdd(Shell parent, List<ISonarLintProject> alreadySelected) {
    var projects = SonarLintUtils.allProjects()
      .stream()
      .filter(p -> !alreadySelected.contains(p))
      .sorted(comparing(ISonarLintProject::getName))
      .collect(toList());
    var dialog = new ProjectToBindSelectionDialog(parent, "Select projects to add:", projects);
    dialog.setMultipleSelection(true);
    if (dialog.open() == Window.OK) {
      return Stream.of(dialog.getResult()).map(ISonarLintProject.class::cast).collect(Collectors.toList());
    }
    return List.of();
  }

}
