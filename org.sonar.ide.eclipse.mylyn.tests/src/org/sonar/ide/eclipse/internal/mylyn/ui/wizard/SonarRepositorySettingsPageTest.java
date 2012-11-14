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
package org.sonar.ide.eclipse.internal.mylyn.ui.wizard;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@Ignore("Requires UI")
public class SonarRepositorySettingsPageTest {
  private SonarRepositorySettingsPage page;

  @Before
  public void setUp() {
    page = new SonarRepositorySettingsPage(null);
  }

  @Test
  public void should_accept_valid_urls() {
    assertThat(page.isValidUrl("http://localhost"), is(true));
    assertThat(page.isValidUrl("https://localhost:9000"), is(true));
  }

  @Test
  public void should_reject_invalid_urls() {
    assertThat(page.isValidUrl("http://localhost:9000/"), is(false)); // ends on slash
    assertThat(page.isValidUrl("http:/localhost:9000"), is(false)); // missing slash in protocol
    assertThat(page.isValidUrl("http://localhost:9000:9000"), is(false)); // MailformedURLException
  }
}
