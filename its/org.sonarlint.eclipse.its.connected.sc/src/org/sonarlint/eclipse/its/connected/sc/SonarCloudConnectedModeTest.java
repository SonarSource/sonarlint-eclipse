/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonarlint.eclipse.its.connected.sc;

import java.time.Instant;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.DialogMessageIsExpected;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.ProjectBindingWizardIsOpened;
import org.sonarlint.eclipse.its.shared.reddeer.wizards.ProjectBindingWizard;
import org.sonarlint.eclipse.its.shared.reddeer.wizards.ServerConnectionWizard;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.usertokens.GenerateRequest;
import org.sonarqube.ws.client.usertokens.RevokeRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class SonarCloudConnectedModeTest extends AbstractSonarLintTest {
  private static final String TIMESTAMP = Long.toString(Instant.now().toEpochMilli());
  private static final String SONARCLOUD_STAGING_URL = "https://sc-staging.io";
  private static final String SONARCLOUD_ORGANIZATION_KEY = "sonarlint-it";
  private static final String SONARCLOUD_USER = "sonarlint-it";
  private static final String SONARCLOUD_PASSWORD = System.getenv("SONARCLOUD_IT_PASSWORD");

  private static final String TOKEN_NAME = "SLE-IT-" + TIMESTAMP;

  private static final String CONNECTION_NAME = "connection";

  private static WsClient adminWsClient;
  private static String token;

  @BeforeClass
  public static void prepare() {
    adminWsClient = WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(SONARCLOUD_STAGING_URL)
      .credentials(SONARCLOUD_USER, SONARCLOUD_PASSWORD)
      .build());

    token = adminWsClient.userTokens()
      .generate(new GenerateRequest().setName(TOKEN_NAME))
      .getToken();
  }

  @AfterClass
  public static void cleanupOrchestrator() {
    adminWsClient.userTokens()
      .revoke(new RevokeRequest().setName(TOKEN_NAME));
  }

  @Test
  public void configureServerWithTokenAndOrganization() throws InterruptedException {
    var wizard = new ServerConnectionWizard();
    wizard.open();
    new ServerConnectionWizard.ServerTypePage(wizard).selectSonarCloud();
    wizard.next();

    assertThat(wizard.isNextEnabled()).isFalse();
    var authenticationPage = new ServerConnectionWizard.AuthenticationPage(wizard);
    authenticationPage.setToken("Foo");
    assertThat(wizard.isNextEnabled()).isTrue();

    wizard.next();
    new WaitUntil(new DialogMessageIsExpected(wizard, "Authentication failed"));

    authenticationPage.setToken("");
    assertThat(wizard.isNextEnabled()).isFalse();
    new WaitUntil(new DialogMessageIsExpected(wizard, "You must provide an authentication token"));

    authenticationPage.setToken(token);
    wizard.next();

    var organizationsPage = new ServerConnectionWizard.OrganizationsPage(wizard);
    organizationsPage.waitForOrganizationsToBeFetched();

    assertThat(organizationsPage.getOrganization()).isEqualTo(SONARCLOUD_ORGANIZATION_KEY);

    organizationsPage.setOrganization(SONARCLOUD_ORGANIZATION_KEY);
    assertThat(wizard.isNextEnabled()).isTrue();
    wizard.next();

    var connectionNamePage = new ServerConnectionWizard.ConnectionNamePage(wizard);
    assertThat(connectionNamePage.getConnectionName()).isEqualTo(SONARCLOUD_ORGANIZATION_KEY);
    connectionNamePage.setConnectionName(CONNECTION_NAME);
    assertThat(wizard.isNextEnabled()).isTrue();

    // Sadly we have to invoke sleep here as in the background there is SL communicating with SC regarding the
    // availability of notifications "on the server". As this is not done in a job we could listen to, we wait the
    // 5 seconds. Once we change it in SonarLint to not ask for notifications (for all supported SQ versions and SC
    // they are supported by now), we can somehow circumvent this.
    Thread.sleep(5000);
    wizard.next();

    var notificationsPage = new ServerConnectionWizard.NotificationsPage(wizard);
    assertThat(notificationsPage.areNotificationsEnabled()).isTrue();
    assertThat(wizard.isNextEnabled()).isTrue();
    wizard.next();

    assertThat(wizard.isNextEnabled()).isFalse();
    // Because of the binding background job that is triggered we have to wait here for the project binding wizard to
    // appear. It might happen that the new wizards opens over the old one before it closes, but this is okay as the
    // old one will close itself lazily.
    wizard.finish(TimePeriod.VERY_LONG);
    new WaitUntil(new ProjectBindingWizardIsOpened());

    // Close project binding wizard as we don't have a project opened
    var projectBindingWizard = new ProjectBindingWizard();
    new ProjectBindingWizard.BoundProjectsPage(projectBindingWizard);
    projectBindingWizard.cancel();
  }
}
