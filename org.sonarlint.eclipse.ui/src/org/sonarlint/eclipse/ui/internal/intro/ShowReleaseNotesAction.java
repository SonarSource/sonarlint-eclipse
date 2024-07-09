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
package org.sonarlint.eclipse.ui.internal.intro;

import java.util.Properties;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.intro.config.IIntroAction;
import org.sonarlint.eclipse.ui.internal.properties.ReleaseNotesPage;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;

/** Action called from the "Welcome" view triggering the opening of the Release Notes preferences */
public class ShowReleaseNotesAction implements IIntroAction {
  @Override
  public void run(IIntroSite site, Properties params) {
    Display.getDefault().asyncExec(
      () -> PlatformUtils.showPreferenceDialog(ReleaseNotesPage.ABOUT_CONFIGURATION_ID).open());
  }
}
