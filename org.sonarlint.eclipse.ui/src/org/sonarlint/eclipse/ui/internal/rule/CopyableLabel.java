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
package org.sonarlint.eclipse.ui.internal.rule;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 *  Workaround for a Label that supports selection and copy to clipboard.
 *  We don't use inheritance as it is discouraged in {@link StyledText} javadoc, and it makes testing with Reddeer more difficult.
 *  Instead we use composition.
 */
public class CopyableLabel extends Composite {

  private final StyledText text;

  public CopyableLabel(Composite parent) {
    super(parent, SWT.NONE);
    var layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    setLayout(layout);
    text = new StyledText(this, SWT.READ_ONLY);
    text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    text.setEditable(false);
    text.setCaret(null);
    text.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
  }

  public void setText(String name) {
    text.setText(name);
    layout();
  }

  @Override
  public void setFont(Font font) {
    text.setFont(font);
  }

}
