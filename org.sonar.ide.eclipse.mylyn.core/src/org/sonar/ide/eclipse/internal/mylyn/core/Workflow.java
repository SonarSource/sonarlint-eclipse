/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
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
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.eclipse.osgi.util.NLS;

/**
 * Workflow for review can be described as following:
 * <ul>
 * <li>For review with status "OPEN" or "REOPENED" possible to set status to "RESOLVED", reassign.</li>
 * <li>For review with status "RESOLVED" possible to set status to "REOPENED".</li>
 * <li>Any review except of closed can be commented.</li>
 * <li>Nothing can be done with closed review.</li>
 * </ul>
 */
public final class Workflow {

  private static long getReviewId(TaskData taskData) {
    return Long.parseLong(taskData.getTaskId());
  }

  private static String getAttribute(TaskData taskData, String attributeId) {
    TaskAttribute attribute = taskData.getRoot().getAttribute(attributeId);
    return attribute == null ? null : attribute.getValue();
  }

  private static String getStatus(TaskData taskData) {
    return getAttribute(taskData, TaskAttribute.STATUS);
  }

  private static String getAssignee(TaskData taskData) {
    return getAttribute(taskData, TaskAttribute.USER_ASSIGNED);
  }

  private static String getComment(TaskData taskData) {
    String comment = getAttribute(taskData, TaskAttribute.COMMENT_NEW);
    return comment == null ? "" : comment; //$NON-NLS-1$
  }

  private static CoreException createException(String message) {
    return new CoreException(new Status(IStatus.WARNING, SonarMylynCorePlugin.PLUGIN_ID, message));
  }

  public abstract static class Operation {
    final String getId() {
      return getClass().getSimpleName();
    }

    boolean isDefault() {
      return false;
    }

    abstract String getLabel(TaskData taskData);

    abstract boolean canPerform(TaskData taskData);

    abstract long perform(SonarClient client, TaskData taskData, IProgressMonitor monitor) throws CoreException;
  }

  public abstract static class CreateOperation extends Operation {
    @Override
    final boolean canPerform(TaskData taskData) {
      return taskData.isNew();
    }

    @Override
    final long perform(SonarClient client, TaskData taskData, IProgressMonitor monitor) throws CoreException {
      final long violationId = Long.parseLong(taskData.getRoot().getAttribute("violationId").getValue());
      final String comment = getComment(taskData);
      if ("".equals(comment)) { //$NON-NLS-1$
        throw createException(Messages.Workflow_CommentRequired_Error);
      }
      return performCreate(client, violationId, comment, taskData, monitor);
    }

    abstract long performCreate(SonarClient client, long violationId, String comment, TaskData taskData, IProgressMonitor monitor) throws CoreException;
  }

  public abstract static class EditOperation extends Operation {
    @Override
    final boolean canPerform(TaskData taskData) {
      return !taskData.isNew() && canPerformEdit(getStatus(taskData));
    }

    abstract boolean canPerformEdit(String status);

    @Override
    final long perform(SonarClient client, TaskData taskData, IProgressMonitor monitor) throws CoreException {
      performEdit(client, taskData, monitor);
      return getReviewId(taskData);
    }

    abstract void performEdit(SonarClient client, TaskData taskData, IProgressMonitor monitor) throws CoreException;
  }

  public static class DefaultCreate extends CreateOperation {
    @Override
    boolean isDefault() {
      return true;
    }

    @Override
    String getLabel(TaskData taskData) {
      return "Create";
    }

    @Override
    long performCreate(SonarClient client, long violationId, String comment, TaskData taskData, IProgressMonitor monitor) throws CoreException {
      return client.create(violationId, SonarClient.STATUS_OPEN, null, comment, getAssignee(taskData)).getId();
    }
  }

  public static class CreateFalsePositive extends CreateOperation {
    @Override
    String getLabel(TaskData taskData) {
      return Messages.Workflow_ResolveAsFalsePositive_Label;
    }

    @Override
    long performCreate(SonarClient client, long violationId, String comment, TaskData taskData, IProgressMonitor monitor) throws CoreException {
      return client.create(violationId, SonarClient.STATUS_RESOLVED, SonarClient.RESOLUTION_FALSE_POSITIVE, comment, null).getId();
    }
  }

  public static class CreateFixed extends CreateOperation {
    @Override
    String getLabel(TaskData taskData) {
      return Messages.Workflow_ResolveAsFixed_Label;
    }

