/*
 * SonarLint for Eclipse
 * Copyright (C) SonarSource Sàrl
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
import org.sonarlint.eclipse.core.internal.utils.JavaRuntimeUtils;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.preferences.SonarLintPreferencePage;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;

/** Popup warning users that their configured custom JRE is below the minimum required version */
public class InvalidJrePopup extends AbstractSonarLintPopup {

  private final int detectedVersion;

  public InvalidJrePopup(int detectedVersion) {
    this.detectedVersion = detectedVersion;
  }

  @Override
  protected String getMessage() {
    return "The configured Java runtime (Java " + detectedVersion + ") is below the minimum required version "
      + "(Java " + JavaRuntimeUtils.MINIMUM_JRE_VERSION + "+). SonarQube for Eclipse will not work correctly. "
      + "Please update the JRE path in the SonarQube for Eclipse settings.";
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);
    addLink("Open Settings", e -> {
      close();
      PlatformUtils.showPreferenceDialog(SonarLintPreferencePage.ID).open();
    });
    addLink("Learn more", e -> {
      BrowserUtils.openExternalBrowser(SonarLintDocumentation.PROVIDE_JAVA_RUNTIME_LINK, composite.getDisplay());
      close();
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarQube for Eclipse - Java Runtime";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.IMG_ERROR;
  }
}
