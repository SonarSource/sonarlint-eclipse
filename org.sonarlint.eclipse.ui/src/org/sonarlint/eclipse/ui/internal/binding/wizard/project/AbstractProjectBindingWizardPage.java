/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding.wizard.project;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public abstract class AbstractProjectBindingWizardPage extends WizardPage {

  protected final ProjectBindingModel model;
  private final int numCols;

  protected AbstractProjectBindingWizardPage(String pageName, String title, ProjectBindingModel model, int numCols) {
    super(pageName, title, SonarLintImages.IMG_WIZBAN_NEW_CONNECTION);
    this.model = model;
    this.numCols = numCols;
  }

  @Override
  public final void createControl(Composite parent) {
    var container = new Composite(parent, SWT.NONE);

    var layout = new GridLayout();
    layout.numColumns = numCols;
    container.setLayout(layout);

    var layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
    container.setLayoutData(layoutData);

    doCreateControl(container);

    setControl(container);

    ((WizardDialog) getContainer()).addPageChangedListener((ProjectBindingWizard) getWizard());
  }

  protected abstract void doCreateControl(Composite container);

}
