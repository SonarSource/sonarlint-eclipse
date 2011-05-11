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
package org.sonar.ide.eclipse.internal.mylyn.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.tasks.core.*;
import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;

import java.util.List;

public class SonarConnector extends AbstractRepositoryConnector {

  public static final String CONNECTOR_KIND = "sonar"; //$NON-NLS-1$

  public static final String REVIEW_PREFIX = "/reviews/view/"; //$NON-NLS-1$

  private final SonarTaskDataHandler taskDataHandler = new SonarTaskDataHandler(this);

  public SonarConnector() {
    if (SonarMylynCorePlugin.getDefault() != null) {
      SonarMylynCorePlugin.getDefault().setConnector(this);
    }
  }

  @Override
  public String getConnectorKind() {
    return CONNECTOR_KIND;
  }

  @Override
  public String getLabel() {
    return Messages.SonarConnector_Label;
  }

  @Override
  public boolean canCreateNewTask(TaskRepository repository) {
    return false;
  }

  @Override
  public boolean canCreateTaskFromKey(TaskRepository repository) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public String getRepositoryUrlFromTaskUrl(String taskFullUrl) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor) throws CoreException {
    return taskDataHandler.getTaskData(repository, taskId, monitor);
  }

  @Override
  public SonarTaskDataHandler getTaskDataHandler() {
    return taskDataHandler;
  }

  @Override
  public String getTaskIdFromTaskUrl(String taskFullUrl) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getTaskUrl(String repositoryUrl, String taskId) {
    return repositoryUrl + REVIEW_PREFIX + taskId;
  }

  @Override
  public boolean hasTaskChanged(TaskRepository repository, ITask task, TaskData taskData) {
    TaskMapper mapper = (TaskMapper) getTaskMapping(taskData);
    return mapper.hasChanges(task);
  }

  @Override
  public IStatus performQuery(TaskRepository repository, IRepositoryQuery query, TaskDataCollector resultCollector,
      ISynchronizationSession session, IProgressMonitor monitor) {
    try {
      monitor.beginTask(Messages.SonarConnector_Executing_query, IProgressMonitor.UNKNOWN);

      SonarClient client = new SonarClient();
      List<Review> reviews = client.getReviews(monitor);
      for (Review review : reviews) {
        TaskData taskData = taskDataHandler.createTaskData(repository, review.id + "", monitor); //$NON-NLS-1$
        taskData.setPartial(true);
        taskDataHandler.updateTaskData(repository, taskData, review);
        resultCollector.accept(taskData);
      }

      return Status.OK_STATUS;
    } finally {
      monitor.done();
    }
  }

  @Override
  public void updateRepositoryConfiguration(TaskRepository repository, IProgressMonitor monitor) throws CoreException {
    // TODO Auto-generated method stub
  }

  @Override
  public void updateTaskFromTaskData(TaskRepository repository, ITask task, TaskData taskData) {
    TaskMapper mapper = (TaskMapper) getTaskMapping(taskData);
    mapper.applyTo(task);
  }

  @Override
  public ITaskMapping getTaskMapping(TaskData taskData) {
    return new SonarTaskMapper(taskData);
  }

  private static class SonarTaskMapper extends TaskMapper {
    public SonarTaskMapper(TaskData taskData) {
      super(taskData);
    }

    @Override
    public PriorityLevel getPriorityLevel() {
      String value = getPriority();
      if ("blocker".equals(value)) { //$NON-NLS-1$
        return PriorityLevel.P1;
      } else if ("critical".equals(value)) { //$NON-NLS-1$
        return PriorityLevel.P2;
      } else if ("major".equals(value)) { //$NON-NLS-1$
        return PriorityLevel.P3;
      } else if ("minor".equals(value)) { //$NON-NLS-1$
        return PriorityLevel.P4;
      } else if ("info".equals(value)) { //$NON-NLS-1$
        return PriorityLevel.P5;
      } else {
        return null;
      }
    }
  }

}
