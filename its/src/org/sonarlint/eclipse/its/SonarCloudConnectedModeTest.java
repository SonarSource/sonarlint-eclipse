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

import java.time.Instant;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonarlint.eclipse.its.reddeer.conditions.DialogMessageIsExpected;
import org.sonarlint.eclipse.its.reddeer.views.BindingsView;
import org.sonarlint.eclipse.its.reddeer.wizards.ProjectBindingWizard;
import org.sonarlint.eclipse.its.reddeer.wizards.ProjectSelectionDialog;
import org.sonarlint.eclipse.its.reddeer.wizards.ServerConnectionWizard;
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
  //private static final String SONARCLOUD_ORGANIZATION_NAME = "SonarLint IT Tests";
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
  public static void cleanupOrchestrator() {
    adminWsClient.userTokens()
      .revoke(new RevokeWsRequest().setName(TOKEN_NAME));
    adminWsClient.projects()
      .delete(DeleteRequest.builder()
        .setKey(SONARCLOUD_PROJECT_KEY).build());
  }

  @Test
  public void configureServerWithTokenAndOrganization() {
    importExistingProjectIntoWorkspace("java/java-simple");

    ServerConnectionWizard wizard = new ServerConnectionWizard();
    wizard.open();
    new ServerConnectionWizard.ServerTypePage(wizard).selectSonarCloud();
    wizard.next();

    assertThat(wizard.isNextEnabled()).isFalse();
    ServerConnectionWizard.AuthenticationPage authenticationPage = new ServerConnectionWizard.AuthenticationPage(wizard);
    authenticationPage.setToken("Foo");
    assertThat(wizard.isNextEnabled()).isTrue();

    wizard.next();
    new WaitUntil(new DialogMessageIsExpected(wizard, "Authentication failed"));

    authenticationPage.setToken(token);
    wizard.next();

    ServerConnectionWizard.OrganizationsPage organizationsPage = new ServerConnectionWizard.OrganizationsPage(wizard);
    organizationsPage.waitForOrganizationsToBeFetched();

    assertThat(organizationsPage.getOrganization()).isEqualTo(SONARCLOUD_ORGANIZATION_KEY);

    //organizationsPage.typeOrganizationAndSelectFirst(SONARCLOUD_ORGANIZATION_NAME);
    //assertThat(organizationsPage.getOrganization()).isEqualTo(SONARCLOUD_ORGANIZATION_KEY);
    organizationsPage.setOrganization(SONARCLOUD_ORGANIZATION_KEY);
    assertThat(wizard.isNextEnabled()).isTrue();
    wizard.next();

    ServerConnectionWizard.ConnectionNamePage connectionNamePage = new ServerConnectionWizard.ConnectionNamePage(wizard);
    assertThat(connectionNamePage.getConnectionName()).isEqualTo("SonarCloud/" + SONARCLOUD_ORGANIZATION_KEY);
    connectionNamePage.setConnectionName(CONNECTION_NAME);
    wizard.next();

    ServerConnectionWizard.NotificationsPage notificationsPage = new ServerConnectionWizard.NotificationsPage(wizard);
    assertThat(notificationsPage.areNotificationsEnabled()).isTrue();
    assertThat(wizard.isNextEnabled()).isTrue();
    wizard.next();

    assertThat(wizard.isNextEnabled()).isFalse();
    wizard.finish();

    
    ProjectBindingWizard projectBindingWizard = new ProjectBindingWizard();
    ProjectBindingWizard.BoundProjectsPage projectsToBindPage = new ProjectBindingWizard.BoundProjectsPage(projectBindingWizard);
    projectsToBindPage.clickAdd();
    
    ProjectSelectionDialog projectSelectionDialog = new ProjectSelectionDialog();
    projectSelectionDialog.setProjectName(IMPORTED_PROJECT_NAME);
    projectSelectionDialog.ok();
    
    projectBindingWizard.next();
    ProjectBindingWizard.ServerProjectSelectionPage serverProjectSelectionPage = new ProjectBindingWizard.ServerProjectSelectionPage(projectBindingWizard);
    serverProjectSelectionPage.waitForProjectsToBeFetched();
    serverProjectSelectionPage.setProjectKey(SONARCLOUD_PROJECT_KEY);
    projectBindingWizard.finish();
    
    BindingsView bindingsView = new BindingsView();
    bindingsView.open();
    bindingsView.waitForServerUpdate(CONNECTION_NAME, null);
  }

}
