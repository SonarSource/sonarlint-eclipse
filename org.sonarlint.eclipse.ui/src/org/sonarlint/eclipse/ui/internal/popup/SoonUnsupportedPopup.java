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
package org.sonarlint.eclipse.ui.internal.popup;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;

/** Popup indicating that there is a connection in the workspace to a soon unsupported SonarQube version */
public class SoonUnsupportedPopup extends AbstractSonarLintPopup {
  private final String doNotShowAgainId;
  private final String message;
  
  public SoonUnsupportedPopup(String doNotShowAgainId, String message) {
    this.doNotShowAgainId = doNotShowAgainId;
    this.message = message;
  }

  @Override
  protected String getMessage() {
    return message;
  }
  
  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Don't show again", e -> {
      SonarLintGlobalConfiguration.addSoonUnsupportedConnection(doNotShowAgainId);
      BrowserUtils.openExternalBrowser(SonarLintDocumentation.VERSION_SUPPORT_POLICY, getShell().getDisplay());
      close();
    });
  }
  
  @Override
  protected String getPopupShellTitle() {
    return "SonarQube - Soon unsupported version";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.SONARQUBE_SERVER_ICON_IMG;
  }
}
