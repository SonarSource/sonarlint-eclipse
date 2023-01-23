/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.ui.handlers.HandlerUtil;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem.Type;
import org.sonarlint.eclipse.core.internal.utils.FileExclusionsChecker;
import org.sonarlint.eclipse.ui.internal.util.SelectionUtils;

public class ExcludeCommand extends AbstractHandler {

  @Nullable
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    for (var file : SelectionUtils.allSelectedFiles(HandlerUtil.getCurrentSelectionChecked(event), false)) {
      var project = file.getProject();
      if (!FileExclusionsChecker.isPathAlreadyExcludedInProject(file)) {
        FileExclusionsChecker.addProjectFileExclusion(project, file, new ExclusionItem(Type.FILE, file.getProjectRelativePath()));
      }
    }
    return null;
  }
}
