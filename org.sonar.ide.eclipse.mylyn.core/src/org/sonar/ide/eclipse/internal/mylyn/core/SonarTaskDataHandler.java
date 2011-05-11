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
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.*;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskSchema.Field;
import org.sonar.wsclient.services.Review;

import java.util.Date;
import java.util.Set;

public class SonarTaskDataHandler extends AbstractTaskDataHandler {

  private SonarConnector connector;

  public SonarTaskDataHandler(SonarConnector connector) {
    this.connector = connector;
  }

  /**
   * Retrieves task data for the given review from repository.
   */
  public TaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor) throws CoreException {
    monitor = Policy.monitorFor(monitor);
    try {
      monitor.beginTask(Messages.SonarTaskDataHandler_Downloading_task, IProgressMonitor.UNKNOWN);

      SonarClient client = new SonarClient(repository);
      Review review = client.getReview(Long.parseLong(taskId), monitor);

      TaskData taskData = createTaskData(repository, taskId, monitor);
      updateTaskData(repository, taskData, review);
      return taskData;
    } finally {
      monitor.done();
    }
  }

  public void updateTaskData(TaskRepository repository, TaskData data, Review review) {
    SonarTaskSchema schema = SonarTaskSchema.getDefault();

    setAttributeValue(data, schema.ID, review.getId() + ""); //$NON-NLS-1$
    setAttributeValue(data, schema.URL, connector.getTaskUrl(repository.getUrl(), data.getTaskId()));
    setAttributeValue(data, schema.SUMMARY, review.getTitle());
    setAttributeValue(data, schema.PRIORITY, review.getSeverity());
    setAttributeValue(data, schema.DESCRIPTION, "Resource: " + review.getResourceKee() + " Line: " + review.getLine()); //$NON-NLS-1$ //$NON-NLS-2$

    setAttributeValue(data, schema.USER_REPORTER, review.getAuthorLogin());
    setAttributeValue(data, schema.USER_ASSIGNED, review.getAssigneeLogin());

    setAttributeValue(data, schema.DATE_CREATION, dateToString(review.getCreatedAt()));
    setAttributeValue(data, schema.DATE_MODIFICATION, dateToString(review.getUpdatedAt()));

    setAttributeValue(data, schema.STATUS, review.getStatus());
    if ("CLOSED".equals(review.getStatus())) { //$NON-NLS-1$
      // Set the completion date, this allows Mylyn mark the review as completed
      setAttributeValue(data, schema.DATE_COMPLETION, dateToString(review.getUpdatedAt()));
    }

    addComments(repository, data, review);
  }

  private void addComments(TaskRepository repository, TaskData data, Review review) {
    int count = 1;
    for (Review.Comment comment : review.getComments()) {
      TaskCommentMapper mapper = new TaskCommentMapper();
      mapper.setAuthor(repository.createPerson(comment.getAuthorLogin()));
      mapper.setCreationDate(comment.getUpdatedAt());
      mapper.setText(comment.getText());
      mapper.setNumber(count);
      TaskAttribute attribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_COMMENT + count);
      mapper.applyTo(attribute);
      count++;
    }
  }

  private static String dateToString(Date date) {
    return date.getTime() + ""; //$NON-NLS-1$
  }

  /**
   * Convenience method to set the value of a given Attribute in the given {@link TaskData}.
   */
  private static TaskAttribute setAttributeValue(TaskData data, Field sonarAttribute, String value) {
    TaskAttribute attribute = data.getRoot().getAttribute(sonarAttribute.getKey());
    if (value != null) {
      attribute.setValue(value);
    }
    return attribute;
  }

  public TaskData createTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor) {
    TaskData data = new TaskData(getAttributeMapper(repository), SonarConnector.CONNECTOR_KIND,
        repository.getRepositoryUrl(), taskId);
    initializeTaskData(repository, data, null, monitor);
    return data;
  }

  @Override
  public RepositoryResponse postTaskData(TaskRepository repository, TaskData taskData, Set<TaskAttribute> oldAttributes,
      IProgressMonitor monitor) throws CoreException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean initializeTaskData(TaskRepository repository, TaskData data, ITaskMapping initializationData, IProgressMonitor monitor) {
    SonarTaskSchema.getDefault().initialize(data);
    return true;
  }

  @Override
  public TaskAttributeMapper getAttributeMapper(TaskRepository repository) {
    return new TaskAttributeMapper(repository);
  }

}
