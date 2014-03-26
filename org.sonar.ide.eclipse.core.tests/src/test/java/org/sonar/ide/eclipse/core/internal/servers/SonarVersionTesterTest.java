/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2014 SonarSource
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
package org.sonar.ide.eclipse.core.internal.servers;

import org.junit.Test;
import org.sonar.ide.eclipse.wsclient.SonarVersionTester;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class SonarVersionTesterTest {

  @Test
  public void shouldCompareServerVersion() {
    assertThat(SonarVersionTester.isServerVersionSupported("3.4", "1.1"), is(false));
    assertThat(SonarVersionTester.isServerVersionSupported("3.4", "2.7-SNAPSHOT"), is(false));
    assertThat(SonarVersionTester.isServerVersionSupported("3.4", "3.4-RC2"), is(true));
  }
}
