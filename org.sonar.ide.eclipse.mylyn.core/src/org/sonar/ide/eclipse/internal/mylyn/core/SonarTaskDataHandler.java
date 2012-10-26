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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.mylyn.commons.net.Policy;
import org.eclipse.mylyn.tasks.core.ITaskMapping;
import org.eclipse.mylyn.tasks.core.RepositoryResponse;
import org.eclipse.mylyn.tasks.core.RepositoryResponse.ResponseKind;
import org.eclipse.mylyn.tasks.core.RepositoryStatus;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.AbstractTaskDataHandler;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMapper;
import org.eclipse.mylyn.tasks.core.data.TaskCommentMapper;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.mylyn.tasks.core.data.TaskOperation;
import org.sonar.ide.eclipse.internal.mylyn.core.AbstractTaskSchema.Field;
import org.sonar.ide.eclipse.internal.mylyn.core.Workflow.Operation;
import org.sonar.wsclient.connectors.ConnectionException;
import org.sonar.wsclient.services.Review;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.Set;

public class SonarTaskDataHandler extends AbstractTaskDataHandler {

  private final SonarConnector connector;

  public SonarTaskDataHandler(SonarConnector connector) {
    this.connector = connector;
  }

  /**
   * Retrieves task data for the given review from repository.
   */
  public TaskData getTaskData(TaskRepository repository, String taskId, IProgressMonitor monitor) {
    monitor = Policy.monitorFor(monitor);
    try {
      monitor.beginTask(Messages.SonarTaskDataHandler_Downloading_task, IProgressMonitor.UNKNOWN);

      SonarClient client = new SonarClient(repository);
      Review review = client.getReview(Long.parseLong(taskId));

      TaskData taskData = createTaskData(repository, taskId, monitor);
      updateTaskData(repository, taskData, review);
      return taskData;
    } finally {
      monitor.done();
    }
  }

  public void createTaskData(TaskRepository repository, TaskData data, long violationId, String title) {
    SonarTaskSchema schema = SonarTaskSchema.getDefault();
    setAttributeValue(data, schema.SUMMARY, title);
    setAttributeValue(data, schema.STATUS, SonarClient.STATUS_OPEN);
    setAttributeValue(data, schema.USER_REPORTER, repository.getUserName());
    setAttributeValue(data, schema.USER_ASSIGNED, repository.getUserName());
    setAttributeValue(data, schema.VIOLATION_ID, violationId);
    data.getRoot().getAttribute(TaskAttribute.USER_ASSIGNED).getMetaData().setReadOnly(false);

    data.getRoot()
        .createAttribute(TaskAttribute.COMMENT_NEW)
        .getMetaData()
        .setType(TaskAttribute.TYPE_LONG_RICH_TEXT)
        .setReadOnly(false);

    createOperations(data);
  }

