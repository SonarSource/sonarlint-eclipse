/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.servers.SonarServersManager;
import org.sonar.ide.eclipse.ui.its.bots.SonarServerPreferencesBot;
import org.sonar.ide.eclipse.ui.its.bots.SonarServerWizardBot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@SuppressWarnings("restriction")
public class ConfigurationTest extends AbstractSQEclipseUITest {

  private static SonarServerPreferencesBot preferencesBot;

  @BeforeClass
  public static void openProperties() throws Exception {
    // Remove all configured servers
    ((SonarServersManager) SonarCorePlugin.getServersManager()).clean();

    preferencesBot = new SonarServerPreferencesBot(bot);
  }

  @AfterClass
  public static void closeProperties() {
    preferencesBot.ok();
  }

  @Test
  public void testServerConfigurationWizard() throws Exception {
    assertThat(preferencesBot.getServersCount(), is(0));

    // can add
    SonarServerWizardBot addWizard = preferencesBot.add();
    assertThat(addWizard.getServerId(), is(""));
    assertThat(addWizard.getServerUrl(), is("http://")); // default url
    assertThat(addWizard.getUsername(), is(""));
    assertThat(addWizard.getPassword(), is(""));
    testConnection(addWizard, getSonarServerUrl() + "/", true); // test for SONARIDE-90
    testConnection(addWizard, getSonarServerUrl(), true);
    testConnection(addWizard, "http://fake", false);
    addWizard.finish();
    assertThat(preferencesBot.getServersCount(), is(1));

    // can edit
    SonarServerWizardBot editWizard = preferencesBot.select("fake").edit();
    assertThat(editWizard.getServerId()).isEqualTo("fake");
    assertThat(editWizard.getServerUrl()).isEqualTo("http://fake");
    assertThat(editWizard.getUsername()).isEmpty();
    assertThat(editWizard.getPassword()).isEmpty();
    editWizard.setServerUrl("http://fake2");
    assertThat(editWizard.getServerId()).isEqualTo("fake");
    editWizard.finish();
    assertThat(preferencesBot.getServersCount(), is(1));

    // can remove
    preferencesBot.select("fake").remove();
    assertThat(preferencesBot.getServersCount(), is(0));
  }

  private void testConnection(SonarServerWizardBot addWizard, String serverUrl, boolean expectedSuccess) {
    String message = expectedSuccess ? "Successfully connected!" : "Unable to connect:";
    assertThat(addWizard.setServerUrl(serverUrl).testConnection().getStatus()).contains(message);
  }
}
