/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2023 SonarSource SA
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
import com.sonar.orchestrator.junit4.OrchestratorRule;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.Map;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.ui.perspectives.JavaPerspective;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.impl.editor.TextEditor;
import org.junit.After;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarlint.eclipse.its.reddeer.conditions.OpenInIdeDialogOpened;
import org.sonarlint.eclipse.its.reddeer.conditions.RuleDescriptionViewOpenedWithContent;
import org.sonarlint.eclipse.its.reddeer.conditions.SonarLintTaintVulnerabilitiesViewOpened;
import org.sonarlint.eclipse.its.reddeer.preferences.SonarLintPreferences.IssueFilter;
import org.sonarlint.eclipse.its.reddeer.preferences.SonarLintPreferences.IssuePeriod;
import org.sonarlint.eclipse.its.reddeer.views.RuleDescriptionView;
import org.sonarlint.eclipse.its.reddeer.views.SonarLintTaintVulnerabilitiesView;
import org.sonarlint.eclipse.its.reddeer.wizards.OpenInIdeDialog;
import org.sonarqube.ws.Issues.Issue;
import org.sonarqube.ws.WsBranches.Branch;
import org.sonarqube.ws.client.issue.SearchWsRequest;
import org.sonarqube.ws.client.project.DeleteRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *  Integration tests on the "Open in IDE" feature only available on ibuilds as it is available since SonarQube 10.2+
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
  }
  
  /** as we re-use the same project we have to delete it on SonarQube after every test */
  @After
  public void deleteSonarQubeProjects() {
    if ("ibuilds".equals(System.getProperty("target.platform", "ibuilds"))) {
      adminWsClient.projects().delete(DeleteRequest.builder().setKey(MAVEN_TAINT_PROJECT_KEY).build());
    }
  }
  
  /** integration test for when the feature fails due to the local file not being found */
  @Test
  public void test_open_in_ide_without_corner_cases() throws IOException, InterruptedException {
    // Only available since SonarQube 10.2+ (ibuilds / locally)
    Assume.assumeTrue("ibuilds".equals(System.getProperty("target.platform", "ibuilds")));
    
    // 1) create project on server / run first analysis
    createProjectOnSonarQube(orchestrator, MAVEN_TAINT_PROJECT_KEY, "SonarLint IT New Code");
    runMavenBuild(orchestrator, MAVEN_TAINT_PROJECT_KEY, "projects", "java/maven-taint/pom.xml",
      Map.of("sonar.branch.name", "main"));
    
    // 2) import project and bind to SonarQube
    new JavaPerspective().open();
    var rootProject = importExistingProjectIntoWorkspace("java/maven-taint", MAVEN_TAINT_PROJECT_KEY);
    createConnectionAndBindProject(orchestrator, MAVEN_TAINT_PROJECT_KEY);
    new DefaultShell("SonarLint Binding Suggestion").close();
    
    // 3) delete file
    rootProject.getResource("src/main/java", "taint", "Variables.java").delete();
    
    // 4) get S1481 issue key / branch name from SonarQube
    var s101 = getFirstIssue(S1481);
    assertThat(s101).isNotNull();
    
    var branch = getFirstBranch();
    assertThat(branch).isNotNull();
    
    // 5) trigger "Open in IDE" feature: issue not found because file not found
    //    -> because the API is quite slow we have to await the opening of the error dialog
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s101.getKey());
    new WaitUntil(new OpenInIdeDialogOpened(), TimePeriod.DEFAULT);
    new OpenInIdeDialog().ok();
    
    // 6) change preferences regarding new code / issue filter + change file content
    setNewCodePreference(IssuePeriod.NEW_CODE);
    openFileAndWaitForAnalysisCompletion(rootProject.getResource("src/main/java", "taint", "SysOut.java"));
    var textEditor = new TextEditor("SysOut.java");
    textEditor.setText("package taint;\n"
      + "\n"
      + "public class SysOut {\n"
      + "  public static void main(String[] args) {\n"
      + "  }\n"
      + "}\n"
      + "");
    textEditor.save();
    Thread.sleep(1000); // we have to wait for the analysis to finish
    
    // 7) get S106 issue key from SonarQube
    var s106 = getFirstIssue(S106);
    assertThat(s106).isNotNull();
    
    // 8) trigger "Open in IDE" feature: issue not found because workspace preferences
    //    -> after we acknowledge SL to change preferences the dialog will open again
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s106.getKey());
    new WaitUntil(new OpenInIdeDialogOpened(), TimePeriod.DEFAULT);
    new OpenInIdeDialog().yes();
    
    Thread.sleep(500); // we have to wait for the preference page to open and to get the job data
    setIssueFilterPreference(IssueFilter.ALL_ISSUES);
    
    new WaitUntil(new OpenInIdeDialogOpened(), TimePeriod.DEFAULT);
    new OpenInIdeDialog().ok();
    
    // 9) get S101 issue key from SonarQube
    var s1481 = getFirstIssue(S101);
    assertThat(s1481).isNotNull();
    
    // 10) trigger "Open in IDE" feature: issue opens correctly including On-The-Fly / Rule Description view
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s1481.getKey());
    var ruleDescriptionView = new RuleDescriptionView();
    new WaitUntil(new RuleDescriptionViewOpenedWithContent(ruleDescriptionView, S101), TimePeriod.DEFAULT);
    ruleDescriptionView.open();
    
    // 11) get S2083 issue key from SonarQube
    var s2083 = getFirstIssue(S2083);
    assertThat(s2083).isNotNull();
    
    // 12) trigger "Open in IDE" feature: taint vulnerability opens correctly including Taint Vulnerabilities view
    triggerOpenInIDE(orchestrator.getServer().getUrl(), branch.getName(), s2083.getKey());
    var sonarLintTaintVulnerabilitiesView = new SonarLintTaintVulnerabilitiesView();
    new WaitUntil(new SonarLintTaintVulnerabilitiesViewOpened(sonarLintTaintVulnerabilitiesView), TimePeriod.DEFAULT);
    sonarLintTaintVulnerabilitiesView.open();
    assertThat(sonarLintTaintVulnerabilitiesView.getItems()).isNotEmpty();
    new DefaultShell("SonarLint - Taint vulnerability found").close();
  }
  
  /**
   *  To emulate a user clicking on "Open in IDE" on their SonarQube on an issue
   *  
   *  @param serverURL the server URL to be passed to the web request ???
   *  @param branch issue branch
   *  @param projectKey project containing the issue
   *  @param issueKey specific issue
   *  @throws IOException when connection fails
   * @throws InterruptedException 
   */
  private void triggerOpenInIDE(String serverURL, String branch, String issueKey) throws InterruptedException, IOException {
    assertThat(hotspotServerPort).isNotEqualTo(-1);
    
    var request = HttpRequest.newBuilder()
      .uri(URI.create("http://localhost:" + hotspotServerPort + "/sonarlint/api/issues/show?server=" + URLEncoder.encode(serverURL)
        + "&project=" + MAVEN_TAINT_PROJECT_KEY
        + "&issue=" + issueKey
        + "&branch=" + branch))
      .header("Origin", serverURL)
      .header("Referer", serverURL)
      .GET().build();
    
    var response = HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
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
