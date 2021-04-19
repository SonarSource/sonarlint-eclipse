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
import org.eclipse.swt.widgets.Label;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.its.reddeer.conditions.DialogMessageIsExpected;
import org.sonarlint.eclipse.its.reddeer.views.BindingsView;
import org.sonarlint.eclipse.its.reddeer.wizards.ProjectBindingWizard;
import org.sonarlint.eclipse.its.reddeer.wizards.ServerConnectionWizard;
import org.sonarlint.eclipse.its.reddeer.wizards.ServerConnectionWizard.ServerUrlPage;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.setting.SetRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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
    adminWsClient.settings().set(SetRequest.builder().setKey("sonar.forceAuthentication").setValue("true").build());
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

      assertThat(response.asObject().iterator()).toIterable().extracting(JsonObject.Member::getName, m -> m.getValue().asString()).containsOnly(
        tuple("ideName", "Eclipse"),
        tuple("description", ""));
    }
  }

}
