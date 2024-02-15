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
package org.sonarlint.eclipse.its;

import com.google.gson.Gson;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.http.HttpMethod;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.ui.markers.matcher.MarkerDescriptionMatcher;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.menu.ContextMenuItem;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.impl.editor.DefaultEditor;
import org.eclipse.reddeer.workbench.impl.editor.Marker;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.eclipse.swt.widgets.Label;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarlint.eclipse.its.reddeer.conditions.DialogMessageIsExpected;
import org.sonarlint.eclipse.its.reddeer.conditions.RuleDescriptionViewIsLoaded;
import org.sonarlint.eclipse.its.reddeer.dialogs.MarkIssueAsDialog;
import org.sonarlint.eclipse.its.reddeer.preferences.SonarLintPreferences.IssuePeriod;
import org.sonarlint.eclipse.its.reddeer.views.BindingsView;
import org.sonarlint.eclipse.its.reddeer.views.BindingsView.Binding;
import org.sonarlint.eclipse.its.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.reddeer.views.RuleDescriptionView;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintIssueMarker;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintTaintVulnerabilitiesView;
import org.sonarlint.eclipse.its.reddeer.wizards.ProjectBindingWizard;
import org.sonarlint.eclipse.its.reddeer.wizards.ServerConnectionWizard;
import org.sonarqube.ws.QualityProfiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.permission.AddGroupWsRequest;
import org.sonarqube.ws.client.permission.RemoveGroupWsRequest;
import org.sonarqube.ws.client.project.CreateRequest;
import org.sonarqube.ws.client.qualityprofile.SearchWsRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertTrue;

public class SonarQubeConnectedModeTest extends AbstractSonarQubeConnectedModeTest {
  private static final String S106 = "S106";
  private static final String JAVA_SIMPLE_PROJECT_KEY = "java-simple";
  private static final String SECRET_JAVA_PROJECT_NAME = "secret-java";
  private static final String MAVEN2_PROJECT_KEY = "maven2";
  private static final String MAVEN_TAINT_PROJECT_KEY = "maven-taint";
  private static final String INSUFFICIENT_PERMISSION_USER = "iHaveNoRights";
  private static final MarkerDescriptionMatcher ISSUE_MATCHER = new MarkerDescriptionMatcher(
    CoreMatchers.containsString("System.out"));

  /** Orchestrator to not be re-used in order for ITs to not fail -> always use latest release locally (not LTS) */
  @ClassRule
  public static final OrchestratorRule orchestrator = OrchestratorRule.builderEnv()
    .defaultForceAuthentication()
    .useDefaultAdminCredentialsForBuilds(true)
    .keepBundledPlugins()
    .setEdition(Edition.DEVELOPER)
    .activateLicense()
    .setSonarVersion(System.getProperty("sonar.runtimeVersion", "LATEST_RELEASE"))
    // Ensure SSE are processed correctly just after SQ startup
    .setServerProperty("sonar.pushevents.polling.initial.delay", "2")
    .setServerProperty("sonar.pushevents.polling.period", "1")
    .setServerProperty("sonar.pushevents.polling.last.timestamp", "1")
    .build();

  @BeforeClass
  public static void prepare() {
    prepare(orchestrator);
    adminWsClient.projects().create(CreateRequest.builder()
      .setName(JAVA_SIMPLE_PROJECT_KEY)
      .setKey(JAVA_SIMPLE_PROJECT_KEY).build());
    orchestrator.getServer().associateProjectToQualityProfile(JAVA_SIMPLE_PROJECT_KEY, "java", "SonarLint IT Java");
  }

