/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2019 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.its;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.its.bots.ServerConnectionWizardBot;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.setting.SetRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarQubeConnectedModeTest extends AbstractSonarLintTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[6.7]"))
    .build();

  private static WsClient adminWsClient;

  @BeforeClass
  public static void prepare() {
    adminWsClient = newAdminWsClient(orchestrator);
    adminWsClient.settingsService().set(SetRequest.builder().setKey("sonar.forceAuthentication").setValue("true").build());
  }

  @Test
  public void configureServerFromNewWizard() {
    ServerConnectionWizardBot wizardBot = new ServerConnectionWizardBot(bot);
    wizardBot.openFromFileNewWizard();

    wizardBot.assertTitle("Connect to SonarQube or SonarCloud");

    wizardBot.selectSonarQube();
    wizardBot.clickNext();

    assertThat(wizardBot.isNextEnabled()).isFalse();

    wizardBot.setServerUrl("Foo");
    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.assertErrorMessage("This is not a valid URL");

    wizardBot.setServerUrl("http://");
    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.assertErrorMessage("Please provide a valid URL");

    wizardBot.setServerUrl("");
    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.assertErrorMessage("You must provide a server URL");

    wizardBot.setServerUrl(orchestrator.getServer().getUrl());
    assertThat(wizardBot.isNextEnabled()).isTrue();
    wizardBot.clickNext();

    wizardBot.selectUsernamePassword();
    wizardBot.clickNext();

    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.setUsername(Server.ADMIN_LOGIN);
    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.setPassword("wrong");
    assertThat(wizardBot.isNextEnabled()).isTrue();

    wizardBot.clickNext();
    wizardBot.assertErrorMessage("Authentication failed");

    wizardBot.setPassword(Server.ADMIN_PASSWORD);
    wizardBot.clickNext();

    assertThat(wizardBot.getConnectionName()).isEqualTo("127.0.0.1");
    assertThat(wizardBot.isNextEnabled()).isTrue();

    wizardBot.setConnectionName("");
    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.assertErrorMessage("Connection name must be specified");

    wizardBot.setConnectionName("test");
    wizardBot.clickNext();

    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.clickFinish();

    waitForServerUpdate("test", orchestrator, false);
  }

}
