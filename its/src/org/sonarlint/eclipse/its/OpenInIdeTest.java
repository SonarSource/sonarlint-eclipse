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

import com.sonar.orchestrator.container.Edition;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.jdt.ui.wizards.NewClassCreationWizard;
import org.eclipse.reddeer.eclipse.jdt.ui.wizards.NewClassWizardPage;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.link.DefaultLink;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.eclipse.swt.widgets.Label;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonarlint.eclipse.its.reddeer.conditions.ConfirmConnectionCreationDialogOpened;
import org.sonarlint.eclipse.its.reddeer.conditions.OpenInIdeDialogOpened;
import org.sonarlint.eclipse.its.reddeer.conditions.RuleDescriptionViewOpenedWithContent;
import org.sonarlint.eclipse.its.reddeer.conditions.SonarLintTaintVulnerabilitiesViewOpened;
import org.sonarlint.eclipse.its.reddeer.dialogs.ConfirmConnectionCreationDialog;
import org.sonarlint.eclipse.its.reddeer.dialogs.OpenInIdeDialog;
import org.sonarlint.eclipse.its.reddeer.dialogs.ProjectSelectionDialog;
import org.sonarlint.eclipse.its.reddeer.preferences.SonarLintPreferences.IssueFilter;
import org.sonarlint.eclipse.its.reddeer.preferences.SonarLintPreferences.IssuePeriod;
import org.sonarlint.eclipse.its.reddeer.views.RuleDescriptionView;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintTaintVulnerabilitiesView;
import org.sonarlint.eclipse.its.reddeer.wizards.ServerConnectionWizard;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.WsBranches.Branch;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import org.sonarqube.ws.client.project.DeleteRequest;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *  Integration tests on the "Open in IDE" feature only available since SonarQube 10.2+
 */
public class OpenInIdeTest extends AbstractSonarQubeConnectedModeTest {
  private static final String MAVEN_TAINT_PROJECT_KEY = "maven-taint";
  private static final String S101 = "java:S101";
  private static final String S106 = "java:S106";
  private static final String S1481 = "java:S1481";
  private static final String S2083 = "javasecurity:S2083";

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

