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
package org.sonarlint.eclipse.ui.internal.popup;

import java.util.List;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingWizard;

public class BindingSuggestionPopup extends AbstractBindingSuggestionPopup {

  public BindingSuggestionPopup(List<ISonarLintProject> projectsToBind) {
    super(projectsToBind);
  }

  @Override
  protected String getMessage() {
    if (projectsToBind.size() == 1) {
      return "Bind project " + projectsToBind.iterator().next().getName() + "'?";
    } else {
      return "Bind " + projectsToBind.size() + " projects?";
    }
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLinkWithTooltip("Bind", "Select binding", e -> {
      close();
      final var dialog = ProjectBindingWizard.createDialog(getParentShell(), projectsToBind);
      dialog.open();
    });

    addLearnMoreLink();

    addDontAskAgainLink();
  }
}