    @Override
    long performCreate(SonarClient client, long violationId, String comment, TaskData taskData, IProgressMonitor monitor) throws CoreException {
      return client.create(violationId, SonarClient.STATUS_RESOLVED, SonarClient.RESOLUTION_FIXED, comment, getAssignee(taskData)).getId();
    }
  }

  /**
   * Leave status as is, but maybe add comment or reassign.
   */
  public static class Default extends EditOperation {
    @Override
    String getLabel(TaskData taskData) {
      return NLS.bind(Messages.Workflow_Default_Label, getStatus(taskData));
    }

    @Override
    boolean isDefault() {
      return true;
    }

    @Override
    boolean canPerformEdit(String status) {
      return !SonarClient.STATUS_CLOSED.equals(status);
    }

    @Override
    void performEdit(SonarClient client, TaskData taskData, IProgressMonitor monitor) {
      final String comment = getComment(taskData);
      final String status = getAttribute(taskData, TaskAttribute.STATUS);
      // First try to reassign - if this will fail, then we will not add comment
      if (SonarClient.STATUS_OPEN.equals(status) || SonarClient.STATUS_REOPENED.equals(status)) {
        client.reassign(getReviewId(taskData), getAssignee(taskData));
      }
      if (!SonarClient.STATUS_CLOSED.equals(status) && !"".equals(comment)) { //$NON-NLS-1$
        client.addComment(getReviewId(taskData), comment);
      }
    }
  }

  /**
   * Change status from "OPEN" or "REOPENED" to "RESOLVED" with resolution "FIXED".
   */
  public static class ResolveAsFixed extends EditOperation {
    @Override
    String getLabel(TaskData taskData) {
      return Messages.Workflow_ResolveAsFixed_Label;
    }

    @Override
    boolean canPerformEdit(String status) {
      return SonarClient.STATUS_OPEN.equals(status)
        || SonarClient.STATUS_REOPENED.equals(status);
    }

    @Override
    void performEdit(SonarClient client, TaskData taskData, IProgressMonitor monitor) {
      client.resolve(getReviewId(taskData), SonarClient.RESOLUTION_FIXED, getComment(taskData));
    }
  }

  /**
   * Change status from "OPEN" or "REOPENED" to "RESOLVED" with resolution "FALSE-POSITIVE".
   */
  public static class ResolveAsFalsePositive extends EditOperation {
    @Override
    String getLabel(TaskData taskData) {
      return Messages.Workflow_ResolveAsFalsePositive_Label;
    }

    @Override
    boolean canPerformEdit(String status) {
      return SonarClient.STATUS_OPEN.equals(status)
        || SonarClient.STATUS_REOPENED.equals(status);
    }

    @Override
    void performEdit(SonarClient client, TaskData taskData, IProgressMonitor monitor) throws CoreException {
      final String comment = getComment(taskData);
      if ("".equals(comment)) { //$NON-NLS-1$
        throw createException(Messages.Workflow_CommentRequired_Error);
      }
      client.resolve(getReviewId(taskData), SonarClient.RESOLUTION_FALSE_POSITIVE, getComment(taskData));
    }
  }

  /**
   * Change status from "RESOLVED" to "REOPENED".
   */
  public static class Reopen extends EditOperation {
    @Override
    String getLabel(TaskData taskData) {
      return Messages.Workflow_Reopen_Label;
    }

    @Override
    boolean canPerformEdit(String status) {
      return SonarClient.STATUS_RESOLVED.equals(status);
    }

    @Override
    void performEdit(SonarClient client, TaskData taskData, IProgressMonitor monitor) throws CoreException {
      final String comment = getComment(taskData);
      final String resolution = getAttribute(taskData, TaskAttribute.RESOLUTION);
      if (SonarClient.RESOLUTION_FALSE_POSITIVE.equals(resolution) && "".equals(comment)) { //$NON-NLS-1$
        throw createException(Messages.Workflow_CommentRequired_Error);
      }
      long id = getReviewId(taskData);
      client.reopen(id, getComment(taskData));
      client.reassign(id, getAssignee(taskData));
    }
  }

  /**
   * All possible workflow operations.
   */
  public static final Operation[] OPERATIONS = new Operation[] {
    new DefaultCreate(), new CreateFalsePositive(), new CreateFixed(),
    new Default(), new ResolveAsFixed(), new ResolveAsFalsePositive(), new Reopen()};

  public static Operation operationById(String id) {
    for (Operation op : OPERATIONS) {
      if (op.getId().equals(id)) {
        return op;
      }
    }
    return null;
  }

  private Workflow() {
  }

}
