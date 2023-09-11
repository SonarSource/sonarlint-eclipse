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
package org.sonarlint.eclipse.ui.internal.trim;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public class StatusWidget extends WorkbenchWindowControlContribution {

  @Nullable
  private Composite trimComposite = null;

  @Override
  public void dispose() {
    if (trimComposite != null && !trimComposite.isDisposed()) {
      trimComposite.dispose();
    }
    trimComposite = null;
  }

  @Override
  protected Control createControl(Composite parent) {
    // Create a composite to place the label in
    trimComposite = new Composite(parent, SWT.NONE);
    trimComposite.setLayout(createLayout());

    var icon = new Label(trimComposite, SWT.CENTER);
    icon.setImage(SonarLintImages.BALLOON_IMG);

    return trimComposite;
  }

  // Give some room around the control
  private RowLayout createLayout() {
    var type = getOrientation();
    var layout = new RowLayout(type);
    layout.marginTop = 2;
    layout.marginBottom = 2;
    layout.marginLeft = 2;
    layout.marginRight = 2;
    return layout;
  }

}
