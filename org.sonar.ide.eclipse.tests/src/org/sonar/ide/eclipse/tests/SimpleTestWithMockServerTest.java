/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.tests;

import org.junit.Test;
import org.sonar.ide.eclipse.tests.common.AbstractSonarTest;


/**
 * Simple test case to start the mock sonar server.
 * @link http://jira.codehaus.org/browse/SONARIDE-35
 * 
 * @author Jérémie Lagarde
 *
 */
public class SimpleTestWithMockServerTest extends AbstractSonarTest {
  
  @Test
  public void testStartMockServer() throws Exception {
    // start the mock sonar server.
    startTestServer();
  }

}
