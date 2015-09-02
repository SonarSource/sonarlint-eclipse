/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.ide.eclipse.ui.internal.command;

import java.util.List;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.sonar.ide.eclipse.ui.internal.wizards.associate.ConfigureProjectsWizard;

/**
 *
 * @see ConfigureProjectsWizard
 */
public class ConfigureProjectsCommand extends AbstractProjectsCommand {

  public Display getDisplay() {
    Display display = Display.getCurrent();
    if (display == null) {
      display = Display.getDefault();
    }
    return display;
  }

  @Override
  public Object execute(ExecutionEvent event) throws ExecutionException {
    List<IProject> selectedProjects = findSelectedProjects(event);

    ConfigureProjectsWizard wizard = new ConfigureProjectsWizard(selectedProjects);

    final Display display = getDisplay();
    final WizardDialog dialog = new WizardDialog(display.getActiveShell(), wizard);
    dialog.setHelpAvailable(true);
    BusyIndicator.showWhile(display, new Runnable() {
      @Override
      public void run() {
        dialog.open();
      }
    });

    return null;
  }

}
