/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.user.UserParameters;
import org.sonarlint.eclipse.its.bots.ServerConnectionWizardBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarqube.ws.WsUserTokens.GenerateWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class NotificationsCheckboxTest extends AbstractSonarLintTest {

  private static final String SONARLINT_USER = "sonarlint";
  private static final String SONARLINT_PWD = "sonarlintpwd";

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .build();

  private static WsClient adminWsClient;
  private static String token;

  @BeforeClass
  public static void prepare() {
    assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals("6.3"));
    adminWsClient = newAdminWsClient(orchestrator);

    orchestrator.getServer().adminWsClient().userClient()
      .create(UserParameters.create()
        .login(SONARLINT_USER)
        .password(SONARLINT_PWD)
        .passwordConfirmation(SONARLINT_PWD)
        .name("SonarLint"));

    GenerateWsResponse wsResponse = adminWsClient.userTokens().generate(new GenerateWsRequest()
      .setLogin(SONARLINT_USER)
      .setName("For SonarLint"));
    token = wsResponse.getToken();
  }

  @Test
  public void configureServerWithTokenAndOrganization() {
    ServerConnectionWizardBot wizardBot = new ServerConnectionWizardBot(bot);
    wizardBot.openFromFileNewWizard();

    wizardBot.assertTitle("Connect to a SonarQube Server");

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

    JobHelpers.waitForServerUpdateJob(bot);

    SWTBotView serversView = bot.viewById("org.sonarlint.eclipse.ui.ServersView");
    assertThat(serversView.bot().tree().getAllItems()[0].getText()).matches(connectionName + " \\[Version: " + orchestrator.getServer().version() + "(.*), Last update: (.*)\\]");
  }
}