    if (orchestrator.getServer().version().isGreaterThanOrEquals(10, 2)) {
      createProjectOnSonarQube(orchestrator, MAVEN_TAINT_PROJECT_KEY, "SonarLint IT New Code");
      runMavenBuild(orchestrator, MAVEN_TAINT_PROJECT_KEY, "projects", "java/maven-taint/pom.xml",
        Map.of("sonar.branch.name", "main"));
    }
  }

  @AfterClass
  public static void deleteSonarQubeProjects() {
    if (orchestrator.getServer().version().isGreaterThanOrEquals(10, 2)) {
      adminWsClient.projects().delete(DeleteRequest.builder().setKey(MAVEN_TAINT_PROJECT_KEY).build());
    }
  }

  /**
   *  Integration test for the following case: Connected to SQ 10.2 / 10.3 (therefore "Open in IDE" provides no token)
   *  with matching project -> user has to manually connect, binding will be done automatically
   */
  @Test
  @Ignore("TODO: Enable later, disabled just for SQ analysis!")
  public void test_open_in_ide_assist_manual_binding() throws IOException, InterruptedException {
    // Only available since SonarQube 10.2+, enhanced with token by SonarQube 10.4
    var version = orchestrator.getServer().version();
    Assume.assumeTrue(version.isGreaterThanOrEquals(10, 2));

    // 1) import project
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/maven-taint", MAVEN_TAINT_PROJECT_KEY);

    // 2) get S1481 issue key / branch name from SonarQube
    var s101 = getFirstIssue(S101);
    assertThat(s101).isNotNull();

    var branch = getFirstBranch();
    assertThat(branch).isNotNull();

    // 3) trigger "Open in IDE" feature, but cancel
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s101.getKey());
    new WaitUntil(new ConfirmConnectionCreationDialogOpened(), TimePeriod.DEFAULT);
    new ConfirmConnectionCreationDialog().donottrust();

    // 4) trigger "Open in IDE" feature, but accept this time
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s101.getKey());
    new WaitUntil(new ConfirmConnectionCreationDialogOpened(), TimePeriod.DEFAULT);
    new ConfirmConnectionCreationDialog().trust();

    var wizard = new ServerConnectionWizard();
    assertThat(new ServerConnectionWizard.ServerTypePage(wizard).isSonarQubeSelected()).isTrue();
    wizard.next();

    var serverUrlPage = new ServerConnectionWizard.ServerUrlPage(wizard);
    assertThat(serverUrlPage.getUrl()).isEqualTo(orchestrator.getServer().getUrl());
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

    connectionNamePage.setConnectionName("from open IDE");
    wizard.next();

    wizard.next();
    wizard.finish();

    try {
      var projectSelectionDialog = new ProjectSelectionDialog();
      assertThat(projectSelectionDialog.getMessage()).contains("This Eclipse project will be bound to the Sonar project 'maven-taint' using connection 'from open IDE'");
      projectSelectionDialog.filterProjectName(MAVEN_TAINT_PROJECT_KEY);
      projectSelectionDialog.ok();
    } catch (Exception err) {
      // ================================================================================================================
      // INFO: Because of SLE-797 we currently have to bind it manually and trigger it again
      var popUp = new DefaultShell("SonarLint - No mathing open project found");
      new DefaultLink(popUp, "Open Troubleshooting documentation").click();
      bindProjectFromContextMenu(rootProject, MAVEN_TAINT_PROJECT_KEY);
      triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s101.getKey());
      // ================================================================================================================
    }

    // 5) check rule description view
    var ruleDescriptionView = new RuleDescriptionView();
    new WaitUntil(new RuleDescriptionViewOpenedWithContent(ruleDescriptionView, S101), TimePeriod.DEFAULT);

    closeTaintPopupIfAny();
  }

  /**
   *  Integration test for the following case: Connected to SQ 10.4+ (therefore "Open in IDE" provides a token) but
   *  workspace is empty -> SLCORE cannot match any project, so the user has to manually bind the project
   */
  @Test
  @Ignore("TODO: Enable later, disabled just for SQ analysis!")
  public void test_open_in_ide_assist_automated_binding_empty_workspace() throws InterruptedException, IOException {
    // Only available since SonarQube 10.4+
    Assume.assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals(10, 4));

    // 1) Generate first token
    var tokenName = "tokenName1";
    var tokenValue = adminWsClient
      .userTokens()
      .generate(new GenerateWsRequest().setName(tokenName).setLogin(Server.ADMIN_LOGIN))
      .getToken();

    // 2) get S1481 issue key / branch name from SonarQube
    var s101 = getFirstIssue(S101);
    assertThat(s101).isNotNull();

    var branch = getFirstBranch();
    assertThat(branch).isNotNull();

    // 3) trigger "Open in IDE" feature and accept
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s101.getKey(), tokenName, tokenValue);
    new WaitUntil(new ConfirmConnectionCreationDialogOpened(), TimePeriod.DEFAULT);
    new ConfirmConnectionCreationDialog().trust();

    // 4) await pop-up saying that automatic binding is not possible
    var popUp = new DefaultShell("SonarLint - No mathing open project found");
    new DefaultLink(popUp, "Open Troubleshooting documentation").click();

    // 5) Check that token still exists
    var userTokens = adminWsClient
      .userTokens()
      .search(new org.sonarqube.ws.client.usertoken.SearchWsRequest().setLogin(Server.ADMIN_LOGIN))
      .getUserTokensList();
    assertThat(userTokens.stream().filter(token -> tokenName.equals(token.getName())).collect(Collectors.toList())).hasSize(1);
  }

  @Test
  @Ignore("TODO: Enable later, disabled just for SQ analysis!")
  public void test_open_in_ide_assist_automated_binding() throws IOException, InterruptedException {
    // Only available since SonarQube 10.4+
    Assume.assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals(10, 4));

    // 1) Generate first token
    var tokenName = "tokenName";
    var tokenValue = adminWsClient
      .userTokens()
      .generate(new GenerateWsRequest().setName(tokenName).setLogin(Server.ADMIN_LOGIN))
      .getToken();

    // 2) import project
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/maven-taint", MAVEN_TAINT_PROJECT_KEY);

    // 3) get S1481 issue key / branch name from SonarQube
    var s101 = getFirstIssue(S101);
    assertThat(s101).isNotNull();

    var branch = getFirstBranch();
    assertThat(branch).isNotNull();

    // 4) trigger "Open in IDE" feature, but cancel
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s101.getKey(), tokenName, tokenValue);
    new WaitUntil(new ConfirmConnectionCreationDialogOpened(), TimePeriod.DEFAULT);
    new ConfirmConnectionCreationDialog().donottrust();

    // 5) Check that token was revoked
    var userTokens = adminWsClient
      .userTokens()
      .search(new org.sonarqube.ws.client.usertoken.SearchWsRequest().setLogin(Server.ADMIN_LOGIN))
      .getUserTokensList();
    assertThat(userTokens.stream().filter(token -> tokenName.equals(token.getName())).collect(Collectors.toList())).isEmpty();

    // 6) Generate second token
    tokenValue = adminWsClient
      .userTokens()
      .generate(new GenerateWsRequest().setName(tokenName).setLogin(Server.ADMIN_LOGIN))
      .getToken();

    // 7) trigger "Open in IDE" feature, but accept this time
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s101.getKey(), tokenName, tokenValue);
    new WaitUntil(new ConfirmConnectionCreationDialogOpened(), TimePeriod.DEFAULT);
    new ConfirmConnectionCreationDialog().trust();

    try {
      var projectSelectionDialog = new ProjectSelectionDialog();
      assertThat(projectSelectionDialog.getMessage()).contains("This Eclipse project will be bound to the Sonar project 'maven-taint' using connection '127.0.0.1'");
      projectSelectionDialog.filterProjectName(MAVEN_TAINT_PROJECT_KEY);
      projectSelectionDialog.ok();
    } catch (Exception err) {
      // ================================================================================================================
      // INFO: Because of SLE-797 we currently have to bind it manually and trigger it again
      var popUp = new DefaultShell("SonarLint - No mathing open project found");
      new DefaultLink(popUp, "Open Troubleshooting documentation").click();
      bindProjectFromContextMenu(rootProject, MAVEN_TAINT_PROJECT_KEY);
      triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s101.getKey());
      // ================================================================================================================
    }

    var ruleDescriptionView = new RuleDescriptionView();
    new WaitUntil(new RuleDescriptionViewOpenedWithContent(ruleDescriptionView, S101), TimePeriod.DEFAULT);

    // 8) Check that token still exists
    userTokens = adminWsClient
      .userTokens()
      .search(new org.sonarqube.ws.client.usertoken.SearchWsRequest().setLogin(Server.ADMIN_LOGIN))
      .getUserTokensList();
    assertThat(userTokens.stream().filter(token -> tokenName.equals(token.getName())).collect(Collectors.toList())).hasSize(1);

    closeTaintPopupIfAny();
  }

  private void closeTaintPopupIfAny() {
    try {
      new DefaultShell("SonarLint - Taint vulnerability found").close();
    } catch (CoreLayerException notFound) {

    }
  }

  @Test
  @Ignore("TODO: Enable later, disabled just for SQ analysis!")
  public void test_open_in_ide_when_project_already_bound() throws IOException, InterruptedException {
    // Only available since SonarQube 10.2+ (LATEST_RELEASE / locally)
    Assume.assumeTrue(orchestrator.getServer().version().isGreaterThanOrEquals(10, 2));

    // 1) import project and bind to SonarQube
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/maven-taint", MAVEN_TAINT_PROJECT_KEY);
    createConnectionAndBindProject(orchestrator, MAVEN_TAINT_PROJECT_KEY);
    try {
      new DefaultShell("SonarLint Binding Suggestion").close();
    } catch (Exception ignored) {
    }

    // 2) delete file
    rootProject.getResource("src/main/java", "taint", "Variables.java").delete();

    // 3) get S1481 issue key / branch name from SonarQube
    var s1481 = getFirstIssue(S1481);
    assertThat(s1481).isNotNull();

    var branch = getFirstBranch();
    assertThat(branch).isNotNull();

    // 4) trigger "Open in IDE" feature: issue not found because file not found
    // -> because the API is quite slow we have to await the opening of the error dialog
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s1481.getKey());
    new WaitUntil(new OpenInIdeDialogOpened(), TimePeriod.DEFAULT);
    new OpenInIdeDialog().ok();

    // 5) change preferences regarding new code / issue filter + change file content
    setNewCodePreference(IssuePeriod.NEW_CODE);

    // INFO: We get rid of the file and create a new one from scratch to simulate actual changes happening after the
    // last SonarQube analysis. Doing it by just changing the file was too flaky in the build pipeline!
    rootProject.getResource("src/main/java", "taint", "SysOut.java").delete();
    createClass("taint", "SysOut.java");
    var textEditor = new TextEditor("SysOut.java");
    textEditor.setText("package taint;\n"
      + "\n"
      + "public class SysOut {\n"
      + "  public static void main(String[] args) {\n"
      + "  }\n"
      + "}\n"
      + "");
    textEditor.save();
    textEditor.close();
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src/main/java", "taint", "SysOut.java"));

    // 6) get S106 issue key from SonarQube
    var s106 = getFirstIssue(S106);
    assertThat(s106).isNotNull();

    // 7) trigger "Open in IDE" feature: issue not found because workspace preferences
    // -> after we acknowledge SL to change preferences the dialog will open again
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s106.getKey());
    new WaitUntil(new OpenInIdeDialogOpened(), TimePeriod.DEFAULT);
    new OpenInIdeDialog().yes();

    Thread.sleep(500); // we have to wait for the preference page to open and to get the job data
    setIssueFilterPreference(IssueFilter.ALL_ISSUES);

    new WaitUntil(new OpenInIdeDialogOpened(), TimePeriod.DEFAULT);
    new OpenInIdeDialog().ok();

    // 8) get S101 issue key from SonarQube
    var s101 = getFirstIssue(S101);
    assertThat(s101).isNotNull();

    // 9) trigger "Open in IDE" feature: issue opens correctly including On-The-Fly / Rule Description view
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s101.getKey());
    var ruleDescriptionView = new RuleDescriptionView();
    new WaitUntil(new RuleDescriptionViewOpenedWithContent(ruleDescriptionView, S101), TimePeriod.DEFAULT);
    ruleDescriptionView.open();

    // 10) get S2083 issue key from SonarQube
    var s2083 = getFirstIssue(S2083);
    assertThat(s2083).isNotNull();

    // 11) trigger "Open in IDE" feature: taint vulnerability opens correctly including Taint Vulnerabilities view
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s2083.getKey());
    var sonarLintTaintVulnerabilitiesView = new SonarLintTaintVulnerabilitiesView();
    new WaitUntil(new SonarLintTaintVulnerabilitiesViewOpened(sonarLintTaintVulnerabilitiesView), TimePeriod.DEFAULT);
    sonarLintTaintVulnerabilitiesView.open();
    assertThat(sonarLintTaintVulnerabilitiesView.getItems()).isNotEmpty();

    closeTaintPopupIfAny();
  }

  /**
   *  To emulate a user clicking on "Open in IDE" on their SonarQube on an issue without any token
   */
  private void triggerOpenInIDE(String serverURL, String branch, String issueKey) throws InterruptedException, IOException {
    triggerOpenInIDE(serverURL, branch, issueKey, null, null);
  }

  /**
   *  To emulate a user clicking on "Open in IDE" on their SonarQube on an issue
   *
   *  @param serverURL the server URL to be passed to the web request ???
   *  @param branch issue branch
   *  @param projectKey project containing the issue
   *  @param issueKey specific issue
   *  @param tokenName (optional) token name for automatic connected mode setup
   *  @param tokenValue (optional) token value for automatic connected mode setup
   *  @throws IOException when connection fails
   * @throws InterruptedException
   */
  private void triggerOpenInIDE(String serverURL, String branch, String issueKey, String tokenName, String tokenValue) throws InterruptedException, IOException {
    assertThat(hotspotServerPort).isNotEqualTo(-1);

    var request = HttpRequest.newBuilder()
      .uri(URI.create("http://localhost:" + hotspotServerPort + "/sonarlint/api/issues/show?server=" + URLEncoder.encode(serverURL)
        + "&project=" + MAVEN_TAINT_PROJECT_KEY
        + "&issue=" + issueKey
        + "&branch=" + branch
        + (tokenName != null ? "&tokenName=" + tokenName : "")
        + (tokenValue != null ? "&tokenValue=" + tokenValue : "")))
      .header("Origin", serverURL)
      .header("Referer", serverURL)
      .GET().build();

    var response = HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
  }

  /** To create a new Java class in a specific package */
  private void createClass(String javaPackage, String className) {
    var dialog = new NewClassCreationWizard();
    dialog.open();
    var page = new NewClassWizardPage(dialog);
    page.setName(className);
    page.setPackage(javaPackage);
    dialog.finish();
  }

  /** Get first issue from project matching the rule key provided (does not contain branch information???) */
  private static Issue getFirstIssue(String ruleKey) {
    var response = adminWsClient.issues().search(new SearchWsRequest()
      .setRules(List.of(ruleKey))
      .setProjects(List.of(MAVEN_TAINT_PROJECT_KEY)));

    assertThat(response.getIssuesCount()).isPositive();
    return response.getIssues(0);
  }

  /** Get first branch from project */
  private static Branch getFirstBranch() {
    var response = adminWsClient.projectBranches().list(MAVEN_TAINT_PROJECT_KEY);
    assertThat(response.getBranchesCount()).isPositive();

    return response.getBranches(0);
  }
}
