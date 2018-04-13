/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2018 SonarSource SA
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
import com.sonar.orchestrator.http.HttpMethod;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.user.UserParameters;
import org.sonarlint.eclipse.its.bots.ServerConnectionWizardBot;
import org.sonarqube.ws.WsUserTokens.GenerateWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

public class ConnectedModeWithOrgaTest extends AbstractSonarLintTest {

  private static final String SONARLINT_USER = "sonarlint";
  private static final String SONARLINT_PWD = "sonarlintpwd";
  private static final String ORGANIZATION_KEY = "test-org";
  private static final String ORGANIZATION_NAME = "Test organization";

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    // Need at least one plugin to avoid bug SONAR-8918
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
    .setServerProperty("sonar.sonarcloud.enabled", "true")
    .addPlugin("java")
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

    enableOrganizationsSupport();
    createOrganization();

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

    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.setToken("Foo");
    assertThat(wizardBot.isNextEnabled()).isTrue();

    wizardBot.clickNext();
    wizardBot.assertErrorMessage("Authentication failed");

    wizardBot.setToken(token);
    wizardBot.clickNext();
    wizardBot.waitForOrganizationsToBeFetched();

    wizardBot.typeOrganizationAndSelectFirst("test");
    assertThat(wizardBot.getOrganization()).isEqualTo(ORGANIZATION_KEY);
    assertThat(wizardBot.isNextEnabled()).isTrue();
    wizardBot.clickNext();

    assertThat(wizardBot.getConnectionName()).isEqualTo("127.0.0.1/" + ORGANIZATION_KEY);
    wizardBot.setConnectionName("testWithOrga");
    wizardBot.clickNext();

    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.clickFinish();

    SWTBotView serversView = bot.viewById("org.sonarlint.eclipse.ui.ServersView");
    final SWTBotTreeItem serverCell = serversView.bot().tree().getAllItems()[0];
    bot.waitUntil(new DefaultCondition() {
      @Override
      public boolean test() throws Exception {
        return UIThreadRunnable.syncExec(new BoolResult() {
          @Override
          public Boolean run() {
            return serverCell.getText().matches("testWithOrga \\[Version: " + orchestrator.getServer().version() + "(.*), Last update: (.*)\\]");
          }
        });
      };
      
      @Override
      public String getFailureMessage() {
        return "Server status is: " + serverCell.getText();
      }
    }, 20_000);
  }

  public static void enableOrganizationsSupport() {
    orchestrator.getServer().newHttpCall("/api/organizations/enable_support")
      .setMethod(HttpMethod.POST)
      .setAdminCredentials()
      .execute();
  }

  private static void createOrganization() {
    adminWsClient.organizations().create(new CreateWsRequest.Builder().setKey(ORGANIZATION_KEY).setName(ORGANIZATION_NAME).build());
  }

}
