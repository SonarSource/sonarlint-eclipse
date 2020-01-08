/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2020 SonarSource SA
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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.its.bots.ServerConnectionWizardBot;
import org.sonarqube.ws.WsUserTokens.GenerateWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.user.CreateRequest;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationsCheckboxTest extends AbstractSonarLintTest {

  private static final String SONARLINT_USER = "sonarlint";
  private static final String SONARLINT_PWD = "sonarlintpwd";

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[6.7]"))
    .build();

  private static WsClient adminWsClient;
  private static String token;

  @BeforeClass
  public static void prepare() {
    adminWsClient = newAdminWsClient(orchestrator);

    adminWsClient.users().create(CreateRequest.builder()
      .setLogin(SONARLINT_USER)
      .setPassword(SONARLINT_PWD)
      .setName("SonarLint")
      .build());

    GenerateWsResponse wsResponse = adminWsClient.userTokens().generate(new GenerateWsRequest()
      .setLogin(SONARLINT_USER)
      .setName("For SonarLint"));
    token = wsResponse.getToken();
  }

  @Test
  public void configureServerWithoutNotifications() {
    ServerConnectionWizardBot wizardBot = new ServerConnectionWizardBot(bot);
    wizardBot.openFromFileNewWizard();

    wizardBot.selectSonarQube();
    wizardBot.clickNext();

    wizardBot.setServerUrl(orchestrator.getServer().getUrl());
    wizardBot.clickNext();

    wizardBot.selectToken();
    wizardBot.clickNext();

    wizardBot.setToken(token);
    wizardBot.clickNext();

    String connectionName = "local";
    wizardBot.setConnectionName(connectionName);
    wizardBot.clickNext();

    wizardBot.waitForNotificationSupportCheckToBeFetched();

    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.clickFinish();

    waitForServerUpdate(connectionName, orchestrator, false);
  }
}
