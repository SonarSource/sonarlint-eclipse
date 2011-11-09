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
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.mylyn.commons.net.AuthenticationCredentials;
import org.eclipse.mylyn.commons.net.AuthenticationType;
import org.eclipse.mylyn.internal.tasks.core.RepositoryQuery;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.junit.Test;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.services.ReviewQuery;

public class SonarClientTest {

  @Test
  public void testGetSonarHost() {
    TaskRepository repository = new TaskRepository(SonarConnector.CONNECTOR_KIND, "http://localhost:9000");
    SonarClient client = new SonarClient(repository);

    Host host = client.getSonarHost();
    assertThat(host.getHost(), is("http://localhost:9000"));
    assertThat(host.getUsername(), nullValue());
    assertThat(host.getPassword(), nullValue());

    repository.setCredentials(AuthenticationType.REPOSITORY, new AuthenticationCredentials("username", "password"), false);
    host = client.getSonarHost();
    assertThat(host.getUsername(), is("username"));
    assertThat(host.getPassword(), is("password"));
  }

  @Test
  public void testConvertQuery() {
    TaskRepository repository = new TaskRepository(SonarConnector.CONNECTOR_KIND, "http://localhost:9000");
    repository.setCredentials(AuthenticationType.REPOSITORY, new AuthenticationCredentials("username", "password"), false);
    SonarClient client = new SonarClient(repository);

    IRepositoryQuery repositoryQuery = new RepositoryQuery(SonarConnector.CONNECTOR_KIND, "");
    repositoryQuery.setAttribute(SonarQuery.PROJECT, "key");
    repositoryQuery.setAttribute(SonarQuery.REPORTER, "Any");
    repositoryQuery.setAttribute(SonarQuery.ASSIGNEE, "Current user");
    repositoryQuery.setAttribute(SonarQuery.STATUSES, "OPEN,REOPENED");
    repositoryQuery.setAttribute(SonarQuery.SEVERITIES, "BLOCKER");
    ReviewQuery query = client.convertQuery(repositoryQuery);
    assertThat(query.getProjectKeysOrIds(), is(new String[] { "key" }));
    assertThat(query.getAuthorLoginsOrIds(), nullValue());
    assertThat(query.getAssigneeLoginsOrIds(), is(new String[] { "username" }));
    assertThat(query.getStatuses(), is(new String[] { "OPEN", "REOPENED" }));
    assertThat(query.getSeverities(), is(new String[] { "BLOCKER" }));
  }
}
