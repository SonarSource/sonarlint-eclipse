/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.internal.mylyn.ui.wizard;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractRepositoryQueryPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.sonar.ide.eclipse.internal.mylyn.ui.Messages;

public class SonarQueryPage extends AbstractRepositoryQueryPage {

  public SonarQueryPage(TaskRepository repository, IRepositoryQuery queryToEdit) {
    super(Messages.SonarQueryPage_Title /* page name */, repository, queryToEdit);
    setTitle(Messages.SonarQueryPage_Title);
    setDescription(Messages.SonarQueryPage_Description);
  }

  public void createControl(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    // TODO Auto-generated method stub

    Dialog.applyDialogFont(composite);
    setControl(composite);
  }

  @Override
  public String getQueryTitle() {
    // TODO Auto-generated method stub
    return getTaskRepository().getRepositoryLabel();
  }

  @Override
  public void applyTo(IRepositoryQuery query) {
    // TODO Auto-generated method stub
  }

}
