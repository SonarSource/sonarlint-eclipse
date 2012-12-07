/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.ide.eclipse.internal.mylyn.core;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.mylyn.context.core.IInteractionContext;
import org.eclipse.mylyn.internal.context.core.ContextCorePlugin;
import org.eclipse.mylyn.monitor.core.InteractionEvent;
import org.eclipse.mylyn.tasks.core.AbstractRepositoryConnector;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.ITask;
import org.eclipse.mylyn.tasks.core.ITask.PriorityLevel;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskDataCollector;
import org.eclipse.mylyn.tasks.core.data.TaskMapper;
import org.eclipse.mylyn.tasks.core.sync.ISynchronizationSession;
import org.eclipse.osgi.util.NLS;
import org.sonar.ide.eclipse.core.internal.resources.ResourceUtils;
import org.sonar.wsclient.services.Review;

import java.util.Collection;

public class SonarConnector extends AbstractRepositoryConnector {

  public static final String CONNECTOR_KIND = "sonar"; //$NON-NLS-1$

  private static final String REVIEW_PREFIX = "/reviews/view/"; //$NON-NLS-1$

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
    return NLS.bind(Messages.SonarConnector_Label, SonarMylynCorePlugin.MINIMAL_SONAR_VERSION);
  }

  @Override
  public boolean canCreateNewTask(TaskRepository repository) {
    return false;
  }

  @Override
  public boolean canCreateTaskFromKey(TaskRepository repository) {
    return false;
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
  public String getRepositoryUrlFromTaskUrl(String taskFullUrl) {
    if (taskFullUrl == null) {
      return null;
    }
    int index = taskFullUrl.lastIndexOf(REVIEW_PREFIX);
    return index == -1 ? null : taskFullUrl.substring(0, index);
  }

  @Override
  public String getTaskIdFromTaskUrl(String taskFullUrl) {
    if (taskFullUrl == null) {
      return null;
    }
    int index = taskFullUrl.lastIndexOf(REVIEW_PREFIX);
    return index == -1 ? null : taskFullUrl.substring(index + REVIEW_PREFIX.length());
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

      SonarClient client = new SonarClient(repository);
      Collection<Review> reviews = client.getReviews(query);
      for (Review review : reviews) {
        TaskData taskData = taskDataHandler.createTaskData(repository, review.getId() + "", monitor); //$NON-NLS-1$
        taskData.setPartial(true);
        taskDataHandler.updateTaskData(repository, taskData, review);
        resultCollector.accept(taskData);
      }

      if (session != null) { // See SONARIDE-246
        /*
         * Godin: I'm not sure that all tasks from session should be marked as stale,
         * however this allows to update attributes in case when SonarTaskSchema was updated.
         */
        for (ITask task : session.getTasks()) {
          session.markStale(task);
        }
      }

      return Status.OK_STATUS;
    } finally {
      monitor.done();
    }
  }

  @Override
  public void updateRepositoryConfiguration(TaskRepository repository, IProgressMonitor monitor) throws CoreException {
    // ignore, nothing to do
  }

  @Override
  public void updateTaskFromTaskData(TaskRepository repository, ITask task, TaskData taskData) {
    TaskMapper mapper = (TaskMapper) getTaskMapping(taskData);
    mapper.applyTo(task);

    populateContext(task, taskData);
  }

  private void populateContext(ITask task, TaskData taskData) {
    TaskAttribute attribute = taskData.getRoot().getAttribute(SonarTaskSchema.getDefault().RESOURCE.getKey());
    if (attribute == null) {
      return;
    }
    String sonarResourceKey = attribute.getValue();
    IResource resource = ResourceUtils.getResource(sonarResourceKey);
    if (!(resource instanceof IFile)) {
      return;
    }
    IFile file = (IFile) resource;

    IInteractionContext context = ContextCorePlugin.getContextStore().loadContext(task.getHandleIdentifier());
    ContextCorePlugin.getContextManager().processInteractionEvent(
        file,
        InteractionEvent.Kind.PROPAGATION,
        InteractionEvent.ID_UNKNOWN,
        context);
    ContextCorePlugin.getContextStore().saveContext(context);
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
      return Utils.toMylynPriority(getPriority());
    }
  }

}
