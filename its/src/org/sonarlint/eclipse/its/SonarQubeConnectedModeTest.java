/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2022 SonarSource SA
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

import com.google.gson.Gson;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.locator.URLLocation;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.editor.Marker;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.eclipse.swt.widgets.Label;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.sonarlint.eclipse.its.reddeer.conditions.DialogMessageIsExpected;
import org.sonarlint.eclipse.its.reddeer.views.BindingsView;
import org.sonarlint.eclipse.its.reddeer.views.BindingsView.Binding;
import org.sonarlint.eclipse.its.reddeer.wizards.ProjectBindingWizard;
import org.sonarlint.eclipse.its.reddeer.wizards.ProjectSelectionDialog;
import org.sonarlint.eclipse.its.reddeer.wizards.ServerConnectionWizard;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.project.CreateRequest;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;
import org.sonarqube.ws.client.setting.SetRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SonarQubeConnectedModeTest extends AbstractSonarLintTest {

  @ClassRule
  public static Orchestrator orchestrator;

  static {
    try {
      orchestrator = Orchestrator.builderEnv()
        .defaultForceAuthentication()
        .useDefaultAdminCredentialsForBuilds(true)
        .keepBundledPlugins()
        .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE[8.9]"))
        // Ensure SSE are processed correctly just after SQ startup
        .setServerProperty("sonar.pushevents.polling.initial.delay", "2")
        .setServerProperty("sonar.pushevents.polling.period", "1")
        .setServerProperty("sonar.pushevents.polling.last.timestamp", "1")
        .restoreProfileAtStartup(
          URLLocation.create(FileLocator.toFileURL(FileLocator.find(FrameworkUtil.getBundle(SonarQubeConnectedModeTest.class), new Path("res/java-sonarlint.xml"), null))))
        // Ensure SSE are processed correctly just after SQ startup
        .setServerProperty("sonar.pushevents.polling.initial.delay", "2")
        .setServerProperty("sonar.pushevents.polling.period", "1")
        .setServerProperty("sonar.pushevents.polling.last.timestamp", "1")
        .build();
    } catch (IOException e) {
      fail("Cannot start orchestrator: " + e.getMessage());
    }
  }

  private static WsClient adminWsClient;

  private static final String PROJECT_NAME = "secret-java";
  private static final String JAVA_SIMPLE_PROJECT_KEY = "java-simple";

  @BeforeClass
  public static void prepare() {
    adminWsClient = newAdminWsClient(orchestrator);
    adminWsClient.settings().set(SetRequest.builder().setKey("sonar.forceAuthentication").setValue("true").build());
    adminWsClient.projects()
      .create(CreateRequest.builder()
        .setName(PROJECT_NAME)
        .setKey(PROJECT_NAME).build());
    adminWsClient.projects()
      .create(CreateRequest.builder()
        .setName(JAVA_SIMPLE_PROJECT_KEY)
        .setKey(JAVA_SIMPLE_PROJECT_KEY).build());
    orchestrator.getServer().associateProjectToQualityProfile(JAVA_SIMPLE_PROJECT_KEY, "java", "SonarLint IT Java");
  }

  @Before
  public void cleanBindings() {
    var bindingsView = new BindingsView();
    bindingsView.open();
    bindingsView.removeAllBindings();
  }

  @Test
  public void configureServerFromNewWizard() {
    var wizard = new ServerConnectionWizard();
    wizard.open();
    new ServerConnectionWizard.ServerTypePage(wizard).selectSonarQube();
    wizard.next();

    assertThat(wizard.isNextEnabled()).isFalse();
    var serverUrlPage = new ServerConnectionWizard.ServerUrlPage(wizard);

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

    var authenticationModePage = new ServerConnectionWizard.AuthenticationModePage(wizard);
    authenticationModePage.selectUsernamePasswordMode();
    wizard.next();

    var authenticationPage = new ServerConnectionWizard.AuthenticationPage(wizard);
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
    var connectionNamePage = new ServerConnectionWizard.ConnectionNamePage(wizard);

    assertThat(connectionNamePage.getConnectionName()).isEqualTo("127.0.0.1");
    assertThat(wizard.isNextEnabled()).isTrue();

    connectionNamePage.setConnectionName("");
    assertThat(wizard.isNextEnabled()).isFalse();
    new WaitUntil(new DialogMessageIsExpected(wizard, "Connection name must be specified"));

    connectionNamePage.setConnectionName("test");
    wizard.next();

    if (orchestrator.getServer().version().isGreaterThanOrEquals(8, 7)) {
      // SONAR-14306 Starting from 8.7, dev notifications are available even in community edition
      var notificationsPage = new ServerConnectionWizard.NotificationsPage(wizard);
      assertThat(notificationsPage.areNotificationsEnabled()).isTrue();
      assertThat(wizard.isNextEnabled()).isTrue();
      wizard.next();
    }

    assertThat(wizard.isNextEnabled()).isFalse();
    wizard.finish();

    new ProjectBindingWizard().cancel();

    var bindingsView = new BindingsView();
    assertThat(bindingsView.getBindings()).extracting(Binding::getLabel).contains("test");
    new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
  }

  private static class Status {
    private String ideName;
    private String description;
  }

  @Test
  public void testLocalServerStatusRequest() throws Exception {
    assertThat(hotspotServerPort).isNotEqualTo(-1);
    var statusConnection = (HttpURLConnection) new URL(String.format("http://localhost:%d/sonarlint/api/status", hotspotServerPort)).openConnection();
    statusConnection.setConnectTimeout(1000);
    statusConnection.connect();
    int code = statusConnection.getResponseCode();
    assertThat(code).isEqualTo(200);
    try (var inputStream = statusConnection.getInputStream(); var reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      var response = new Gson().fromJson(reader, Status.class);
      assertThat(response.description).isEmpty();
      // When running tests locally the ideName is "Eclipse Platform" for some reason
      assertThat(response.ideName).isIn("Eclipse", "Eclipse Platform");
    }
  }

  @Test
  public void shouldFindSecretsInConnectedMode() {
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("secrets/secret-java", PROJECT_NAME);

    createConnectionAndBindProject(PROJECT_NAME);

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "sec", "Secret.java"));

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Make sure this AWS Secret Access Key is not disclosed.", 4));

    var notificationShell = new DefaultShell("SonarLint - Secret(s) detected");
    new DefaultLink(notificationShell, "Dismiss").click();
  }

  @Test
  public void shouldAutomaticallyUpdateRuleSetWhenChangedOnServer() throws Exception {
    Assume.assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals(9, 4));

    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-simple", JAVA_SIMPLE_PROJECT_KEY);

    createConnectionAndBindProject(JAVA_SIMPLE_PROJECT_KEY);

    var file = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(file);

    var defaultEditor = new TextEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Replace this use of System.out or System.err by a logger.", 9));

    var qualityProfile = getQualityProfile(JAVA_SIMPLE_PROJECT_KEY, "SonarLint IT Java");
    deactivateRule(qualityProfile, "S106");
    Thread.sleep(5000);

    doAndWaitForSonarLintAnalysisJob(() -> {
      defaultEditor.insertText(0, " ");
      defaultEditor.save();
    });

    assertThat(defaultEditor.getMarkers()).isEmpty();
  }

  private static void createConnectionAndBindProject(String projectKey) {
    var wizard = new ServerConnectionWizard();
    wizard.open();
    new ServerConnectionWizard.ServerTypePage(wizard).selectSonarQube();
    wizard.next();

    var serverUrlPage = new ServerConnectionWizard.ServerUrlPage(wizard);
    serverUrlPage.setUrl(orchestrator.getServer().getUrl());
    wizard.next();

    var authenticationModePage = new ServerConnectionWizard.AuthenticationModePage(wizard);
    authenticationModePage.selectUsernamePasswordMode();
    wizard.next();

    var authenticationPage = new ServerConnectionWizard.AuthenticationPage(wizard);
    authenticationPage.setUsername(Server.ADMIN_LOGIN);
    authenticationPage.setPassword(Server.ADMIN_PASSWORD);
    wizard.next();

    // as login can take time, wait for the next page to appear
    new WaitUntil(new WidgetIsFound(Label.class, new WithTextMatcher("SonarQube Connection Identifier")));
    var connectionNamePage = new ServerConnectionWizard.ConnectionNamePage(wizard);

    connectionNamePage.setConnectionName("test");
    wizard.next();

    if (orchestrator.getServer().version().isGreaterThanOrEquals(8, 7)) {
      // SONAR-14306 Starting from 8.7, dev notifications are available even in community edition
      wizard.next();
    }
    wizard.finish();

    var projectBindingWizard = new ProjectBindingWizard();
    var projectsToBindPage = new ProjectBindingWizard.BoundProjectsPage(projectBindingWizard);
    projectsToBindPage.clickAdd();

    var projectSelectionDialog = new ProjectSelectionDialog();
    projectSelectionDialog.setProjectName(projectKey);
    projectSelectionDialog.ok();

    projectBindingWizard.next();
    var serverProjectSelectionPage = new ProjectBindingWizard.ServerProjectSelectionPage(projectBindingWizard);
    serverProjectSelectionPage.waitForProjectsToBeFetched();
    serverProjectSelectionPage.setProjectKey(projectKey);
    projectBindingWizard.finish();

    var bindingsView = new BindingsView();
    bindingsView.open();
    bindingsView.updateAllProjectBindings();
    new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
  }

  private QualityProfile getQualityProfile(String projectKey, String qualityProfileName) {
    var searchReq = new SearchWsRequest();
    searchReq.setQualityProfile(qualityProfileName);
    searchReq.setProjectKey(projectKey);
    searchReq.setDefaults(false);
    var search = adminWsClient.qualityProfiles().search(searchReq);
    for (QualityProfile profile : search.getProfilesList()) {
      if (profile.getName().equals(qualityProfileName)) {
        return profile;
      }
    }
    fail("Unable to get quality profile " + qualityProfileName);
    throw new IllegalStateException("Should have failed");
  }

  private void deactivateRule(QualityProfile qualityProfile, String ruleKey) {
    var request = new PostRequest("/api/qualityprofiles/deactivate_rule")
      .setParam("key", qualityProfile.getKey())
      .setParam("rule", javaRuleKey(ruleKey));
    try (var response = adminWsClient.wsConnector().call(request)) {
      assertTrue("Unable to deactivate rule " + ruleKey, response.isSuccessful());
    }
  }

  private static String javaRuleKey(String key) {
    // Starting from SonarJava 6.0 (embedded in SQ 8.2), rule repository has been changed
    return orchestrator.getServer().version().isGreaterThanOrEquals(8, 2) ? ("java:" + key) : ("squid:" + key);
  }

}
