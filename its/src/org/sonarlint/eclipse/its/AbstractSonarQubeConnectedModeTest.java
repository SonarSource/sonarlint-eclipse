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

import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import com.sonar.orchestrator.locator.URLLocation;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.swt.widgets.Label;
import org.junit.Before;
import org.osgi.framework.FrameworkUtil;
import org.sonarlint.eclipse.its.reddeer.views.BindingsView;
import org.sonarlint.eclipse.its.reddeer.wizards.ProjectBindingWizard;
import org.sonarlint.eclipse.its.reddeer.wizards.ProjectSelectionDialog;
import org.sonarlint.eclipse.its.reddeer.wizards.ServerConnectionWizard;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.project.CreateRequest;
import org.sonarqube.ws.client.setting.SetRequest;

import static org.assertj.core.api.Assertions.fail;

/** Every test class targeting SonarQube derives from here */
public abstract class AbstractSonarQubeConnectedModeTest extends AbstractSonarLintTest {
  protected static WsClient adminWsClient;
  
  /** Should be used on @BeforeClass implementation for orchestrators to share the logic */
  public static void prepare(OrchestratorRule orchestrator) {
    adminWsClient = newAdminWsClient(orchestrator.getServer());
    adminWsClient.settings().set(SetRequest.builder().setKey("sonar.forceAuthentication").setValue("true").build());
    
    try {
      orchestrator.getServer().restoreProfile(
        URLLocation.create(FileLocator.toFileURL(FileLocator.find(FrameworkUtil.getBundle(SonarQubeConnectedModeTest.class), new Path("res/java-sonarlint.xml"), null))));
      orchestrator.getServer().restoreProfile(
        URLLocation.create(FileLocator.toFileURL(FileLocator.find(FrameworkUtil.getBundle(SonarQubeConnectedModeTest.class), new Path("res/java-sonarlint-new-code.xml"), null))));
    } catch (IOException e) {
      fail("Unable to load quality profile", e);
    }
  }

  @Before
  public void cleanBindings() {
    var bindingsView = new BindingsView();
    bindingsView.open();
    bindingsView.removeAllBindings();
  }
  
  /** Create a project on SonarQube via Web API with corresponding quality profile assigned */
  public void createProjectOnSonarQube(OrchestratorRule orchestrator, String projectKey, String qualityProfile) {
    adminWsClient.projects()
      .create(CreateRequest.builder()
        .setName(projectKey)
        .setKey(projectKey)
        .build());
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "java", qualityProfile);
  }
  
  /** Run Maven build on specific project in folder with optional additional analysis properties */
  public void runMavenBuild(OrchestratorRule orchestrator, String projectKey, String folder, String path,
    Map<String, String> analysisProperties) {
    var build = MavenBuild.create(new File(folder, path))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.login", Server.ADMIN_LOGIN)
      .setProperty("sonar.password", Server.ADMIN_PASSWORD)
      .setProperty("sonar.projectKey", projectKey);
    
    for (var pair: analysisProperties.entrySet()) {
      build = build.setProperty(pair.getKey(), pair.getValue());
    }

    orchestrator.executeBuild(build);
  }
  
  /** Bind a specific project to SonarQube */
  protected static void createConnectionAndBindProject(OrchestratorRule orchestrator, String projectKey) {
    createConnectionAndBindProject(orchestrator, projectKey, Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD);
  }

  protected static void createConnectionAndBindProject(OrchestratorRule orchestrator, String projectKey,
    String username, String password) {
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
    authenticationPage.setUsername(username);
    authenticationPage.setPassword(password);
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

    new WaitWhile(new JobIsRunning(), TimePeriod.LONG);

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
}
