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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.ui.wizards.AbstractTaskRepositoryPage;
import org.eclipse.swt.widgets.Composite;

public class SonarSettingsPage extends AbstractTaskRepositoryPage {

  public SonarSettingsPage(String title, String description, TaskRepository taskRepository) {
    super(title, description, taskRepository);
    // TODO Auto-generated constructor stub
  }

  @Override
  public String getConnectorKind() {
    // TODO Auto-generated method stub
    return "Sonar Mylyn Kind";
  }

  @Override
  protected void createSettingControls(Composite parent) {
    // TODO Auto-generated method stub
  }

  @Override
  protected IStatus validate() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getRepositoryUrl() {
    // TODO Auto-generated method stub
    return null;
  }

}
