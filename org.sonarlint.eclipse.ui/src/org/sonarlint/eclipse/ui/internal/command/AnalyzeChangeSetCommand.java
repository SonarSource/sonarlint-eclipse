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
package org.sonarlint.eclipse.ui.internal.command;

import java.util.Arrays;
import java.util.Collection;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.sonarlint.eclipse.core.internal.jobs.AnalyzeProjectRequest.FileWithDocument;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.util.SelectionUtils;
import org.sonarlint.eclipse.ui.internal.views.issues.ChangeSetIssuesView;

public class AnalyzeChangeSetCommand extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Collection<ISonarLintProject> selectedProjects = SelectionUtils.allSelectedProjects(event);

    if (selectedProjects.isEmpty()) {
      FileWithDocument editedFile = AnalyzeCommand.findEditedFile(event);
      if (editedFile != null) {
        selectedProjects = Arrays.asList(editedFile.getFile().getProject());
      }
    }

    ChangeSetIssuesView.triggerAnalysis(selectedProjects);

    return null;
  }

}
