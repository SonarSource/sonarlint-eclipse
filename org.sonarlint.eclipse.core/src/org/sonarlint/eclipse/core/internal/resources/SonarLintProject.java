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
package org.sonarlint.eclipse.core.internal.resources;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.builder.SonarLintBuilder;

public class SonarLintProject {

  private final IProject project;
  private List<SonarLintProperty> extraProperties = new ArrayList<>();

  public SonarLintProject(IProject project) {
    this.project = project;
  }

  public static SonarLintProject getInstance(IResource resource) {
    IProject project = resource.getProject();
    if (project == null || !project.isAccessible()) {
      throw new IllegalStateException("Unable to find project for resource " + resource);
    }
    return SonarLintCorePlugin.getDefault().getProjectManager().readSonarConfiguration(project);
  }

  public void save() {
    SonarLintCorePlugin.getDefault().getProjectManager().saveSonarConfiguration(project, this);
  }

  public IProject getProject() {
    return project;
  }

  public List<SonarLintProperty> getExtraProperties() {
    return extraProperties;
  }

  public void setExtraProperties(List<SonarLintProperty> extraProperties) {
    this.extraProperties = extraProperties;
  }

  public boolean isBuilderEnabled() {
    IProjectDescription desc;
    try {
      desc = project.getDescription();
    } catch (CoreException e) {
      throw new IllegalStateException("Unable to read project description", e);
    }
    ICommand[] commands = desc.getBuildSpec();

    for (int i = 0; i < commands.length; ++i) {
      if (commands[i].getBuilderName().equals(SonarLintBuilder.BUILDER_ID)) {
        return true;
      }
    }
    return false;
  }

  public void setBuilderEnabled(boolean enabled) {
    if (enabled && !isBuilderEnabled()) {
      addBuilder();
    } else if (!enabled && isBuilderEnabled()) {
      removeBuilder();
    }
  }

  private void removeBuilder() {
    try {
      IProjectDescription desc = project.getDescription();
      ICommand[] commands = desc.getBuildSpec();
      ICommand[] newCommands = new ICommand[commands.length - 1];
      int i = 0;
      for (ICommand previousCommand : commands) {
        if (!previousCommand.getBuilderName().equals(SonarLintBuilder.BUILDER_ID)) {
          newCommands[i] = previousCommand;
          i++;
        }
      }
      desc.setBuildSpec(newCommands);
      project.setDescription(desc, null);
    } catch (CoreException e) {
      throw new IllegalStateException("Unable to add builder", e);
    }
  }

  private void addBuilder() {
    try {
      IProjectDescription desc = project.getDescription();
      ICommand[] commands = desc.getBuildSpec();
      // add builder to project
      ICommand command = desc.newCommand();
      command.setBuilderName(SonarLintBuilder.BUILDER_ID);
      ICommand[] newCommands = new ICommand[commands.length + 1];

      // Add it after other builders.
      System.arraycopy(commands, 0, newCommands, 0, commands.length);
      newCommands[commands.length] = command;
      desc.setBuildSpec(newCommands);
      project.setDescription(desc, null);
    } catch (CoreException e) {
      throw new IllegalStateException("Unable to add builder", e);
    }
  }

}
