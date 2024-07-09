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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.PopupUtils;

/** Used for pop-ups that are related to the SonarLint version (e.g. Release Notes, newer version available) */
public abstract class AbstractSonarLintVersionPopup extends AbstractSonarLintPopup {
  private final String title;
  private final String message;

  protected AbstractSonarLintVersionPopup(String title, String message) {
    this.title = title;
    this.message = message;
  }

  @Override
  protected String getMessage() {
    return message;
  }

  @Override
  protected String getPopupShellTitle() {
    return title;
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLinks();
    addLink("Close", e -> close());

    composite.getShell().addDisposeListener(e -> PopupUtils.removeCurrentlyDisplayedPopup(getClass()));
  }

  protected abstract void addLinks();
}
