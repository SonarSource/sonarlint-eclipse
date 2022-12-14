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
package org.sonarlint.eclipse.ui.internal.popup;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingProcess;
import org.sonarlint.eclipse.ui.internal.binding.wizard.project.ProjectBindingWizard;
import org.sonarsource.sonarlint.core.clientapi.backend.config.binding.BindingSuggestionDto;

public class SingleBindingSuggestionPopup extends AbstractSonarLintPopup {

  private final List<ISonarLintProject> projectsToBind;
  private final BindingSuggestionDto bindingSuggestionDto;

  public SingleBindingSuggestionPopup(List<ISonarLintProject> projectsToBind, BindingSuggestionDto bindingSuggestionDto) {
    this.projectsToBind = projectsToBind;
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
      ProjectBindingProcess.scheduleProjectBinding(bindingSuggestionDto.getConnectionId(), projectsToBind, bindingSuggestionDto.getSonarProjectKey());
      close();
    });

    addLinkWithTooltip("Select another", "Select another binding", e -> {
      close();
      final var dialog = ProjectBindingWizard.createDialog(getParentShell(), projectsToBind);
      dialog.open();
    });

    addLink("Learn more", e -> {
      try {
        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://github.com/SonarSource/sonarlint-eclipse/wiki/Connected-Mode"));
      } catch (PartInitException | MalformedURLException ex) {
        SonarLintLogger.get().error("Unable to open the browser", ex);
      }
      close();
    });

    addLink("Don't ask again", e -> {
      close();
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarLint Binding Suggestion";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }
}
