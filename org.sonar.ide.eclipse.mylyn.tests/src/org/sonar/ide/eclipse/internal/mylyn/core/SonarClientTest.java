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
import org.eclipse.mylyn.tasks.core.TaskRepository;
import org.junit.Test;
import org.sonar.wsclient.Host;

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

}
