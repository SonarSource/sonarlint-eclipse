/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.mylyn3;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylyn.tasks.ui.wizards.ITaskRepositoryPage;


public class SonarConnectorUi extends AbstractRepositoryConnectorUi {

  @Override
  public String getConnectorKind() {
    // TODO Auto-generated method stub
    return "Sonar Mylyn Kind";
  }

  @Override
  public IWizard getNewTaskWizard(TaskRepository repository, ITaskMapping taskSelection) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public IWizard getQueryWizard(TaskRepository repository, IRepositoryQuery query) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ITaskRepositoryPage getSettingsPage(TaskRepository repository) {
    // TODO Auto-generated method stub
    return new SonarSettingsPage("title", "description", repository);
  }

  @Override
  public boolean hasSearchPage() {
    // TODO Auto-generated method stub
    return false;
  }

}
