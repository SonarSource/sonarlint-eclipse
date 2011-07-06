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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.ITaskDataWorkingCopy;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.ui.TasksUi;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarConnector;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarMylynCorePlugin;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarTaskDataHandler;

public class SonarMylynUiPlugin extends Plugin {

  public static final String PLUGIN_ID = "org.sonar.ide.eclipse.mylyn.ui"; //$NON-NLS-1$

  /**
   * See org.eclipse.mylyn.internal.tasks.ui.util.TasksUiInternal#createAndOpenNewTask
   * @throws CoreException
   */
  public static void createAndOpen(TaskRepository repository, long violationId, String title) throws CoreException {
    ITask newTask = TasksUiUtil.createOutgoingNewTask(SonarConnector.CONNECTOR_KIND, repository.getUrl());
    SonarTaskDataHandler taskDataHandler = SonarMylynCorePlugin.getDefault().getConnector().getTaskDataHandler();
    TaskData taskData = taskDataHandler.createTaskData(repository, "", new NullProgressMonitor());

    taskDataHandler.createTaskData(repository, taskData, violationId, title);

    ITaskDataWorkingCopy workingCopy = TasksUi.getTaskDataManager().createWorkingCopy(newTask, taskData);
    workingCopy.save(null, null);

    TasksUiUtil.openTask(newTask);
  }
}
