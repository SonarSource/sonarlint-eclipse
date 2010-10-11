/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

package org.sonar.ide.eclipse.ui.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.ui.tests.bots.SonarPreferencesBot;
import org.sonar.ide.eclipse.ui.tests.bots.SonarServerWizardBot;
import org.sonar.ide.eclipse.ui.tests.utils.ProjectUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ConfigurationTest extends UITestCase {

  private static SonarPreferencesBot preferencesBot;

  @BeforeClass
  public static void openProperties() throws Exception {
    // Remove all configured servers
    SonarPlugin.getServerManager().clean();

    preferencesBot = new SonarPreferencesBot();
  }

  @AfterClass
  public static void closeProperties() {
    preferencesBot.ok();
  }

  @Test
  public void test() throws Exception {
    assertThat(preferencesBot.getServersCount(), is(0));

    // can add
    SonarServerWizardBot addWizard = preferencesBot.add();
    assertThat(addWizard.getServerUrl(), is("http://localhost:9000")); // default url
    assertThat(addWizard.getUsername(), is(""));
    assertThat(addWizard.getPassword(), is(""));
    testConnection(addWizard, ProjectUtils.getSonarServerUrl() + "/", true); // test for SONARIDE-90
    testConnection(addWizard, ProjectUtils.getSonarServerUrl(), true);
    testConnection(addWizard, "http://fake", false);
    addWizard.finish();
    assertThat(preferencesBot.getServersCount(), is(1));

    // can edit
    SonarServerWizardBot editWizard = preferencesBot.select("http://fake").edit();
    assertThat(editWizard.getServerUrl(), is("http://fake"));
    assertThat(editWizard.getUsername(), is(""));
    assertThat(editWizard.getPassword(), is(""));
    editWizard.setServerUrl("http://fake2");
    editWizard.finish();
    assertThat(preferencesBot.getServersCount(), is(1));

    // can remove
    preferencesBot.select("http://fake2").remove();
    assertThat(preferencesBot.getServersCount(), is(0));
  }

  private void testConnection(SonarServerWizardBot addWizard, String serverUrl, boolean expectedSuccess) {
    String message = expectedSuccess ? " Successfully connected!" : " Unable to connect.";
    assertThat(addWizard.setServerUrl(serverUrl).testConnection().getStatus(), is(message));
  }
}
