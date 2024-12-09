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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.groups.Tuple;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.CFamilyLoaded;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.ConfirmConnectionCreationDialogOpened;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.DialogMessageIsExpected;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.FixSuggestionAvailableDialogOpened;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.FixSuggestionUnavailableDialogOpened;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.ProjectBindingWizardIsOpened;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.ProjectSelectionDialogOpened;
import org.sonarlint.eclipse.its.shared.reddeer.dialogs.ConfirmConnectionCreationDialog;
import org.sonarlint.eclipse.its.shared.reddeer.dialogs.FixSuggestionAvailableDialog;
import org.sonarlint.eclipse.its.shared.reddeer.dialogs.FixSuggestionUnavailableDialog;
import org.sonarlint.eclipse.its.shared.reddeer.dialogs.ProjectSelectionDialog;
import org.sonarlint.eclipse.its.shared.reddeer.perspectives.CppPerspective;
import org.sonarlint.eclipse.its.shared.reddeer.views.BindingsView;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.shared.reddeer.views.SonarLintConsole;
import org.sonarlint.eclipse.its.shared.reddeer.wizards.ProjectBindingWizard;
import org.sonarlint.eclipse.its.shared.reddeer.wizards.ServerConnectionWizard;
import org.sonarqube.ws.ProjectBranches.Branch;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.projectbranches.ListRequest;
import org.sonarqube.ws.client.usertokens.GenerateRequest;
import org.sonarqube.ws.client.usertokens.RevokeRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;

public class SonarCloudConnectedModeTest extends AbstractSonarLintTest {
  private static final String TIMESTAMP = Long.toString(Instant.now().toEpochMilli());
  private static final String SONARCLOUD_STAGING_URL = "https://sc-staging.io";
  private static final String SONARCLOUD_ORGANIZATION_KEY = "sonarlint-it";
  private static final String SONARCLOUD_USER = "sonarlint-it";
  private static final String SONARCLOUD_PASSWORD = System.getenv("SONARCLOUD_IT_PASSWORD");
  private static final String TOKEN_NAME = "SLE-IT-" + TIMESTAMP;
  private static final String SAMPLE_JAVA_ISSUES_PROJECT_KEY = "sonarlint-its-sample-java-issues";
  private static final String MAKEFILE_PROJECT_KEY = "MakefileProject";
  private static final String MAKEFILE_PROJECT_SONAR_KEY = MAKEFILE_PROJECT_KEY + "-" + TIMESTAMP;

  private static HttpConnector connector;
  private static WsClient adminWsClient;
  private static String token;
  private static String firstSonarCloudProjectKey;
  private static String firstSonarCloudIssueKey;
  private static String firstSonarCloudBranch;

  @BeforeClass
  public static void prepare() {
    connector = HttpConnector.newBuilder()
      .url(SONARCLOUD_STAGING_URL)
      .credentials(SONARCLOUD_USER, SONARCLOUD_PASSWORD)
      .build();
    adminWsClient = WsClientFactories.getDefault().newClient(connector);

    createProject(MAKEFILE_PROJECT_KEY, MAKEFILE_PROJECT_SONAR_KEY);

    token = adminWsClient.userTokens()
      .generate(new GenerateRequest().setName(TOKEN_NAME))
      .getToken();

    try {
      var sonarCloudProjectKeys = getProjectKeys();
      var keyAndProject = getFirstProjectAndIssueKey(sonarCloudProjectKeys);
      firstSonarCloudIssueKey = (String) keyAndProject.toList().get(0);
      firstSonarCloudProjectKey = (String) keyAndProject.toList().get(1);
    } catch (InterruptedException | IOException err) {
      fail("Cannot query the project keys and / or first issue and project key from SonarCloud!", err);
    }

    firstSonarCloudBranch = getFirstBranch(firstSonarCloudProjectKey).getName();
  }

  @AfterClass
  public static void cleanupOrchestrator() {
    adminWsClient.userTokens()
      .revoke(new RevokeRequest().setName(TOKEN_NAME));

    // Because we only use CDT in here, we switch back for other tests to not get confused!
    new JavaPerspective().open();
  }

