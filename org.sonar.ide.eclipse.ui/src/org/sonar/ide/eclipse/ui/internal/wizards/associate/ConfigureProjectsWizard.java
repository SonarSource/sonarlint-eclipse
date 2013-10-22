/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.wizards.associate;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.Wizard;

import java.util.List;

/**
 * Inspired by org.eclipse.pde.internal.ui.wizards.tools.ConvertedProjectWizard
 */
public class ConfigureProjectsWizard extends Wizard {

  private final List<IProject> projects;
  private ConfigureProjectsPage mainPage;

  public ConfigureProjectsWizard(List<IProject> projects) {
    this.projects = projects;
    setNeedsProgressMonitor(true);
    setWindowTitle("Associate with Sonar");
    setHelpAvailable(false);
  }

  @Override
  public void addPages() {
    mainPage = new ConfigureProjectsPage(projects);
    addPage(mainPage);
  }

  @Override
  public boolean performFinish() {
    return mainPage.finish();
  }

}
