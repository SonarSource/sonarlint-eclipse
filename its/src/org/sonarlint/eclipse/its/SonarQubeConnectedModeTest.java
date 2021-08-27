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
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.JavaUI;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.its.bots.JavaPackageExplorerBot;
import org.sonarlint.eclipse.its.bots.ProjectBindingWizardBot;
import org.sonarlint.eclipse.its.bots.ServerConnectionWizardBot;
import org.sonarlint.eclipse.its.bots.ServersViewBot;
import org.sonarlint.eclipse.its.utils.JobHelpers;
import org.sonarlint.eclipse.its.utils.SwtBotUtils;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.project.CreateRequest;
import org.sonarqube.ws.client.setting.SetRequest;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;

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

    if (orchestrator.getServer().version().isGreaterThanOrEquals(8, 7)) {
      // SONAR-14306 Starting from 8.7, dev notifications are available even in community edition
      assertThat(wizardBot.getNotificationEnabled()).isTrue();
      assertThat(wizardBot.isNextEnabled()).isTrue();
      wizardBot.clickNext();
    }

    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.clickFinish();

    new ServersViewBot(bot)
      .waitForServerUpdateAndCheckVersion("test", orchestrator.getServer().version().toString());

    bot.shell("Bind to a SonarQube or SonarCloud project").close();
  }

  @Test
  public void testLocalServerStatusRequest() throws Exception {

    HttpURLConnection statusConnection = (HttpURLConnection) new URL(String.format("http://localhost:%d/sonarlint/api/status", hostspotServerPort)).openConnection();
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

  @Test
  public void shouldFindSecretsInSourceFiles() throws Exception {
    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);
    IProject project = importEclipseProject("secrets/secret-java", PROJECT_NAME);
    JobHelpers.waitForJobsToComplete(bot);
    ServerConnectionWizardBot wizardBot = new ServerConnectionWizardBot(bot);
    wizardBot.openFromFileNewWizard();
    wizardBot.selectSonarQube();
    wizardBot.clickNext();
    wizardBot.setServerUrl(orchestrator.getServer().getUrl());
    wizardBot.clickNext();
    wizardBot.selectUsernamePassword();
    wizardBot.clickNext();
    wizardBot.setUsername(Server.ADMIN_LOGIN);
    wizardBot.setPassword(Server.ADMIN_PASSWORD);
    wizardBot.clickNext();
    wizardBot.setConnectionName("test1");
    wizardBot.clickNext();
    if (orchestrator.getServer().version().isGreaterThanOrEquals(8, 7)) {
      // SONAR-14306 Starting from 8.7, dev notifications are available even in community edition
      wizardBot.clickNext();
    }
    wizardBot.clickFinish();


    new ProjectBindingWizardBot(bot)
      .clickAdd()
      .chooseProject(PROJECT_NAME)
      .clickNext()
      .typeProjectKey(PROJECT_NAME)
      .clickFinish();

    new ServersViewBot(bot)
      .waitForServerUpdateAndCheckVersion("test", orchestrator.getServer().version().toString());

    new JavaPackageExplorerBot(bot)
      .expandAndDoubleClick("secret-java", "src", "sec", "Secret.java");
    JobHelpers.waitForJobsToComplete(bot);

    List<IMarker> markers = Arrays.asList(project.findMember("src/sec/Secret.java").findMarkers(MARKER_ON_THE_FLY_ID, true, IResource.DEPTH_ONE));
    assertThat(markers).extracting(markerAttributes(IMarker.LINE_NUMBER, IMarker.MESSAGE)).containsOnly(
      tuple("/secret-java/src/sec/Secret.java", 4, "AWS Secret Access Key detected here. Remove this credential from your code."));
  }

}
