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
package org.sonar.ide.eclipse.core.internal;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class SonarNature implements IProjectNature {

  public static final String NATURE_ID = SonarCorePlugin.PLUGIN_ID + ".sonarNature";
  private static final String BUILDER_ID = SonarCorePlugin.PLUGIN_ID + ".sonarQubeBuilder";

  private IProject project;

  @Override
  public void configure() throws CoreException {
    IProjectDescription desc = project.getDescription();
    ICommand[] commands = desc.getBuildSpec();
    boolean found = false;

    for (int i = 0; i < commands.length; ++i) {
      if (commands[i].getBuilderName().equals(BUILDER_ID)) {
        found = true;
        break;
      }
    }
    if (!found) {
      // add builder to project
      ICommand command = desc.newCommand();
      command.setBuilderName(BUILDER_ID);
      ICommand[] newCommands = new ICommand[commands.length + 1];

      // Add it after other builders.
      System.arraycopy(commands, 0, newCommands, 0, commands.length);
      newCommands[commands.length] = command;
      desc.setBuildSpec(newCommands);
      project.setDescription(desc, null);
    }
  }

  @Override
  public void deconfigure() throws CoreException {
    IProjectDescription desc = project.getDescription();
    List<ICommand> commands = new ArrayList<>();
    commands.addAll(Arrays.asList(desc.getBuildSpec()));

    for (ICommand command : commands) {
      if (command.getBuilderName().equals(BUILDER_ID)) {
        commands.remove(command);
        break;
      }
    }
    desc.setBuildSpec(commands.toArray(new ICommand[commands.size()]));
    project.setDescription(desc, null);
  }

  @Override
  public IProject getProject() {
    return project;
  }

  @Override
  public void setProject(IProject project) {
    this.project = project;
  }

  public static boolean hasSonarNature(IProject project) {
    try {
      return project.hasNature(NATURE_ID);
    } catch (CoreException e) {
      SonarCorePlugin.getDefault().error(e.getMessage(), e);
      return false;
    }
  }

  public static void enableNature(IProject project) throws CoreException {
    IProjectDescription description = project.getDescription();
    String[] prevNatures = description.getNatureIds();
    String[] newNatures = new String[prevNatures.length + 1];
    System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
    newNatures[0] = NATURE_ID;
    description.setNatureIds(newNatures);
    project.setDescription(description, null);
  }

  public static void disableNature(IProject project) throws CoreException {
    project.deleteMarkers(SonarCorePlugin.MARKER_ID, true, IResource.DEPTH_INFINITE);

    IProjectDescription description = project.getDescription();
    List<String> newNatures = Lists.newArrayList();
    for (String natureId : description.getNatureIds()) {
      if (!NATURE_ID.equals(natureId)) {
        newNatures.add(natureId);
      }
    }
    description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
    project.setDescription(description, null);
  }

}