  @Before
  public void cleanBindings() {
    var bindingsView = new BindingsView();
    bindingsView.open();
    bindingsView.removeAllBindings();
  }

  @Test
  public void test_makefile_based_project() {
    // i) Open C/C++ perspective and import project
    new CppPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("cdt/MakefileProject", MAKEFILE_PROJECT_KEY);

    // ii) Open file and await notification and no SonarLint issue to be shown
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("hello.c"));
    shellByName("SonarQube for Eclipse - Language could not be analyzed").ifPresent(DefaultShell::close);

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();
    waitForNoSonarLintMarkers(onTheFlyView);
    new DefaultEditor().close();

    // iii) Create connection / bind project and SonarLint issue to be shown
    createConnectionAndBindProject(MAKEFILE_PROJECT_KEY, MAKEFILE_PROJECT_SONAR_KEY);
    shellByName("SonarQube - Binding Suggestion").ifPresent(DefaultShell::close);
    new SonarLintConsole().clear();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("hello.c"));
    new WaitUntil(new CFamilyLoaded(new SonarLintConsole().getConsoleView()), TimePeriod.getCustom(120));
    onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();
    waitForSonarLintMarkers(onTheFlyView,
      tuple("Complete the task associated to this \"TODO\" comment.", "hello.c", "few seconds ago"));
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
    connectionNamePage.setConnectionName(SONARCLOUD_ORGANIZATION_KEY);

    // Sadly we have to invoke sleep here as in the background there is SL communicating with SC regarding the
    // availability of notifications "on the server". As this is not done in a job we could listen to, we wait the
    // 5 seconds. Once we change it in SonarLint to not ask for notifications (for all supported SQ versions and SC
    // they are supported by now), we can somehow circumvent this.
    Thread.sleep(5000);
    assertThat(wizard.isNextEnabled()).isTrue();
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

  @Test
  public void fixSuggestion_with_ConnectionSetup_noProject() throws InterruptedException, IOException {
    triggerOpenFixSuggestionWithOneChange(firstSonarCloudProjectKey,
      firstSonarCloudIssueKey,
      "NotExisting.txt",
      "fixSuggestion_with_ConnectionSetup_noProject",
      "before",
      "after",
      0, 1);

    new WaitUntil(new ConfirmConnectionCreationDialogOpened(true));
    new ConfirmConnectionCreationDialog(true).trust();

    shellByName("SonarQube Cloud - No matching open project found")
      .ifPresent(shell -> {
        new DefaultLink(shell, "Open Troubleshooting documentation").click();
      });
  }

  @Test
  public void fixSuggestion_with_ConnectionSetup_fileNotFound() throws InterruptedException, IOException {
    importExistingProjectIntoWorkspace("connected-sc/" + SAMPLE_JAVA_ISSUES_PROJECT_KEY, SAMPLE_JAVA_ISSUES_PROJECT_KEY);

    triggerOpenFixSuggestionWithOneChange(firstSonarCloudProjectKey,
      firstSonarCloudIssueKey,
      "NotExisting.txt",
      "fixSuggestion_with_ConnectionSetup_fileNotFound",
      "before",
      "after",
      0, 1);

    new WaitUntil(new ConfirmConnectionCreationDialogOpened(true));
    new ConfirmConnectionCreationDialog(true).trust();

    new WaitUntil(new ProjectSelectionDialogOpened());
    new ProjectSelectionDialog().ok();

    // The error message from SLCORE is not denoted by a specific title.
    var shellOpt = shellByName("SonarQube");
    assertThat(shellOpt).isNotEmpty();
    shellOpt.get().close();
  }

  @Test
  public void fixSuggestion_with_fix() throws InterruptedException, IOException {
    final var file = "FileExists.txt";
    final var explanation = "This is common knowledge!";
    final var before = "Eclipse IDE is the best!";
    final var after = "IntelliJ IDEA is not the best!";
    final var startLine = 0;
    final var endLine = 1;

    importExistingProjectIntoWorkspace("connected-sc/" + SAMPLE_JAVA_ISSUES_PROJECT_KEY, SAMPLE_JAVA_ISSUES_PROJECT_KEY);

    // 1) Cancel the suggestion (available)
    triggerOpenFixSuggestionWithOneChange(firstSonarCloudProjectKey, firstSonarCloudIssueKey, file, explanation, before, after, startLine, endLine);

    new WaitUntil(new ConfirmConnectionCreationDialogOpened(true));
    new ConfirmConnectionCreationDialog(true).trust();

    new WaitUntil(new ProjectSelectionDialogOpened());
    new ProjectSelectionDialog().ok();

    new WaitUntil(new FixSuggestionAvailableDialogOpened(0, 1));
    new FixSuggestionAvailableDialog(0, 1).cancel();

    // 2) Decline the suggestion
    triggerOpenFixSuggestionWithOneChange(firstSonarCloudProjectKey, firstSonarCloudIssueKey, file, explanation, before, after, startLine, endLine);

    new WaitUntil(new FixSuggestionAvailableDialogOpened(0, 1));
    new FixSuggestionAvailableDialog(0, 1).declineTheChange();

    // 3) Apply the suggestion
    triggerOpenFixSuggestionWithOneChange(firstSonarCloudProjectKey, firstSonarCloudIssueKey, file, explanation, before, after, startLine, endLine);

    new WaitUntil(new FixSuggestionAvailableDialogOpened(0, 1));
    new FixSuggestionAvailableDialog(0, 1).applyTheChange();

    // 4) Cancel the suggestion (unavailable)
    triggerOpenFixSuggestionWithOneChange(firstSonarCloudProjectKey, firstSonarCloudIssueKey, file, explanation, before, after, startLine, endLine);

    new WaitUntil(new FixSuggestionUnavailableDialogOpened(0, 1));
    new FixSuggestionUnavailableDialog(0, 1).cancel();

    // 5) Suggestion not found
    triggerOpenFixSuggestionWithOneChange(firstSonarCloudProjectKey, firstSonarCloudIssueKey, file, explanation, before, after, startLine, endLine);

    new WaitUntil(new FixSuggestionUnavailableDialogOpened(0, 1));
    new FixSuggestionUnavailableDialog(0, 1).proceed();
  }

  @Test
  public void fixSuggestion_with_multipleFixes() throws InterruptedException, IOException {
    final var file = "FileExists.txt";
    final var explanation = "We need to change this!";
    final var before = "Eclipse IDE is the best!";
    final var firstAfter = "IntelliJ IDEA is not the best!";
    final var secondAfter = "PyCharm CE is also quite okey!";
    final var firstStartLine = 0;
    final var firstEndLine = 1;
    final var secondStartLine = 1107;
    final var secondEndLine = 1108;

    importExistingProjectIntoWorkspace("connected-sc/" + SAMPLE_JAVA_ISSUES_PROJECT_KEY, SAMPLE_JAVA_ISSUES_PROJECT_KEY);

    triggerOpenFixSuggestionWithTwoChanges(
      firstSonarCloudProjectKey,
      firstSonarCloudIssueKey,
      file,
      explanation,
      before, firstAfter, firstStartLine, firstEndLine,
      firstAfter, secondAfter, secondStartLine, secondEndLine);

    new WaitUntil(new ConfirmConnectionCreationDialogOpened(true));
    new ConfirmConnectionCreationDialog(true).trust();

    new WaitUntil(new ProjectSelectionDialogOpened());
    new ProjectSelectionDialog().ok();

    // 1) Accept first suggestion
    new WaitUntil(new FixSuggestionAvailableDialogOpened(0, 2));
    new FixSuggestionAvailableDialog(0, 2).applyTheChange();

    // 2) Proceed with second suggestion (way out of range of the file)
    new WaitUntil(new FixSuggestionUnavailableDialogOpened(1, 2));
    new FixSuggestionUnavailableDialog(1, 2).proceed();
  }

  private static List<String> getProjectKeys() throws InterruptedException, IOException {
    assertThat(hotspotServerPort).isNotEqualTo(-1);

    var request = HttpRequest.newBuilder()
      .uri(URI.create(SONARCLOUD_STAGING_URL + "/api/projects/search?organization=" + SONARCLOUD_ORGANIZATION_KEY
        + "&analyzedBefore=" + LocalDate.now()))
      .header("Authorization", "Bearer " + token)
      .GET()
      .build();

    var response = HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);

    var jsonObject = (JsonObject) JsonParser.parseString(response.body());

    var projectKeys = new ArrayList<String>();
    var projectsList = jsonObject.get("components").getAsJsonArray();
    for (var project : projectsList) {
      var key = project.getAsJsonObject().get("key").getAsString();
      if (key.contains(SAMPLE_JAVA_ISSUES_PROJECT_KEY)) {
        projectKeys.add(key);
      }
    }
    return projectKeys;
  }

  private static Tuple getFirstProjectAndIssueKey(List<String> projectKeys) throws IOException, InterruptedException {
    assertThat(hotspotServerPort).isNotEqualTo(-1);

    var request = HttpRequest.newBuilder()
      .uri(URI.create(SONARCLOUD_STAGING_URL + "/api/issues/search?organization=" + SONARCLOUD_ORGANIZATION_KEY
        + "&componentKeys=" + String.join(",", projectKeys)))
      .header("Authorization", "Bearer " + token)
      .GET()
      .build();

    var response = HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);

    var jsonObject = (JsonObject) JsonParser.parseString(response.body());
    var firstIssueKey = jsonObject.get("issues").getAsJsonArray().get(0).getAsJsonObject().get("key").getAsString();
    var firstProjectKey = jsonObject.get("issues").getAsJsonArray().get(0).getAsJsonObject().get("project").getAsString();
    return new Tuple(firstIssueKey, firstProjectKey);
  }

  private static Branch getFirstBranch(String projectKey) {
    var response = adminWsClient.projectBranches()
      .list(new ListRequest().setProject(projectKey));
    assertThat(response.getBranchesCount()).isPositive();

    return response.getBranches(0);
  }

  private void triggerOpenFixSuggestionWithOneChange(String projectKey, String issueKey, String relativePath,
    String explanation, String before, String after, int startLine, int endLine)
    throws InterruptedException, IOException {
    assertThat(hotspotServerPort).isNotEqualTo(-1);

    var body = "{"
      + "\"suggestionId\":\"9689b623-708e-4128-ae90-8432206c61fe\","
      + "\"explanation\":\"" + explanation + "\","
      + "\"fileEdit\":{"
      + "\"path\":\"" + relativePath + "\","
      + "\"changes\":["
      + "{"
      + "\"before\":\"" + before + "\","
      + "\"after\":\"" + after + "\","
      + "\"beforeLineRange\":{"
      + "\"startLine\":" + startLine + ","
      + "\"endLine\":" + endLine
      + "}"
      + "}"
      + "]"
      + "}"
      + "}";

    triggerOpenFixSuggestion(projectKey, issueKey, body);
  }

  private void triggerOpenFixSuggestionWithTwoChanges(String projectKey, String issueKey, String relativePath,
    String explanation, String firstBefore, String firstAfter, int firstStartLine, int firstEndLine,
    String secondBefore, String secondAfter, int secondStartLine, int secondEndLine)
    throws InterruptedException, IOException {
    assertThat(hotspotServerPort).isNotEqualTo(-1);

    var body = "{"
      + "\"suggestionId\":\"9689b623-708e-4128-ae90-8432206c61fe\","
      + "\"explanation\":\"" + explanation + "\","
      + "\"fileEdit\":{"
      + "\"path\":\"" + relativePath + "\","
      + "\"changes\":["
      + "{"
      + "\"before\":\"" + firstBefore + "\","
      + "\"after\":\"" + firstAfter + "\","
      + "\"beforeLineRange\":{"
      + "\"startLine\":" + firstStartLine + ","
      + "\"endLine\":" + firstEndLine
      + "}"
      + "},"
      + "{"
      + "\"before\":\"" + secondBefore + "\","
      + "\"after\":\"" + secondAfter + "\","
      + "\"beforeLineRange\":{"
      + "\"startLine\":" + secondStartLine + ","
      + "\"endLine\":" + secondEndLine
      + "}"
      + "}"
      + "]"
      + "}"
      + "}";

    triggerOpenFixSuggestion(projectKey, issueKey, body);
  }

  private void triggerOpenFixSuggestion(String projectKey, String issueKey, String body)
    throws InterruptedException, IOException {
    assertThat(hotspotServerPort).isNotEqualTo(-1);

    var request = HttpRequest.newBuilder()
      .uri(URI.create("http://localhost:" + hotspotServerPort
        + "/sonarlint/api/fix/show"
        + "?project=" + projectKey
        + "&issue=" + issueKey
        + "&organizationKey=" + SONARCLOUD_ORGANIZATION_KEY
        + "&tokenName=" + TOKEN_NAME
        + "&tokenValue=" + token
        + "&branch=" + firstSonarCloudBranch))
      .header("Content-Type", "application/json")
      .header("Origin", SONARCLOUD_STAGING_URL)
      .POST(BodyPublishers.ofString(body))
      .build();

    var response = HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
  }

  /**
   *  Create the connection and bind a project where the project key used on SonarCloud staging differs as it is
   *  generated for every build.
   *
   *  @param projectKey equals project name
   *  @param sonarProjectKey generated project key
   */
  protected static void createConnectionAndBindProject(String projectKey, String sonarProjectKey) {
    var wizard = new ServerConnectionWizard();
    wizard.open();
    new ServerConnectionWizard.ServerTypePage(wizard).selectSonarCloud();
    wizard.next();

    assertThat(wizard.isNextEnabled()).isFalse();
    var authenticationPage = new ServerConnectionWizard.AuthenticationPage(wizard);
    authenticationPage.setToken(token);
    assertThat(wizard.isNextEnabled()).isTrue();
    wizard.next();

    var organizationsPage = new ServerConnectionWizard.OrganizationsPage(wizard);
    organizationsPage.waitForOrganizationsToBeFetched();

    assertThat(organizationsPage.getOrganization()).isEqualTo(SONARCLOUD_ORGANIZATION_KEY);

    organizationsPage.setOrganization(SONARCLOUD_ORGANIZATION_KEY);
    assertThat(wizard.isNextEnabled()).isTrue();
    wizard.next();

    var connectionNamePage = new ServerConnectionWizard.ConnectionNamePage(wizard);
    assertThat(connectionNamePage.getConnectionName()).isEqualTo(SONARCLOUD_ORGANIZATION_KEY);
    connectionNamePage.setConnectionName(SONARCLOUD_ORGANIZATION_KEY);

    // Sadly we have to invoke sleep here as in the background there is SL communicating with SC regarding the
    // availability of notifications "on the server". As this is not done in a job we could listen to, we wait the
    // 5 seconds. Once we change it in SonarLint to not ask for notifications (for all supported SQ versions and SC
    // they are supported by now), we can somehow circumvent this.
    try {
      Thread.sleep(5000);
    } catch (InterruptedException ignored) {
    }
    assertThat(wizard.isNextEnabled()).isTrue();
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
    new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
    new WaitUntil(new ProjectBindingWizardIsOpened());

    var projectBindingWizard = new ProjectBindingWizard();
    var projectsToBindPage = new ProjectBindingWizard.BoundProjectsPage(projectBindingWizard);

    // Because RedDeer can be faster than the actual UI, we have to wait for the page to populate itself!
    try {
      Thread.sleep(500);
    } catch (Exception ignored) {
    }
    projectsToBindPage.clickAdd();

    var projectSelectionDialog = new ProjectSelectionDialog();
    projectSelectionDialog.filterProjectName(projectKey);
    projectSelectionDialog.ok();

    projectBindingWizard.next();
    var serverProjectSelectionPage = new ProjectBindingWizard.ServerProjectSelectionPage(projectBindingWizard);
    serverProjectSelectionPage.waitForProjectsToBeFetched();
    serverProjectSelectionPage.setProjectKey(sonarProjectKey);
    projectBindingWizard.finish();
  }

  /** Creating a project on SonarCloud with all necessary information */
  private static void createProject(String projectName, String projectKey) {
    assertThat(hotspotServerPort).isNotEqualTo(-1);

    var response = connector.call(new PostRequest("/api/projects/create")
      .setParam("name", projectName)
      .setParam("project", projectKey)
      .setParam("organization", SONARCLOUD_ORGANIZATION_KEY));
    assertThat(response.code()).isEqualTo(200);
  }
}
