/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2021 SonarSource SA
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

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonarlint.eclipse.its.bots.ProjectBindingWizardBot;
import org.sonarlint.eclipse.its.bots.ServerConnectionWizardBot;
import org.sonarlint.eclipse.its.bots.ServersViewBot;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.project.CreateRequest;
import org.sonarqube.ws.client.project.DeleteRequest;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;
import org.sonarqube.ws.client.usertoken.RevokeWsRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SonarCloud.class)
public class SonarCloudConnectedModeTest extends AbstractSonarLintTest {
  private static final String IMPORTED_PROJECT_NAME = "java-simple";
  private static final String TIMESTAMP = Long.toString(Instant.now().toEpochMilli());

  private static final String SONARCLOUD_STAGING_URL = "https://sc-staging.io";
  private static final String SONARCLOUD_ORGANIZATION_KEY = "sonarlint-it";
  private static final String SONARCLOUD_ORGANIZATION_NAME = "SonarLint IT Tests";
  private static final String SONARCLOUD_USER = "sonarlint-it";
  private static final String SONARCLOUD_PASSWORD = System.getenv("SONARCLOUD_IT_PASSWORD");
  private static final String SONARCLOUD_PROJECT_KEY = IMPORTED_PROJECT_NAME + '-' + TIMESTAMP;

  private static final String TOKEN_NAME = "SLE-IT-" + TIMESTAMP;

  private static final String CONNECTION_NAME = "connection";

  private static WsClient adminWsClient;
  private static String token;

  @BeforeClass
  public static void prepare() {
    System.setProperty("sonarlint.internal.sonarcloud.url", SONARCLOUD_STAGING_URL);
    adminWsClient = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(SONARCLOUD_STAGING_URL)
      .credentials(SONARCLOUD_USER, SONARCLOUD_PASSWORD)
      .build());

    token = adminWsClient.userTokens()
      .generate(new GenerateWsRequest().setName(TOKEN_NAME))
      .getToken();

    adminWsClient.projects()
      .create(CreateRequest.builder()
        .setName(IMPORTED_PROJECT_NAME)
        .setKey(SONARCLOUD_PROJECT_KEY)
        .setOrganization(SONARCLOUD_ORGANIZATION_KEY).build());
  }

  @AfterClass
  public static void cleanup() {
    adminWsClient.userTokens()
      .revoke(new RevokeWsRequest().setName(TOKEN_NAME));
    adminWsClient.projects()
      .delete(DeleteRequest.builder()
        .setKey(SONARCLOUD_PROJECT_KEY).build());
  }

  @Test
  public void configureServerWithTokenAndOrganization() throws InvocationTargetException, InterruptedException {
    importEclipseProject("java/java-simple", IMPORTED_PROJECT_NAME);

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

    assertThat(wizardBot.getOrganization()).isEqualTo(SONARCLOUD_ORGANIZATION_KEY);

    wizardBot.typeOrganizationAndSelectFirst(SONARCLOUD_ORGANIZATION_NAME);
    assertThat(wizardBot.getOrganization()).isEqualTo(SONARCLOUD_ORGANIZATION_KEY);
    assertThat(wizardBot.isNextEnabled()).isTrue();
    wizardBot.clickNext();

    assertThat(wizardBot.getConnectionName()).isEqualTo("SonarCloud/" + SONARCLOUD_ORGANIZATION_KEY);
    wizardBot.setConnectionName(CONNECTION_NAME);
    wizardBot.clickNext();

    assertThat(wizardBot.getNotificationEnabled()).isTrue();
    assertThat(wizardBot.isNextEnabled()).isTrue();
    wizardBot.clickNext();

    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.clickFinish();

    new ProjectBindingWizardBot(bot)
      .clickAdd()
      .chooseProject(IMPORTED_PROJECT_NAME)
      .clickNext()
      .waitForOrganizationProjectsToBeFetched()
      .typeProjectKey(SONARCLOUD_PROJECT_KEY)
      .clickFinish();

    new ServersViewBot(bot)
      .waitForServerUpdate(CONNECTION_NAME);
  }

}
