/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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

import java.util.Collection;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.util.SelectionUtils;

/**
 * Command class for toggling the editor open analysis flag of each selected
 * project so that analysis does not occur on each editor window opened when
 * <i>false</i>. If <i>true</i> normal behavior will continue.
 *
 */
public class ToggleTriggerEditorOpenCommand extends AbstractHandler {

  /**
   * 
   */
  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    Collection<ISonarLintProject> selectedProjects = SelectionUtils.allSelectedProjects(event);
    ToggleUtils.updateToggleState(selectedProjects, TriggerType.EDITOR_OPEN);

    return null;
  }

}
