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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class SonarConnectorTest {

  private SonarConnector connector;

  @Before
  public void setUp() {
    connector = new SonarConnector();
  }

  @Test
  public void testGetConnectorKind() {
    assertThat(connector.getConnectorKind(), is(SonarConnector.CONNECTOR_KIND));
  }

  @Test
  public void shouldNotAllowToCreateNewTask() {
    assertThat(connector.canCreateNewTask(null), is(false));
  }

  @Test
  public void testGetRepositoryUrlFromTaskUrl() {
    assertThat(connector.getRepositoryUrlFromTaskUrl(null), nullValue());
    assertThat(connector.getRepositoryUrlFromTaskUrl("http://localhost:9000/1"), nullValue());
    assertThat(connector.getRepositoryUrlFromTaskUrl("http://localhost:9000/reviews/view/1"), is("http://localhost:9000"));
  }

  @Test
  public void testGetTaskIdFromTaskUrl() {
    assertThat(connector.getTaskIdFromTaskUrl(null), nullValue());
    assertThat(connector.getTaskIdFromTaskUrl("http://localhost:9000/1"), nullValue());
    assertThat(connector.getTaskIdFromTaskUrl("http://localhost:9000/reviews/view/1"), is("1"));
  }

  @Test
  public void testGetTaskUrl() {
    assertThat(connector.getTaskUrl("http://localhost:9000", "1"), is("http://localhost:9000/reviews/view/1"));
  }

  @Test
  public void shouldCompareServerVersion() {
    assertThat(SonarConnector.isServerVersionSupported("1.1"), is(false));
    assertThat(SonarConnector.isServerVersionSupported("2.7-SNAPSHOT"), is(false));
    assertThat(SonarConnector.isServerVersionSupported("2.9-RC2"), is(true));
  }
}
