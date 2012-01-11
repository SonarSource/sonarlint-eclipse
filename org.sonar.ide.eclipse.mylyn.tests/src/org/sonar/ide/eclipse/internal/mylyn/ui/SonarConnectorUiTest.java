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
package org.sonar.ide.eclipse.internal.mylyn.ui;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarConnector;

@Ignore("Requires UI")
public class SonarConnectorUiTest {

  private SonarConnectorUi connectorUi;

  @Before
  public void setUp() {
    connectorUi = new SonarConnectorUi();
  }

  @Test
  public void testGetConnectorKind() {
    assertThat(connectorUi.getConnectorKind(), is(SonarConnector.CONNECTOR_KIND));
  }

  @Test
  public void testHasSearchPage() {
    assertThat(connectorUi.hasSearchPage(), is(false));
  }

  @Test
  public void testGetReplyText() {
    assertThat(connectorUi.getReplyText(null, null, null, false), is(""));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void testGetNewTaskWizard() {
    connectorUi.getNewTaskWizard(null, null);
  }

}
