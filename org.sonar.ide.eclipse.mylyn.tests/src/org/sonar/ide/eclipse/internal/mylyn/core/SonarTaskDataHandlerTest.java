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
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.services.Review;

import java.util.Date;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SonarTaskDataHandlerTest {
  private SonarConnector connector;
  private SonarTaskDataHandler handler;
  private TaskRepository repository;

  @Before
  public void setUp() {
    connector = new SonarConnector();
    handler = connector.getTaskDataHandler();
    repository = new TaskRepository(SonarConnector.CONNECTOR_KIND, "http://localhost:9000");
  }

  @Ignore("Sonar 2.9 supports modifications, so test must be rewriten")
  @Test
  public void testUpdateTaskData() {
    Review review = new Review()
        .setId(1L)
        .setTitle("Title")
        .setStatus("OPEN")
        .setSeverity("MINOR")
        .setType("VIOLATION")
        .setResourceKee("resource")
        .setCreatedAt(new Date(1))
        .setUpdatedAt(new Date(2))
        .setAuthorLogin("admin")
        .setAssigneeLogin("godin");
    TaskData data = handler.createTaskData(repository, "1", null);
    handler.updateTaskData(repository, data, review);

    assertThat(data.getRoot().getAttributes().size(), is(14));
    assertThat(getAttributeValue(data, TaskAttribute.TASK_KEY), is("1"));
    assertThat(getAttributeValue(data, TaskAttribute.TASK_URL), is("http://localhost:9000/reviews/view/1"));
    assertThat(getAttributeValue(data, TaskAttribute.SUMMARY), is("Title"));
    assertThat(getAttributeValue(data, TaskAttribute.STATUS), is("OPEN"));
    assertThat(getAttributeValue(data, TaskAttribute.PRIORITY), is("MINOR"));
    assertThat(getAttributeValue(data, TaskAttribute.TASK_KIND), is("VIOLATION"));
    assertThat(getAttributeValue(data, "sonarResource"), is("resource"));
    assertThat(getAttributeValue(data, TaskAttribute.DATE_CREATION), is("1"));
    assertThat(getAttributeValue(data, TaskAttribute.DATE_MODIFICATION), is("2"));
    assertThat(getAttributeValue(data, TaskAttribute.DATE_COMPLETION), is(""));
    assertThat(getAttributeValue(data, TaskAttribute.USER_REPORTER), is("admin"));
    assertThat(getAttributeValue(data, TaskAttribute.USER_ASSIGNED), is("godin"));
    assertThat(data.getRoot().getAttribute(TaskAttribute.COMMENT_NEW), notNullValue());

    // Add comment
    review.addComments(1L, new Date(3), "godin", "Comment");
    data = handler.createTaskData(repository, "1", null);
    handler.updateTaskData(repository, data, review);

    assertThat(data.getRoot().getAttributes().size(), is(15));
    assertThat(getAttributeValue(data, TaskAttribute.DATE_MODIFICATION), is("3"));
    TaskAttribute comment = data.getRoot().getAttribute(TaskAttribute.PREFIX_COMMENT + "1");
    assertThat(comment.getAttributes().size(), is(4));
    assertThat(getAttributeValue(comment, TaskAttribute.COMMENT_NUMBER), is("1"));
    assertThat(getAttributeValue(comment, TaskAttribute.COMMENT_DATE), is("3"));
    assertThat(getAttributeValue(comment, TaskAttribute.COMMENT_AUTHOR), is("godin"));
    assertThat(getAttributeValue(comment, TaskAttribute.COMMENT_TEXT), is("Comment"));
    assertThat(data.getRoot().getAttribute(TaskAttribute.COMMENT_NEW), notNullValue());

    // Close
    review.setStatus("CLOSED");
    data = handler.createTaskData(repository, "1", null);
    handler.updateTaskData(repository, data, review);
    assertThat(data.getRoot().getAttributes().size(), is(14));
    assertThat(getAttributeValue(data, TaskAttribute.STATUS), is("CLOSED"));
    assertThat(getAttributeValue(data, TaskAttribute.DATE_COMPLETION), is("3"));
    assertThat(data.getRoot().getAttribute(TaskAttribute.COMMENT_NEW), nullValue());
  }

  private String getAttributeValue(TaskAttribute attribute, String key) {
    return attribute.getAttribute(key).getValue();
  }

  private String getAttributeValue(TaskData data, String key) {
    return getAttributeValue(data.getRoot(), key);
  }

  @Ignore("Sonar 2.9 supports modifications, so test must be rewriten")
  @Test(expected = UnsupportedOperationException.class)
  public void testPostTaskData() throws CoreException {
    handler.postTaskData(repository, null, null, null);
  }
}
