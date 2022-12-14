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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public abstract class AbstractBindingSuggestionPopup extends AbstractSonarLintPopup {

  protected final List<ISonarLintProject> projectsToBind;

  protected AbstractBindingSuggestionPopup(List<ISonarLintProject> projectsToBind) {
    this.projectsToBind = projectsToBind;
  }

  protected void addLearnMoreLink() {
    addLink("Learn more", e -> {
      try {
        PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://github.com/SonarSource/sonarlint-eclipse/wiki/Connected-Mode"));
      } catch (PartInitException | MalformedURLException ex) {
        SonarLintLogger.get().error("Unable to open the browser", ex);
      }
      close();
    });
  }

  protected void addDontAskAgainLink() {
    addLink("Don't ask again", e -> {
      close();
      projectsToBind.forEach(p -> {
        var config = SonarLintCorePlugin.loadConfig(p);
        config.setBindingSuggestionsDisabled(true);
        SonarLintCorePlugin.saveConfig(p, config);
      });
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
