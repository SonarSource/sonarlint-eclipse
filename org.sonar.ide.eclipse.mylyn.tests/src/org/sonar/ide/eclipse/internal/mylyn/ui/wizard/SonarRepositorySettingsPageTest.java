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
package org.sonar.ide.eclipse.internal.mylyn.ui.wizard;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Requires UI")
public class SonarRepositorySettingsPageTest {

  private SonarRepositorySettingsPage page;

  @Before
  public void setUp() {
    page = new SonarRepositorySettingsPage(null);
  }

  @Test
  public void shouldValidateUrl() {
    assertThat(page.isValidUrl("http://localhost"), is(true));
    assertThat(page.isValidUrl("https://localhost:9000"), is(true));

    assertThat(page.isValidUrl("http://localhost:9000/"), is(false)); // ends on slash
    assertThat(page.isValidUrl("http:/localhost:9000"), is(false)); // missing slash in protocol
    assertThat(page.isValidUrl("http://localhost:9000:9000"), is(false)); // MailformedURLException
  }
}
