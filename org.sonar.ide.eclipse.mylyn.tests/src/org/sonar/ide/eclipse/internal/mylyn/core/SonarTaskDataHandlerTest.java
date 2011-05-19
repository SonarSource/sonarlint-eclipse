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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskData;
import org.junit.Before;
import org.junit.Test;
import org.sonar.wsclient.services.Review;

import java.util.Date;

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

    assertThat(data.getRoot().getAttributes().size(), is(12));
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

    // Add comment
    review.addComments(new Date(3), "godin", "Comment");
    data = handler.createTaskData(repository, "1", null);
    handler.updateTaskData(repository, data, review);

    assertThat(data.getRoot().getAttributes().size(), is(13));
    assertThat(getAttributeValue(data, TaskAttribute.DATE_MODIFICATION), is("3"));
    TaskAttribute comment = data.getRoot().getAttribute(TaskAttribute.PREFIX_COMMENT + "1");
    assertThat(comment.getAttributes().size(), is(4));
    assertThat(getAttributeValue(comment, TaskAttribute.COMMENT_NUMBER), is("1"));
    assertThat(getAttributeValue(comment, TaskAttribute.COMMENT_DATE), is("3"));
    assertThat(getAttributeValue(comment, TaskAttribute.COMMENT_AUTHOR), is("godin"));
    assertThat(getAttributeValue(comment, TaskAttribute.COMMENT_TEXT), is("Comment"));

    // Close
    review.setStatus("CLOSED");
    data = handler.createTaskData(repository, "1", null);
    handler.updateTaskData(repository, data, review);
    assertThat(data.getRoot().getAttributes().size(), is(13));
    assertThat(getAttributeValue(data, TaskAttribute.STATUS), is("CLOSED"));
    assertThat(getAttributeValue(data, TaskAttribute.DATE_COMPLETION), is("3"));
  }

  private String getAttributeValue(TaskAttribute attribute, String key) {
    return attribute.getAttribute(key).getValue();
  }

  private String getAttributeValue(TaskData data, String key) {
    return getAttributeValue(data.getRoot(), key);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testPostTaskData() throws CoreException {
    handler.postTaskData(repository, null, null, null);
  }
}
