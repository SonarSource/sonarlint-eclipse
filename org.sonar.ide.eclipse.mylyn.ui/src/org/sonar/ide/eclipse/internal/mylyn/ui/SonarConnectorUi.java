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
package org.sonar.ide.eclipse.internal.mylyn.ui;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.mylyn.tasks.core.*;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskRepositoryPage;
import org.eclipse.mylyn.tasks.ui.wizards.RepositoryQueryWizard;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarConnector;
import org.sonar.ide.eclipse.internal.mylyn.ui.wizard.SonarQueryPage;
import org.sonar.ide.eclipse.internal.mylyn.ui.wizard.SonarRepositorySettingsPage;

public class SonarConnectorUi extends AbstractRepositoryConnectorUi {

  @Override
  public String getConnectorKind() {
    return SonarConnector.CONNECTOR_KIND;
  }

  @Override
  public ITaskRepositoryPage getSettingsPage(TaskRepository repository) {
    return new SonarRepositorySettingsPage(repository);
  }

  @Override
  public IWizard getQueryWizard(TaskRepository repository, IRepositoryQuery queryToEdit) {
    RepositoryQueryWizard wizard = new RepositoryQueryWizard(repository);
    wizard.addPage(new SonarQueryPage(repository, queryToEdit));
    return wizard;
  }

  @Override
  public IWizard getNewTaskWizard(TaskRepository repository, ITaskMapping selection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasSearchPage() {
    return false;
  }

  @Override
  public String getReplyText(TaskRepository taskRepository, ITask task, ITaskComment taskComment, boolean includeTask) {
    return "";
  }

  @Override
  public String getTaskKindLabel(ITask task) {
    return Messages.SonarConnectorUi_Review;
  }

}
