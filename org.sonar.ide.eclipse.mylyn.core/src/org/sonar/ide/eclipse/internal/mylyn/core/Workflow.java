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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.sonar.wsclient.services.Review;

/**
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

  private static String getAssignee(TaskData taskData) {
    return getAttribute(taskData, TaskAttribute.USER_ASSIGNED);
  }

  private static String getComment(TaskData taskData) {
    String comment = getAttribute(taskData, TaskAttribute.COMMENT_NEW);
    return comment == null ? "" : comment;
  }

  public abstract static class Operation {
    final String getId() {
      return getClass().getSimpleName();
    }

    boolean isDefault() {
      return false;
    }

    abstract String getLabel(Review review);

    abstract boolean canPerform(Review review);

    abstract void perform(SonarClient client, TaskData taskData, IProgressMonitor monitor);
  }

  /**
   * Add comment.
   */
  public static class AddComment extends Operation {
    String getLabel(Review review) {
      return "Add comment";
    }

    boolean isDefault() {
      return true;
    }

    boolean canPerform(Review review) {
      return !SonarClient.STATUS_CLOSED.equals(review.getStatus());
    }

    void perform(SonarClient client, TaskData taskData, IProgressMonitor monitor) {
      client.addComment(getReviewId(taskData), getComment(taskData), monitor);
    }
  }

  /**
   * Reassign.
   */
  public static class Reassign extends Operation {
    String getLabel(Review review) {
      return "Reassign";
    }

    @Override
    boolean canPerform(Review review) {
      return SonarClient.STATUS_OPEN.equals(review.getStatus())
          || SonarClient.STATUS_REOPENED.equals(review.getStatus());
    }

    @Override
    void perform(SonarClient client, TaskData taskData, IProgressMonitor monitor) {
      client.reassign(getReviewId(taskData), getAssignee(taskData), monitor);
    }
  }

  /**
   * Change status from "OPEN" or "REOPENED" to "RESOLVED" with resolution "FIXED".
   */
  public static class Resolve extends Operation {
    String getLabel(Review review) {
      return "Resolve";
    }

    @Override
    boolean canPerform(Review review) {
      return SonarClient.STATUS_OPEN.equals(review.getStatus())
          || SonarClient.STATUS_REOPENED.equals(review.getStatus());
    }

    @Override
    void perform(SonarClient client, TaskData taskData, IProgressMonitor monitor) {
      long id = getReviewId(taskData);
      client.resolve(id, "FIXED", getComment(taskData), monitor);
    }
  }

  /**
   * Change status from "OPEN" or "REOPENED" to "RESOLVED" with resolution "FALSE-POSITIVE".
   */
  public static class FlagAsFalsePositive extends Operation {
    String getLabel(Review review) {
      return "Flag as false-positive";
    }

    boolean canPerform(Review review) {
      return SonarClient.STATUS_OPEN.equals(review.getStatus())
          || SonarClient.STATUS_REOPENED.equals(review.getStatus());
    }

    void perform(SonarClient client, TaskData taskData, IProgressMonitor monitor) {
      client.resolve(getReviewId(taskData), "FALSE-POSITIVE", getComment(taskData), monitor);
    }
  }

  /**
   * Change status from "RESOLVED" to "REOPENED".
   */
  public static class Reopen extends Operation {
    String getLabel(Review review) {
      return "Reopen";
    }

    boolean canPerform(Review review) {
      return SonarClient.STATUS_RESOLVED.equals(review.getStatus());
    }

    void perform(SonarClient client, TaskData taskData, IProgressMonitor monitor) {
      client.reopen(getReviewId(taskData), getComment(taskData), monitor);
    }
  }

  public static final Operation[] OPERATIONS = new Operation[] { new AddComment(), new Reassign(), new Resolve(), new Reopen(), new FlagAsFalsePositive() };

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
