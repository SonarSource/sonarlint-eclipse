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

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.api.Shell;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.editor.Marker;
import org.eclipse.swt.widgets.Label;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.its.reddeer.conditions.DialogMessageIsExpected;
import org.sonarlint.eclipse.its.reddeer.views.BindingsView;
import org.sonarlint.eclipse.its.reddeer.wizards.ProjectBindingWizard;
import org.sonarlint.eclipse.its.reddeer.wizards.ProjectSelectionDialog;
import org.sonarlint.eclipse.its.reddeer.wizards.ServerConnectionWizard;
import org.sonarlint.eclipse.its.reddeer.wizards.ServerConnectionWizard.ServerUrlPage;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.project.CreateRequest;
import org.sonarqube.ws.client.setting.SetRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class SonarQubeConnectedModeTest extends AbstractSonarLintTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[7.9]"))
    .build();

  private static WsClient adminWsClient;

  private static final String PROJECT_NAME = "secret-java";

  @BeforeClass
  public static void prepare() {
    adminWsClient = newAdminWsClient(orchestrator);
    adminWsClient.settings().set(SetRequest.builder().setKey("sonar.forceAuthentication").setValue("true").build());
    adminWsClient.projects()
      .create(CreateRequest.builder()
        .setName(PROJECT_NAME)
        .setKey(PROJECT_NAME).build());
  }

  @Override
  @Before
  public void cleanup() {
    BindingsView bindingsView = new BindingsView();
    bindingsView.open();
    bindingsView.removeAllBindings();
  }

  @Test
  public void configureServerFromNewWizard() {
    ServerConnectionWizard wizard = new ServerConnectionWizard();
    wizard.open();
    new ServerConnectionWizard.ServerTypePage(wizard).selectSonarQube();
    wizard.next();

    assertThat(wizard.isNextEnabled()).isFalse();
    ServerUrlPage serverUrlPage = new ServerConnectionWizard.ServerUrlPage(wizard);

    serverUrlPage.setUrl("Foo");
    assertThat(wizard.isNextEnabled()).isFalse();
    new WaitUntil(new DialogMessageIsExpected(wizard, "This is not a valid URL"));

    serverUrlPage.setUrl("http://");
    assertThat(wizard.isNextEnabled()).isFalse();
    new WaitUntil(new DialogMessageIsExpected(wizard, "Please provide a valid URL"));

    serverUrlPage.setUrl("");
    assertThat(wizard.isNextEnabled()).isFalse();
    new WaitUntil(new DialogMessageIsExpected(wizard, "You must provide a server URL"));

    serverUrlPage.setUrl(orchestrator.getServer().getUrl());
    assertThat(wizard.isNextEnabled()).isTrue();
    wizard.next();

    ServerConnectionWizard.AuthenticationModePage authenticationModePage = new ServerConnectionWizard.AuthenticationModePage(wizard);
    authenticationModePage.selectUsernamePasswordMode();
    wizard.next();

    ServerConnectionWizard.AuthenticationPage authenticationPage = new ServerConnectionWizard.AuthenticationPage(wizard);
    assertThat(wizard.isNextEnabled()).isFalse();
    authenticationPage.setUsername(Server.ADMIN_LOGIN);
    assertThat(wizard.isNextEnabled()).isFalse();
    authenticationPage.setPassword("wrong");
    assertThat(wizard.isNextEnabled()).isTrue();

    wizard.next();
    new WaitUntil(new DialogMessageIsExpected(wizard, "Authentication failed"));

    authenticationPage.setPassword(Server.ADMIN_PASSWORD);
    wizard.next();

    // as login can take time, wait for the next page to appear
    new WaitUntil(new WidgetIsFound(Label.class, new WithTextMatcher("SonarQube Connection Identifier")));
    ServerConnectionWizard.ConnectionNamePage connectionNamePage = new ServerConnectionWizard.ConnectionNamePage(wizard);

    assertThat(connectionNamePage.getConnectionName()).isEqualTo("127.0.0.1");
    assertThat(wizard.isNextEnabled()).isTrue();

    connectionNamePage.setConnectionName("");
    assertThat(wizard.isNextEnabled()).isFalse();
    new WaitUntil(new DialogMessageIsExpected(wizard, "Connection name must be specified"));

    connectionNamePage.setConnectionName("test");
    wizard.next();

    if (orchestrator.getServer().version().isGreaterThanOrEquals(8, 7)) {
      // SONAR-14306 Starting from 8.7, dev notifications are available even in community edition
      ServerConnectionWizard.NotificationsPage notificationsPage = new ServerConnectionWizard.NotificationsPage(wizard);
      assertThat(notificationsPage.areNotificationsEnabled()).isTrue();
      assertThat(wizard.isNextEnabled()).isTrue();
      wizard.next();
    }

    assertThat(wizard.isNextEnabled()).isFalse();
    wizard.finish();

    new ProjectBindingWizard().cancel();

    BindingsView bindingsView = new BindingsView();
    bindingsView.waitForServerUpdate("test", orchestrator.getServer().version().toString());
  }

  @Test
  public void testLocalServerStatusRequest() throws Exception {
    assertThat(hotspotServerPort).isNotEqualTo(-1);
    HttpURLConnection statusConnection = (HttpURLConnection) new URL(String.format("http://localhost:%d/sonarlint/api/status", hotspotServerPort)).openConnection();
    statusConnection.setConnectTimeout(1000);
    statusConnection.connect();
    int code = statusConnection.getResponseCode();
    assertThat(code).isEqualTo(200);
    try (InputStream inputStream = statusConnection.getInputStream()) {
      JsonValue response = Json.parse(new InputStreamReader(inputStream));

      assertThat(response.asObject().iterator()).toIterable().extracting(JsonObject.Member::getName, m -> m.getValue().asString())
        .hasSize(2)
        .contains(
          tuple("description", ""))
        // When running tests locally the ideName is "Eclipse Platform" for some reason
        .containsAnyOf(tuple("ideName", "Eclipse"), tuple("ideName", "Eclipse Platform"));
    }
  }

  @Test
  public void shouldFindSecretsInConnectedMode() throws Exception {
    new JavaPerspective().open();
    Project rootProject = importExistingProjectIntoWorkspace("secrets/secret-java", PROJECT_NAME);

    ServerConnectionWizard wizard = new ServerConnectionWizard();
    wizard.open();
    new ServerConnectionWizard.ServerTypePage(wizard).selectSonarQube();
    wizard.next();

    ServerUrlPage serverUrlPage = new ServerConnectionWizard.ServerUrlPage(wizard);
    serverUrlPage.setUrl(orchestrator.getServer().getUrl());
    wizard.next();

    ServerConnectionWizard.AuthenticationModePage authenticationModePage = new ServerConnectionWizard.AuthenticationModePage(wizard);
    authenticationModePage.selectUsernamePasswordMode();
    wizard.next();

    ServerConnectionWizard.AuthenticationPage authenticationPage = new ServerConnectionWizard.AuthenticationPage(wizard);
    authenticationPage.setUsername(Server.ADMIN_LOGIN);
    authenticationPage.setPassword(Server.ADMIN_PASSWORD);
    wizard.next();

    // as login can take time, wait for the next page to appear
    new WaitUntil(new WidgetIsFound(Label.class, new WithTextMatcher("SonarQube Connection Identifier")));
    ServerConnectionWizard.ConnectionNamePage connectionNamePage = new ServerConnectionWizard.ConnectionNamePage(wizard);

    connectionNamePage.setConnectionName("test");
    wizard.next();

    if (orchestrator.getServer().version().isGreaterThanOrEquals(8, 7)) {
      // SONAR-14306 Starting from 8.7, dev notifications are available even in community edition
      wizard.next();
    }

    wizard.finish();

    ProjectBindingWizard projectBindingWizard = new ProjectBindingWizard();
    ProjectBindingWizard.BoundProjectsPage projectsToBindPage = new ProjectBindingWizard.BoundProjectsPage(projectBindingWizard);
    projectsToBindPage.clickAdd();

    ProjectSelectionDialog projectSelectionDialog = new ProjectSelectionDialog();
    projectSelectionDialog.setProjectName(PROJECT_NAME);
    projectSelectionDialog.ok();

    projectBindingWizard.next();
    ProjectBindingWizard.ServerProjectSelectionPage serverProjectSelectionPage = new ProjectBindingWizard.ServerProjectSelectionPage(projectBindingWizard);
    serverProjectSelectionPage.waitForProjectsToBeFetched();
    serverProjectSelectionPage.setProjectKey(PROJECT_NAME);
    projectBindingWizard.finish();

    BindingsView bindingsView = new BindingsView();
    bindingsView.open();
    bindingsView.waitForServerUpdate("test", orchestrator.getServer().version().toString());

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "sec", "Secret.java"));

    DefaultEditor defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Make sure this AWS Secret Access Key is not disclosed.", 4));

    Shell preferencesShell = new DefaultShell("SonarLint - Secret(s) detected");
    preferencesShell.close();
  }

}
