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
import com.sonar.orchestrator.http.HttpMethod;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.its.bots.ServerConnectionWizardBot;
import org.sonarqube.ws.WsUserTokens.GenerateWsResponse;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.organization.CreateWsRequest;
import org.sonarqube.ws.client.user.CreateRequest;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarCloudConnectedModeTest extends AbstractSonarLintTest {

  private static final String SONARLINT_USER = "sonarlint";
  private static final String SONARLINT_PWD = "sonarlintpwd";
  private static final String ORGANIZATION_KEY = "test-org";
  private static final String ORGANIZATION_NAME = "Test organization";

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setServerProperty("sonar.sonarcloud.enabled", "true")
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[6.7]"))
    .build();

  private static WsClient adminWsClient;
  private static String token;

  @BeforeClass
  public static void prepare() {
    // Fake SonarCloud
    System.setProperty("sonarlint.internal.sonarcloud.url", orchestrator.getServer().getUrl());
    adminWsClient = newAdminWsClient(orchestrator);

    adminWsClient.users().create(CreateRequest.builder()
      .setLogin(SONARLINT_USER)
      .setPassword(SONARLINT_PWD)
      .setName("SonarLint")
      .build());

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

    wizardBot.assertTitle("Connect to SonarQube or SonarCloud");

    wizardBot.selectSonarCloud();
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

    assertThat(wizardBot.getConnectionName()).isEqualTo("SonarCloud/" + ORGANIZATION_KEY);
    wizardBot.setConnectionName("testWithOrga");
    wizardBot.clickNext();

    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.clickFinish();

    waitForServerUpdate("testWithOrga", orchestrator, true);
  }

  public static void enableOrganizationsSupport() {
    orchestrator.getServer().newHttpCall("/api/organizations/enable_support")
      .setMethod(HttpMethod.POST)
      .setAdminCredentials()
      .execute();
  }

  private static void createOrganization() {
    adminWsClient.organizations().create(new CreateWsRequest.Builder().setKey(ORGANIZATION_KEY).setName(ORGANIZATION_NAME).build());
    adminWsClient.organizations().addMember(ORGANIZATION_KEY, SONARLINT_USER);
  }

}
