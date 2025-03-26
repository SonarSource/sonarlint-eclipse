/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarlint.eclipse.its.connected.sq;

import com.google.gson.Gson;
import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.http.HttpMethod;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.ui.markers.matcher.MarkerDescriptionMatcher;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.menu.ContextMenu;
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
import org.sonarlint.eclipse.its.shared.reddeer.conditions.DialogMessageIsExpected;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.ProjectBindingWizardIsOpened;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.RuleDescriptionViewIsLoaded;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.ShareConnectedModeConfigurationDialogOpened;
import org.sonarlint.eclipse.its.shared.reddeer.dialogs.MarkIssueAsDialog;
import org.sonarlint.eclipse.its.shared.reddeer.dialogs.ShareConnectedModeConfigurationDialog;
import org.sonarlint.eclipse.its.shared.reddeer.views.BindingsView;
import org.sonarlint.eclipse.its.shared.reddeer.views.BindingsView.Binding;
import org.sonarlint.eclipse.its.shared.reddeer.views.OnTheFlyView;
import org.sonarlint.eclipse.its.shared.reddeer.views.RuleDescriptionView;
import org.sonarlint.eclipse.its.shared.reddeer.views.SonarLintConsole;
import org.sonarlint.eclipse.its.shared.reddeer.views.SonarLintIssueMarker;
import org.sonarlint.eclipse.its.shared.reddeer.views.SonarLintTaintVulnerabilitiesView;
import org.sonarlint.eclipse.its.shared.reddeer.wizards.ProjectBindingWizard;
import org.sonarlint.eclipse.its.shared.reddeer.wizards.ServerConnectionWizard;
import org.sonarqube.ws.Qualityprofiles.SearchWsResponse.QualityProfile;
import org.sonarqube.ws.client.PostRequest;
import org.sonarqube.ws.client.permissions.AddGroupRequest;
import org.sonarqube.ws.client.permissions.RemoveGroupRequest;
import org.sonarqube.ws.client.projects.CreateRequest;
import org.sonarqube.ws.client.qualityprofiles.SearchRequest;

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
  private static final String DBD_PROJECT_KEY = "dbd";
  private static final String CUSTOM_SECRETS_PROJECT_KEY = "secrets-custom";
  private static final String INSUFFICIENT_PERMISSION_USER = "iHaveNoRights";
  private static final MarkerDescriptionMatcher ISSUE_MATCHER = new MarkerDescriptionMatcher(
    CoreMatchers.containsString("System.out"));

  /** Orchestrator to not be re-used in order for ITs to not fail -> always use latest release locally (not LTS) */
  @ClassRule
  public static final OrchestratorRule orchestrator = OrchestratorRule.builderEnv()
    .defaultForceAuthentication()
    .useDefaultAdminCredentialsForBuilds(true)
    .keepBundledPlugins()
    .setEdition(Edition.ENTERPRISE)
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
    adminWsClient.projects().create(new CreateRequest()
      .setName(JAVA_SIMPLE_PROJECT_KEY)
      .setProject(JAVA_SIMPLE_PROJECT_KEY));
    orchestrator.getServer().associateProjectToQualityProfile(JAVA_SIMPLE_PROJECT_KEY, "java", "SonarLint IT Java");
  }

  @After
  public void restoreDeactivatedRule() {
    var qualityProfile = getQualityProfile(JAVA_SIMPLE_PROJECT_KEY, "SonarLint IT Java");
    activateRule(qualityProfile, S106);

    shellByName("SonarQube Server - Invalid token for connection").ifPresent(DefaultShell::close);
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

    var authenticationPage = new ServerConnectionWizard.AuthenticationPage(wizard);
    assertThat(wizard.isNextEnabled()).isFalse();
    authenticationPage.setToken("Goo");
    assertThat(wizard.isNextEnabled()).isTrue();

    wizard.next();
    new WaitUntil(new DialogMessageIsExpected(wizard, "Authentication failed"));

    authenticationPage.setToken(token);
    wizard.next();

    // as login can take time, wait for the next page to appear
    new WaitUntil(new WidgetIsFound(Label.class, new WithTextMatcher("SonarQube Server Connection Identifier")));
    var connectionNamePage = new ServerConnectionWizard.ConnectionNamePage(wizard);

    assertThat(connectionNamePage.getConnectionName()).isEqualTo("127.0.0.1");
    assertThat(wizard.isNextEnabled()).isTrue();

    connectionNamePage.setConnectionName("");
    assertThat(wizard.isNextEnabled()).isFalse();
    new WaitUntil(new DialogMessageIsExpected(wizard, "Connection name must be specified"));

    connectionNamePage.setConnectionName("test");
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

    var bindingsView = new BindingsView();
    assertThat(bindingsView.getBindings()).extracting(Binding::getLabel).contains("test");
    new WaitWhile(new JobIsRunning(), TimePeriod.LONG);
  }

  private static class Status {
    private String ideName;
    private String description;
  }

  @Test
  @Ignore("Due to SLCORE-1083, has to be fixed later!")
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
      .create(new CreateRequest()
        .setName(SECRET_JAVA_PROJECT_NAME)
        .setProject(SECRET_JAVA_PROJECT_NAME));

    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("secrets/secret-java", SECRET_JAVA_PROJECT_NAME);

    createConnectionAndBindProject(orchestrator, SECRET_JAVA_PROJECT_NAME);

    // Remove binding suggestion notification
    new DefaultLink(shellByName("SonarQube - Binding Suggestion").get(), "Don't ask again").click();

    waitForAnalysisReady(SECRET_JAVA_PROJECT_NAME);

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "sec", "Secret.java"));
    waitForMarkers(new DefaultEditor(),
      tuple("Make sure this AWS Secret Access Key gets revoked, changed, and removed from the code.", 4));

    shellByName("SonarQube - Secret(s) detected").ifPresent(shell -> {
      assertThat(getNotificationText(shell)).contains(SECRET_JAVA_PROJECT_NAME);
      new DefaultLink(shell, "Dismiss").click();
    });
  }

  @Test
  public void shareConnectedModeConfiguration() {
    new JavaPerspective().open();

    var project = importExistingProjectIntoWorkspace("java/java-simple", JAVA_SIMPLE_PROJECT_KEY);

    // In order to not confuse the "waitForAnalysisReady" with older entries!
    new SonarLintConsole().clear();

    createConnectionAndBindProject(orchestrator, JAVA_SIMPLE_PROJECT_KEY);

    // Remove binding suggestion notification
    shellByName("SonarQube - Binding Suggestion")
      .ifPresent(shell -> new DefaultLink(shell, "Don't ask again").click());

    waitForAnalysisReady(JAVA_SIMPLE_PROJECT_KEY);

    // Share Connected Mode configuration
    new ContextMenu(project.getTreeItem()).getItem("SonarQube", "Share Binding...").select();
    new WaitUntil(new ShareConnectedModeConfigurationDialogOpened());

    var dialog = new ShareConnectedModeConfigurationDialog();
    dialog.saveToProject();
  }

  @Test
  public void shouldAutomaticallyUpdateRuleSetWhenChangedOnServer() throws Exception {
    new JavaPerspective().open();

    // In order to not confuse the "waitForAnalysisReady" with older entries!
    var console = new SonarLintConsole();
    console.clear();

    var rootProject = importExistingProjectIntoWorkspace("java/java-simple", JAVA_SIMPLE_PROJECT_KEY);

    createConnectionAndBindProject(orchestrator, JAVA_SIMPLE_PROJECT_KEY);

    // Remove binding suggestion notification
    shellByName("SonarQube - Binding Suggestion")
      .ifPresent(shell -> new DefaultLink(shell, "Don't ask again").click());

    waitForAnalysisReady(JAVA_SIMPLE_PROJECT_KEY);

    var file = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(file);

    // INFO: This is a corner case where we cannot use AbstractSonarLintTest#waitForMarkers!
    var defaultEditor = new TextEditor();
    await().untilAsserted(() -> {
      assertThat(defaultEditor.getMarkers())
        .filteredOn(marker -> marker.getType().equals("org.sonarlint.eclipse.onTheFlyIssueAnnotationType"))
        .hasSize(1);
      assertThat(defaultEditor.getMarkers())
        .filteredOn(marker -> marker.getType().equals("org.sonarlint.eclipse.onTheFlyIssueAnnotationType"))
        .satisfiesAnyOf(
          list -> assertThat(list)
            .extracting(Marker::getText, Marker::getLineNumber)
            .containsOnly(tuple("Replace this use of System.out by a logger.", 9)),
          list -> assertThat(list)
            .extracting(Marker::getText, Marker::getLineNumber)
            .containsOnly(tuple("Replace this use of System.out or System.err by a logger.", 9)));
    });

    var qualityProfile = getQualityProfile(JAVA_SIMPLE_PROJECT_KEY, "SonarLint IT Java");
    deactivateRule(qualityProfile, S106);

    await().atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
      Thread.sleep(5000);

      doAndWaitForSonarLintAnalysisJob(() -> {
        defaultEditor.insertText(0, " ");
        defaultEditor.save();
      });

      assertThat(defaultEditor.getMarkers())
        .filteredOn(marker -> marker.getType().equals("org.sonarlint.eclipse.onTheFlyIssueAnnotationType"))
        .isEmpty();
    });
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
    new DefaultLink(shellByName("SonarQube - Binding Suggestion").get(), "Don't ask again").click();

    waitForAnalysisReady(JAVA_SIMPLE_PROJECT_KEY);

    var file = rootProject.getResource("src", "hello", "Hello.java");
    openFileAndWaitForAnalysisCompletion(file);

    // INFO: This is a corner case where we cannot use AbstractSonarLintTest#waitForSonarLintMarkers!
    await().untilAsserted(() -> {
      assertThat(onTheFlyView.getIssues(ISSUE_MATCHER)).hasSize(1);
      assertThat(onTheFlyView.getIssues(ISSUE_MATCHER))
        .satisfiesAnyOf(
          list -> assertThat(list)
            .extracting(SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getResource)
            .containsOnly(tuple("Replace this use of System.out by a logger.", "Hello.java")),
          list -> assertThat(list)
            .extracting(SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getResource)
            .containsOnly(tuple("Replace this use of System.out or System.err by a logger.", "Hello.java")));
    });

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
  public void test_MarkIssueAs_Dialog() {
    // INFO: It is flaky when running on top of the oldest Eclipse version but works fine in the other test cases,
    // therefore it should be skipped in that particular situation!
    Assume.assumeTrue(!"oldest".equals(System.getProperty("target.platform")));

    // 1) Create project on SonarQube
    adminWsClient.projects()
      .create(new CreateRequest()
        .setName(MAVEN2_PROJECT_KEY)
        .setProject(MAVEN2_PROJECT_KEY));
    orchestrator.getServer().associateProjectToQualityProfile(MAVEN2_PROJECT_KEY, "java", "SonarLint IT Java");

    // 2) Import project into workspace
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/maven2", MAVEN2_PROJECT_KEY);

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    // 3) Run SonarQube analysis
    runMavenBuild(orchestrator, MAVEN2_PROJECT_KEY, "java/maven2/pom.xml", Map.of());

    // 4) Add new user to SonarQube
    adminWsClient.users().create(new org.sonarqube.ws.client.users.CreateRequest()
      .setLogin(INSUFFICIENT_PERMISSION_USER)
      .setPassword(INSUFFICIENT_PERMISSION_USER)
      .setName(INSUFFICIENT_PERMISSION_USER));

    // 5) Remove user rights from project
    adminWsClient.permissions().removeGroup(new RemoveGroupRequest()
      .setProjectKey(MAVEN2_PROJECT_KEY)
      .setGroupName("sonar-users")
      .setPermission("issueadmin"));

    // 6) Remove connections and reconnect with user
    createConnectionAndBindProject(orchestrator, MAVEN2_PROJECT_KEY);

    // 7) Remove binding suggestion notification
    new DefaultLink(shellByName("SonarQube - Binding Suggestion").get(), "Don't ask again").click();

    waitForAnalysisReady(MAVEN2_PROJECT_KEY);

    // 8) Open first file and try to open the "Mark issue as ..." dialog
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src/main/java", "hello", "Hello.java"));

    await().until(() -> onTheFlyView.getIssues(ISSUE_MATCHER), findings -> !findings.isEmpty());
    onTheFlyView.getIssues(ISSUE_MATCHER).get(0).select();
    new ContextMenuItem(onTheFlyView.getTree(), "Mark Issue as...").select();

    new PushButton(shellByName("Re-Opening resolved Issue on SonarQube Server").get(), "OK").click();

    // 9) Assert marker is still available
    await().untilAsserted(() -> assertThat(onTheFlyView.getIssues(ISSUE_MATCHER)).satisfiesAnyOf(
      list -> assertThat(list)
        .extracting(i -> i.getDescription())
        .containsOnly("Replace this use of System.out by a logger."),
      list -> assertThat(list)
        .extracting(i -> i.getDescription())
        .containsOnly("Replace this use of System.out or System.err by a logger.")));

    // 10) Add user rights from project
    adminWsClient.permissions().addGroup(new AddGroupRequest()
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
    new DefaultLink(shellByName("SonarQube - Issue marked as resolved").get(), "Dismiss").click();

    // 13) Assert marker is gone
    await().until(() -> onTheFlyView.getIssues(ISSUE_MATCHER), List<SonarLintIssueMarker>::isEmpty);
  }

  // integration test for focusing on new code in connected mode
  @Test
  public void test_new_code_period_preference() {
    Assume.assumeTrue("latest".equals(System.getProperty("target.platform", "latest")));

    // 1) create project on server / run first analysis
    adminWsClient.projects()
      .create(new CreateRequest()
        .setName(MAVEN_TAINT_PROJECT_KEY)
        .setProject(MAVEN_TAINT_PROJECT_KEY));
    orchestrator.getServer().associateProjectToQualityProfile(MAVEN_TAINT_PROJECT_KEY, "java", "SonarLint IT New Code");

    runMavenBuild(orchestrator, MAVEN_TAINT_PROJECT_KEY, "java/maven-taint/pom.xml", Map.of());

    // 2) import project / check that new code period preference does nothing in standalone mode
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/maven-taint", MAVEN_TAINT_PROJECT_KEY);

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();
    var taintVulnerabilitiesView = new SonarLintTaintVulnerabilitiesView();
    taintVulnerabilitiesView.open();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src/main/java", "taint", "taint_issue.java"));

    await().untilAsserted(() -> assertThat(onTheFlyView.getItems()).hasSize(2));

    setFocusOnNewCode(true);

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src/main/java", "taint", "taint_issue.java"));

    await().untilAsserted(() -> assertThat(onTheFlyView.getItems()).hasSize(2));

    setFocusOnNewCode(false);

    // 3) bind to project on SonarQube / check issues and taint vulnerabilities exist
    createConnectionAndBindProject(orchestrator, MAVEN_TAINT_PROJECT_KEY);

    new DefaultLink(shellByName("SonarQube - Binding Suggestion").get(), "Don't ask again").click();

    waitForAnalysisReady(MAVEN_TAINT_PROJECT_KEY);

    await().untilAsserted(() -> assertThat(onTheFlyView.getItems()).hasSize(2));
    await().untilAsserted(() -> assertThat(taintVulnerabilitiesView.getItems()).hasSize(1));

    new DefaultLink(shellByName("SonarQube - Taint vulnerability found").get(), "Show in view").click();

    // 4) unbind project / set new code period to "previous version" / run second analysis with a new version
    var bindingsView = new BindingsView();
    bindingsView.open();
    bindingsView.removeAllBindings();

    setNewCodePeriodToPreviousVersion(MAVEN_TAINT_PROJECT_KEY);

    runMavenBuild(orchestrator, MAVEN_TAINT_PROJECT_KEY, "java/maven-taint/pom.xml",
      Map.of("sonar.projectVersion", "1.1-SNAPSHOT"));

    // Because of a bug in SQ that returns the techncial issue creation date, we have to run another analysis
    // to be sure issues will be before the new code period
    runMavenBuild(orchestrator, MAVEN_TAINT_PROJECT_KEY, "java/maven-taint/pom.xml",
      Map.of("sonar.projectVersion", "1.2-SNAPSHOT"));

    // 5) bind to project on SonarQube / check that new code period preference is working
    createConnectionAndBindProject(orchestrator, MAVEN_TAINT_PROJECT_KEY);

    waitForAnalysisReady(MAVEN_TAINT_PROJECT_KEY);

    await().untilAsserted(() -> assertThat(onTheFlyView.getItems()).hasSize(2));
    await().untilAsserted(() -> assertThat(taintVulnerabilitiesView.getItems()).hasSize(1));

    setFocusOnNewCode(true);

    await().untilAsserted(() -> assertThat(onTheFlyView.getItems()).isEmpty());
    await().untilAsserted(() -> assertThat(taintVulnerabilitiesView.getItems()).isEmpty());
  }

  @Test
  public void test_Java_Python_DBD() {
    // INFO: Since 10.6 this is supported for SonarLint for Eclipse!
    Assume.assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals(10, 6));

    // 1) create project on server / run first analysis
    adminWsClient.projects()
      .create(new CreateRequest()
        .setName(DBD_PROJECT_KEY)
        .setProject(DBD_PROJECT_KEY));
    orchestrator.getServer().associateProjectToQualityProfile(DBD_PROJECT_KEY, "java", "SonarLint IT Java DBD");
    orchestrator.getServer().associateProjectToQualityProfile(DBD_PROJECT_KEY, "py", "SonarLint IT Python DBD");

    // 2) import project / check that no issues exist yet
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("dbd", DBD_PROJECT_KEY);

    var onTheFlyView = new OnTheFlyView();
    onTheFlyView.open();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("dbd.py"));
    waitForNoSonarLintMarkers(onTheFlyView);
    new DefaultEditor().close();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "dbd", "Main.java"));
    waitForNoSonarLintMarkers(onTheFlyView);
    new DefaultEditor().close();

    // 3) bind to project on SonarQube / check issues exist now
    createConnectionAndBindProject(orchestrator, DBD_PROJECT_KEY);
    shellByName("SonarQube - Binding Suggestion").ifPresent(shell -> new DefaultLink(shell, "Don't ask again").click());

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("dbd.py"));
    waitForSonarLintMarkers(onTheFlyView,
      tuple("Fix this condition that always evaluates to false; some subsequent code is never executed. [+2 locations]", "dbd.py", "few seconds ago"));
    new DefaultEditor().close();

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src", "dbd", "Main.java"));

    // Due to changes in the DBD Java analyzer the rule "S6466" was changed to now find more locations. This analyzer
    // is only included in the latest version of SonarQube Server!
    Awaitility.await()
      .atMost(20, TimeUnit.SECONDS)
      .untilAsserted(() -> {
        assertThat(onTheFlyView.getIssues()).hasSize(1);
        assertThat(onTheFlyView.getIssues())
          .extracting(SonarLintIssueMarker::getDescription, SonarLintIssueMarker::getResource, SonarLintIssueMarker::getCreationDate)
          .containsAnyOf(
            tuple("Fix this access on a collection that may trigger an 'ArrayIndexOutOfBoundsException'. [+2 locations]", "Main.java", "few seconds ago"),
            tuple("Fix this access on a collection that may trigger an 'ArrayIndexOutOfBoundsException'. [+4 locations]", "Main.java", "few seconds ago"));
      });

    new DefaultEditor().close();
  }

  @Test
  public void test_custom_secrets() {
    // INFO: Since 10.4 this is supported for SonarLint for Eclipse!
    Assume.assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals(10, 4));

    // 1) create project on server / configure quality profile
    adminWsClient.projects()
      .create(new CreateRequest()
        .setName(CUSTOM_SECRETS_PROJECT_KEY)
        .setProject(CUSTOM_SECRETS_PROJECT_KEY));
    orchestrator.getServer().associateProjectToQualityProfile(CUSTOM_SECRETS_PROJECT_KEY, "secrets", "SonarLint IT Custom Secrets");

    // 2) import project / check that no issues exist yet
    var rootProject = importExistingProjectIntoWorkspace("secrets/secrets-custom", CUSTOM_SECRETS_PROJECT_KEY);

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("Heresy.txt"));
    waitForNoMarkers(new DefaultEditor());
    new DefaultEditor().close();

    // 3) bind to project on SonarQube / check issues exist now
    createConnectionAndBindProject(orchestrator, CUSTOM_SECRETS_PROJECT_KEY);
    shellByName("SonarQube - Binding Suggestion").ifPresent(shell -> new DefaultLink(shell, "Don't ask again").click());

    openFileAndWaitForAnalysisCompletion(rootProject.getResource("Heresy.txt"));
    waitForMarkers(new DefaultEditor(),
      tuple("User-specified secrets should not be disclosed.", 1));

    shellByName("SonarQube - Secret(s) detected").ifPresent(shell -> {
      assertThat(getNotificationText(shell)).contains(CUSTOM_SECRETS_PROJECT_KEY);
      shell.close();
    });
  }

  private static QualityProfile getQualityProfile(String projectKey, String qualityProfileName) {
    var searchReq = new SearchRequest();
    searchReq.setQualityProfile(qualityProfileName);
    searchReq.setProject(projectKey);
    searchReq.setDefaults("false");
    var search = adminWsClient.qualityprofiles().search(searchReq);
    for (var profile : search.getProfilesList()) {
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
      .setParam("rule", "java:" + ruleKey);
    try (var response = adminWsClient.wsConnector().call(request)) {
      assertTrue("Unable to deactivate rule " + ruleKey, response.isSuccessful());
    }
  }

  private static void activateRule(QualityProfile qualityProfile, String ruleKey) {
    var request = new PostRequest("/api/qualityprofiles/activate_rule")
      .setParam("key", qualityProfile.getKey())
      .setParam("rule", "java:" + ruleKey);
    try (var response = adminWsClient.wsConnector().call(request)) {
      assertTrue("Unable to activate rule " + ruleKey, response.isSuccessful());
    }
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
