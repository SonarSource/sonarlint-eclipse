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
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingProcess;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingWizard;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.config.binding.BindingSuggestionDto;

public class SingleBindingSuggestionPopup extends AbstractBindingSuggestionPopup {

  private final BindingSuggestionDto bindingSuggestionDto;

  public SingleBindingSuggestionPopup(List<ISonarLintProject> projectsToBind, BindingSuggestionDto bindingSuggestionDto) {
    super(projectsToBind);
    this.bindingSuggestionDto = bindingSuggestionDto;
  }

  @Override
  protected String getMessage() {
    if (projectsToBind.size() == 1) {
      return "Bind project " + projectsToBind.iterator().next().getName() +
        " to '" + bindingSuggestionDto.getSonarProjectName() + "' on '" + bindingSuggestionDto.getConnectionId() + "'?";
    } else {
      return "Bind " + projectsToBind.size() +
        " projects to '" + bindingSuggestionDto.getSonarProjectName() + "' on '" + bindingSuggestionDto.getConnectionId() + "'?";
    }
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLinkWithTooltip("Bind", "Accept suggested binding", e -> {
      ProjectBindingProcess.bindProjects(bindingSuggestionDto.getConnectionId(), projectsToBind, bindingSuggestionDto.getSonarProjectKey());

      if (SonarLintTelemetry.isEnabled()) {
        if (bindingSuggestionDto.isFromSharedConfiguration()) {
          SonarLintTelemetry.addedImportedBindings();
        } else {
          SonarLintTelemetry.addedAutomaticBindings();
        }
      }

      close();
    });

    addLinkWithTooltip("Select another", "Select another binding", e -> {
      close();
      final var dialog = ProjectBindingWizard.createDialog(getParentShell(), projectsToBind);
      dialog.open();
    });

    addLearnMoreLink();

    addDontAskAgainLink();
  }
}
