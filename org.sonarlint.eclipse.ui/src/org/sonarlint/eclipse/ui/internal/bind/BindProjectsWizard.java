/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.bind;

import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;

/**
 * Inspired by org.eclipse.pde.internal.ui.wizards.tools.ConvertedProjectWizard
 */
public class BindProjectsWizard extends Wizard {

  private final List<IProject> projects;
  private BindProjectsPage mainPage;

  public BindProjectsWizard(List<IProject> projects) {
    this.projects = projects;
    setNeedsProgressMonitor(true);
    setWindowTitle("Bind Eclipse projects to SonarQube projects");
    setHelpAvailable(false);
  }

  @Override
  public void addPages() {
    mainPage = new BindProjectsPage(projects);
    addPage(mainPage);
  }

  @Override
  public boolean performFinish() {
    return mainPage.finish();
  }

}
