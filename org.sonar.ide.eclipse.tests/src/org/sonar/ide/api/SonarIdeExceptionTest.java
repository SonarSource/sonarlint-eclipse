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

package org.sonar.ide.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Test of sonar-ide excpetion api in eclipse plugin.
 * 
 * @link http://jira.codehaus.org/browse/SONARIDE-35
 * 
 * @author Jérémie Lagarde
 * 
 */
public class SonarIdeExceptionTest {

  @Test
  public void testException() {
    String msg = "testing ...";
    try {
      throwSonarIdeException(msg);
      fail();
    } catch (SonarIdeException e) {
      assertEquals(msg, e.getMessage());
    }
  }

  @Test
  public void testExceptionWithCause() {
    String msg = "testing ...";
    try {
      throwSonarIdeExceptionWithCause(msg);
      fail();
    } catch (SonarIdeException e) {
      assertEquals(msg, e.getMessage());
      assertNotNull(e.getCause());
    }
  }

  private void throwSonarIdeException(String msg) {
    throw new SonarIdeException(msg);
  }

  private void throwSonarIdeExceptionWithCause(String msg) {
    throw new SonarIdeException(msg, new NullPointerException());
  }
}