  public void updateTaskData(TaskRepository repository, TaskData data, Review review) {
    if (null == review) {
      return;
    }

    SonarTaskSchema schema = SonarTaskSchema.getDefault();

    // Workaround for SONAR-2449
    Date modification = review.getUpdatedAt();
    for (Review.Comment comment : review.getComments()) {
      if (modification.compareTo(comment.getUpdatedAt()) < 0) {
        modification = comment.getUpdatedAt();
      }
    }

    boolean readOnly = SonarClient.STATUS_CLOSED.equals(review.getStatus());

    setAttributeValue(data, schema.ID, review.getId());
    setAttributeValue(data, schema.URL, connector.getTaskUrl(repository.getUrl(), data.getTaskId()));
    setAttributeValue(data, schema.SUMMARY, review.getTitle());
    setAttributeValue(data, schema.PRIORITY, review.getSeverity());
    setAttributeValue(data, schema.RESOURCE, review.getResourceKee());
    setAttributeValue(data, schema.LINE, review.getLine());

    setAttributeValue(data, schema.TASK_KIND, review.getType());

    setAttributeValue(data, schema.USER_REPORTER, review.getAuthorLogin());
    setAttributeValue(data, schema.USER_ASSIGNED, review.getAssigneeLogin());
    data.getRoot().getAttribute(TaskAttribute.USER_ASSIGNED).getMetaData().setReadOnly(readOnly);

    setAttributeValue(data, schema.DATE_CREATION, dateToString(review.getCreatedAt()));
    setAttributeValue(data, schema.DATE_MODIFICATION, dateToString(modification));

    setAttributeValue(data, schema.STATUS, review.getStatus());
    setAttributeValue(data, schema.RESOLUTION, review.getResolution());
    if (SonarClient.STATUS_CLOSED.equals(review.getStatus()) || SonarClient.STATUS_RESOLVED.equals(review.getStatus())) {
      // Set the completion date, this allows Mylyn mark the review as completed
      setAttributeValue(data, schema.DATE_COMPLETION, dateToString(modification));
    }

    addComments(repository, data, review);

    // New comment can not be added to closed review
    if (!readOnly) {
      data.getRoot()
          .createAttribute(TaskAttribute.COMMENT_NEW)
          .getMetaData()
          .setType(TaskAttribute.TYPE_LONG_RICH_TEXT)
          .setReadOnly(false);
    }

    createOperations(data);
  }

  private void createOperations(TaskData data) {
    TaskAttribute operationAttribute = data.getRoot().createAttribute(TaskAttribute.OPERATION);
    operationAttribute.getMetaData().setType(TaskAttribute.TYPE_OPERATION);

    for (Workflow.Operation operation : Workflow.OPERATIONS) {
      if (operation.canPerform(data)) {
        addOperation(data, operation);
      }
    }
  }

  private void addOperation(TaskData data, Operation operation) {
    String id = operation.getId();
    String label = operation.getLabel(data);
    TaskAttribute attribute = data.getRoot().createAttribute(TaskAttribute.PREFIX_OPERATION + id);
    TaskOperation.applyTo(attribute, id, label);

    if (operation.isDefault()) {
      TaskAttribute operationAttribute = data.getRoot().getAttribute(TaskAttribute.OPERATION);
      TaskOperation.applyTo(operationAttribute, id, label);
    }
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
    return Long.toString(date.getTime());
  }

  /**
   * Convenience method to set the value of a given Attribute in the given {@link TaskData}.
   */
  private static TaskAttribute setAttributeValue(TaskData data, Field sonarAttribute, Object value) {
    TaskAttribute attribute = data.getRoot().getAttribute(sonarAttribute.getKey());
    if (value != null) {
      attribute.setValue(value.toString());
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
  public RepositoryResponse postTaskData(TaskRepository repository, TaskData taskData, Set<TaskAttribute> oldAttributes, IProgressMonitor monitor) throws CoreException {
    final SonarClient client = new SonarClient(repository);
    TaskAttribute operationAttribute = taskData.getRoot().getAttribute(TaskAttribute.OPERATION);
    if (operationAttribute != null) {
      Workflow.Operation operation = Workflow.operationById(operationAttribute.getValue());
      if (operation != null) {
        try {
          long reviewId = operation.perform(client, taskData, monitor);
          return new RepositoryResponse(taskData.isNew() ? ResponseKind.TASK_CREATED : ResponseKind.TASK_UPDATED, Long.toString(reviewId));
        } catch (ConnectionException e) {
          StringWriter sw = new StringWriter();
          sw.append(e.getMessage() + "\n"); //$NON-NLS-1$
          e.printStackTrace(new PrintWriter(sw));
          RepositoryStatus status = RepositoryStatus.createHtmlStatus(repository.getUrl(), IStatus.ERROR, SonarMylynCorePlugin.PLUGIN_ID, RepositoryStatus.ERROR_REPOSITORY,
              Messages.SonarTaskDataHandler_ConnectionException, sw.getBuffer().toString());
          throw new CoreException(status);
        }
      }
    }
    return null;
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