  @After
  public void restoreDeactivatedRule() {
    var qualityProfile = getQualityProfile(JAVA_SIMPLE_PROJECT_KEY, "SonarLint IT Java");
    activateRule(qualityProfile, S106);
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

    // until we change the ITs with the removal of the username / password authentication we check here once
    assertThat(authenticationPage.getDeprecationMessage()).isEqualTo(authenticationPage.DEPRECATION_MESSAGE);

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
    var code = statusConnection.getResponseCode();
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
    adminWsClient.projects()
      .create(CreateRequest.builder()
        .setName(SECRET_JAVA_PROJECT_NAME)
        .setKey(SECRET_JAVA_PROJECT_NAME).build());

    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("secrets/secret-java", SECRET_JAVA_PROJECT_NAME);

    createConnectionAndBindProject(orchestrator, SECRET_JAVA_PROJECT_NAME);

    // Remove binding suggestion notification
    var bindingSuggestionNotificationShell = new DefaultShell("SonarLint Binding Suggestion");
    new DefaultLink(bindingSuggestionNotificationShell, "Don't ask again").click();

    waitForAnalysisReady(SECRET_JAVA_PROJECT_NAME);

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "sec", "Secret.java"));

    var defaultEditor = new DefaultEditor();
    assertThat(defaultEditor.getMarkers())
      .extracting(Marker::getText, Marker::getLineNumber)
      .containsOnly(
        tuple("Make sure this AWS Secret Access Key gets revoked, changed, and removed from the code.", 4));

    var notificationShell = new DefaultShell("SonarLint - Secret(s) detected");
    new DefaultLink(notificationShell, "Dismiss").click();
  }

  @Test
  public void shouldAutomaticallyUpdateRuleSetWhenChangedOnServer() throws Exception {
    Assume.assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals(9, 4));

    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/java-simple", JAVA_SIMPLE_PROJECT_KEY);

    createConnectionAndBindProject(orchestrator, JAVA_SIMPLE_PROJECT_KEY);

    // Remove binding suggestion notification
    var bindingSuggestionNotificationShell = new DefaultShell("SonarLint Binding Suggestion");
    new DefaultLink(bindingSuggestionNotificationShell, "Don't ask again").click();

    waitForAnalysisReady(JAVA_SIMPLE_PROJECT_KEY);

    var file = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(file);

    var defaultEditor = new TextEditor();
    assertThat(defaultEditor.getMarkers())
      .satisfiesAnyOf(
        list -> assertThat(list)
          .extracting(Marker::getText, Marker::getLineNumber)
          .containsOnly(tuple("Replace this use of System.out by a logger.", 9)),
        list -> assertThat(list)
          .extracting(Marker::getText, Marker::getLineNumber)
          .containsOnly(tuple("Replace this use of System.out or System.err by a logger.", 9)));

    var qualityProfile = getQualityProfile(JAVA_SIMPLE_PROJECT_KEY, "SonarLint IT Java");
    deactivateRule(qualityProfile, S106);
    Thread.sleep(5000);

    doAndWaitForSonarLintAnalysisJob(() -> {
      defaultEditor.insertText(0, " ");
      defaultEditor.save();
    });

    assertThat(defaultEditor.getMarkers()).isEmpty();
  }

  /**
   *  As we test against different SQ versions, we have to check that the grouping and the rule descriptions
   *  work correctly on old / new CCT connections
   */
  @Test
  public void check_grouping() {
    new JavaPerspective().open();
    var ruleDescriptionView = new RuleDescriptionView();
    ruleDescriptionView.open();
    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    var rootProject = importExistingProjectIntoWorkspace("java/java-simple", JAVA_SIMPLE_PROJECT_KEY);

    createConnectionAndBindProject(orchestrator, JAVA_SIMPLE_PROJECT_KEY);

    // Remove binding suggestion notification
    var bindingSuggestionNotificationShell = new DefaultShell("SonarLint Binding Suggestion");
    new DefaultLink(bindingSuggestionNotificationShell, "Don't ask again").click();

    waitForAnalysisReady(JAVA_SIMPLE_PROJECT_KEY);

    var file = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(file);

    await().untilAsserted(() -> assertThat(
      onTheFlyView.getIssues(ISSUE_MATCHER)).satisfiesAnyOf(
        list -> assertThat(list)
          .extracting(i -> i.getDescription())
          .containsOnly("Replace this use of System.out by a logger."),
        list -> assertThat(list)
          .extracting(i -> i.getDescription())
          .containsOnly("Replace this use of System.out or System.err by a logger.")));

    var emptyMatcher = new MarkerDescriptionMatcher(CoreMatchers.containsString(""));

    onTheFlyView.groupByImpact();
    await().untilAsserted(() -> assertThat(onTheFlyView.getIssues(emptyMatcher)).hasSize(1));
    onTheFlyView.groupBySeverityLegacy();
    await().untilAsserted(() -> assertThat(onTheFlyView.getIssues(emptyMatcher)).hasSize(1));
    onTheFlyView.resetGrouping();
    await().untilAsserted(() -> assertThat(onTheFlyView.getIssues(emptyMatcher)).hasSize(1));

    ruleDescriptionView.open();
    onTheFlyView.selectItem(0);
    new WaitUntil(new RuleDescriptionViewIsLoaded(ruleDescriptionView));
  }

  // integration test for the "Mark issue as ..." dialog without and then with permission
  @Test
  @Ignore("10/2023: Currently failing constantly on the CI, to be investigated later")
  public void test_MarkIssueAs_Dialog() {
    // INFO: It is flaky when running on top of the oldest Eclipse version but works fine in the other test cases,
    // therefore it should be skipped in that particular situation!
    Assume.assumeTrue(!"oldest".equals(System.getProperty("target.platform")));

    // 1) Create project on SonarQube
    adminWsClient.projects()
      .create(CreateRequest.builder()
        .setName(MAVEN2_PROJECT_KEY)
        .setKey(MAVEN2_PROJECT_KEY).build());
    orchestrator.getServer().associateProjectToQualityProfile(MAVEN2_PROJECT_KEY, "java", "SonarLint IT Java");

    // 2) Import project into workspace
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/maven2", MAVEN2_PROJECT_KEY);

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    // 3) Run SonarQube analysis
    runMavenBuild(orchestrator, MAVEN2_PROJECT_KEY, "projects", "java/maven2/pom.xml", Map.of());

    // 4) Add new user to SonarQube
    adminWsClient.users().create(org.sonarqube.ws.client.user.CreateRequest.builder()
      .setLogin(INSUFFICIENT_PERMISSION_USER)
      .setPassword(INSUFFICIENT_PERMISSION_USER)
      .setName(INSUFFICIENT_PERMISSION_USER)
      .build());

    // 5) Remove user rights from project
    adminWsClient.permissions().removeGroup(new RemoveGroupWsRequest()
      .setProjectKey(MAVEN2_PROJECT_KEY)
      .setGroupName("sonar-users")
      .setPermission("issueadmin"));

    // 6) Remove connections and reconnect with user
    createConnectionAndBindProject(orchestrator, MAVEN2_PROJECT_KEY, INSUFFICIENT_PERMISSION_USER,
      INSUFFICIENT_PERMISSION_USER);

    // 7) Remove binding suggestion notification
    var notificationShell = new DefaultShell("SonarLint Binding Suggestion");
    new DefaultLink(notificationShell, "Don't ask again").click();

    waitForAnalysisReady(MAVEN2_PROJECT_KEY);

    // 8) Open first file and try to open the "Mark issue as ..." dialog
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src/main/java", "hello", "Hello.java"));

    await().until(() -> onTheFlyView.getIssues(ISSUE_MATCHER), findings -> !findings.isEmpty());
    onTheFlyView.getIssues(ISSUE_MATCHER).get(0).select();
    new ContextMenuItem(onTheFlyView.getTree(), "Mark Issue as...").select();

    var s = new DefaultShell("Mark Issue as Resolved on SonarQube");
    new PushButton(s, "OK").click();

    // 9) Assert marker is still available
    await().untilAsserted(() -> assertThat(onTheFlyView.getIssues(ISSUE_MATCHER)).satisfiesAnyOf(
      list -> assertThat(list)
        .extracting(i -> i.getDescription())
        .containsOnly("Replace this use of System.out by a logger."),
      list -> assertThat(list)
        .extracting(i -> i.getDescription())
        .containsOnly("Replace this use of System.out or System.err by a logger.")));

    // 10) Add user rights from project
    adminWsClient.permissions().addGroup(new AddGroupWsRequest()
      .setProjectKey(MAVEN2_PROJECT_KEY)
      .setGroupName("sonar-users")
      .setPermission("issueadmin"));

    // 11) Open second file, open the "Mark issue as ..." dialog, resolve and leave comment
    var hello2 = rootProject.getResource("src/main/java", "hello", "Hello2.java");
    openFileAndWaitForAnalysisCompletion(hello2);

    await().until(() -> onTheFlyView.getIssues(ISSUE_MATCHER), findings -> !findings.isEmpty());
    onTheFlyView.getIssues(ISSUE_MATCHER).get(0).select();
    new ContextMenuItem(onTheFlyView.getTree(), "Mark Issue as...").select();

    var dialog = new MarkIssueAsDialog();
    dialog.selectFalsePositive();
    dialog.selectWontFix();
    dialog.setComment("Wasn't sure, started with 'False Positiv' but settled on 'Won't Fix'!");
    doAndWaitForSonarLintAnalysisJob(dialog::ok);

    // 12) Remove marked as resolved notification
    var notificationShell2 = new DefaultShell("SonarLint - Issue marked as resolved");
    new DefaultLink(notificationShell2, "Dismiss").click();

    // 13) Assert marker is gone
    await().until(() -> onTheFlyView.getIssues(ISSUE_MATCHER), List<SonarLintIssueMarker>::isEmpty);
  }

  // integration test for focusing on new code in connected mode
  @Test
  @Ignore("10/2023: Currently failing constantly on the CI, to be investigated later")
  public void test_new_code_period_preference() {
    Assume.assumeTrue("latest".equals(System.getProperty("target.platform")));

    // 1) create project on server / run first analysis
    adminWsClient.projects()
      .create(CreateRequest.builder()
        .setName(MAVEN_TAINT_PROJECT_KEY)
        .setKey(MAVEN_TAINT_PROJECT_KEY).build());
    orchestrator.getServer().associateProjectToQualityProfile(MAVEN_TAINT_PROJECT_KEY, "java", "SonarLint IT New Code");

    runMavenBuild(orchestrator, MAVEN_TAINT_PROJECT_KEY, "projects", "java/maven-taint/pom.xml", Map.of());

    // 2) import project / check that new code period preference does nothing in standalone mode
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/maven-taint", MAVEN_TAINT_PROJECT_KEY);

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();
    var taintVulnerabilitiesView = new SonarLintTaintVulnerabilitiesView();
    taintVulnerabilitiesView.open();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src/main/java", "taint", "taint_issue.java"));

    await().untilAsserted(() -> assertThat(onTheFlyView.getItems()).hasSize(2));

    setNewCodePreference(IssuePeriod.NEW_CODE);

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src/main/java", "taint", "taint_issue.java"));

    await().untilAsserted(() -> assertThat(onTheFlyView.getItems()).hasSize(2));

    setNewCodePreference(IssuePeriod.ALL_TIME);

    // 3) bind to project on SonarQube / check issues and taint vulnerabilities exist
    createConnectionAndBindProject(orchestrator, MAVEN_TAINT_PROJECT_KEY, Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD);

    var notificationShell = new DefaultShell("SonarLint Binding Suggestion");
    new DefaultLink(notificationShell, "Don't ask again").click();

    waitForAnalysisReady(MAVEN_TAINT_PROJECT_KEY);

    await().untilAsserted(() -> assertThat(onTheFlyView.getItems()).hasSize(2));
    await().untilAsserted(() -> assertThat(taintVulnerabilitiesView.getItems()).hasSize(1));

    var newTaintShell = new DefaultShell("SonarLint - Taint vulnerability found");
    new DefaultLink(newTaintShell, "Show in view").click();

    // 4) unbind project / set new code period to "previous version" / run second analysis with a new version
    var bindingsView = new BindingsView();
    bindingsView.open();
    bindingsView.removeAllBindings();

    setNewCodePeriodToPreviousVersion(MAVEN_TAINT_PROJECT_KEY);

    runMavenBuild(orchestrator, MAVEN_TAINT_PROJECT_KEY, "projects", "java/maven-taint/pom.xml",
      Map.of("sonar.projectVersion", "1.1-SNAPSHOT"));

    // Because of a bug in SQ that returns the techncial issue creation date, we have to run another analysis
    // to be sure issues will be before the new code period
    runMavenBuild(orchestrator, MAVEN_TAINT_PROJECT_KEY, "projects", "java/maven-taint/pom.xml",
      Map.of("sonar.projectVersion", "1.2-SNAPSHOT"));

    // 5) bind to project on SonarQube / check that new code period preference is working
    createConnectionAndBindProject(orchestrator, MAVEN_TAINT_PROJECT_KEY, Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD);

    waitForAnalysisReady(MAVEN_TAINT_PROJECT_KEY);

    await().untilAsserted(() -> assertThat(onTheFlyView.getItems()).hasSize(2));
    await().untilAsserted(() -> assertThat(taintVulnerabilitiesView.getItems()).hasSize(1));

    setNewCodePreference(IssuePeriod.NEW_CODE);

    await().untilAsserted(() -> assertThat(onTheFlyView.getItems()).isEmpty());
    await().untilAsserted(() -> assertThat(taintVulnerabilitiesView.getItems()).isEmpty());
  }

  private static QualityProfile getQualityProfile(String projectKey, String qualityProfileName) {
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

  private static void deactivateRule(QualityProfile qualityProfile, String ruleKey) {
    var request = new PostRequest("/api/qualityprofiles/deactivate_rule")
      .setParam("key", qualityProfile.getKey())
      .setParam("rule", javaRuleKey(ruleKey));
    try (var response = adminWsClient.wsConnector().call(request)) {
      assertTrue("Unable to deactivate rule " + ruleKey, response.isSuccessful());
    }
  }

  private static void activateRule(QualityProfile qualityProfile, String ruleKey) {
    var request = new PostRequest("/api/qualityprofiles/activate_rule")
      .setParam("key", qualityProfile.getKey())
      .setParam("rule", javaRuleKey(ruleKey));
    try (var response = adminWsClient.wsConnector().call(request)) {
      assertTrue("Unable to activate rule " + ruleKey, response.isSuccessful());
    }
  }

  private static String javaRuleKey(String key) {
    // Starting from SonarJava 6.0 (embedded in SQ 8.2), rule repository has been changed
    return orchestrator.getServer().version().isGreaterThanOrEquals(8, 2) ? ("java:" + key) : ("squid:" + key);
  }

  private static void setNewCodePeriodToPreviousVersion(String projectKey) {
    orchestrator.getServer()
      .newHttpCall("api/new_code_periods/set")
      .setMethod(HttpMethod.POST)
      .setAdminCredentials()
      .setParam("project", projectKey)
      .setParam("type", "PREVIOUS_VERSION")
      .execute();
  }
}
