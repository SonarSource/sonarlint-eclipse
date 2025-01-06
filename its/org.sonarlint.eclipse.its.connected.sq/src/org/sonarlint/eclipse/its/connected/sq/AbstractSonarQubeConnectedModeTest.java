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

import com.sonar.orchestrator.build.MavenBuild;
import com.sonar.orchestrator.container.Server;
import com.sonar.orchestrator.junit4.OrchestratorRule;
import com.sonar.orchestrator.locator.URLLocation;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.condition.WidgetIsFound;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.eclipse.core.resources.Project;
import org.eclipse.reddeer.swt.impl.menu.ContextMenu;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.swt.widgets.Label;
import org.junit.Before;
import org.osgi.framework.FrameworkUtil;
import org.sonarlint.eclipse.its.shared.AbstractSonarLintTest;
import org.sonarlint.eclipse.its.shared.reddeer.conditions.ProjectBindingWizardIsOpened;
import org.sonarlint.eclipse.its.shared.reddeer.dialogs.ProjectSelectionDialog;
import org.sonarlint.eclipse.its.shared.reddeer.views.BindingsView;
import org.sonarlint.eclipse.its.shared.reddeer.wizards.ProjectBindingWizard;
import org.sonarlint.eclipse.its.shared.reddeer.wizards.ServerConnectionWizard;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.projects.CreateRequest;
import org.sonarqube.ws.client.settings.SetRequest;
import org.sonarqube.ws.client.usertokens.GenerateRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/** Every test class targeting SonarQube derives from here */
public abstract class AbstractSonarQubeConnectedModeTest extends AbstractSonarLintTest {
  private static final String TIMESTAMP = Long.toString(Instant.now().toEpochMilli());
  private static final String TOKEN_NAME = "SLE-IT-" + TIMESTAMP;

  protected static WsClient adminWsClient;
  protected static String token;

  /** Should be used on @BeforeClass implementation for orchestrators to share the logic */
  public static void prepare(OrchestratorRule orchestrator) {
    adminWsClient = newAdminWsClient(orchestrator.getServer());
    token = adminWsClient.userTokens()
      .generate(new GenerateRequest().setName(TOKEN_NAME))
      .getToken();

    adminWsClient.settings().set(new SetRequest().setKey("sonar.forceAuthentication").setValue("true"));

    try {
      orchestrator.getServer().restoreProfile(
        URLLocation.create(FileLocator.toFileURL(FileLocator.find(FrameworkUtil.getBundle(SonarQubeConnectedModeTest.class), new Path("res/java-sonarlint.xml"), null))));
      orchestrator.getServer().restoreProfile(
        URLLocation.create(FileLocator.toFileURL(FileLocator.find(FrameworkUtil.getBundle(SonarQubeConnectedModeTest.class), new Path("res/java-sonarlint-new-code.xml"), null))));

      if (orchestrator.getServer().version().isGreaterThanOrEquals(10, 4)) {
        orchestrator.getServer().restoreProfile(
          URLLocation.create(FileLocator.toFileURL(FileLocator.find(FrameworkUtil.getBundle(SonarQubeConnectedModeTest.class), new Path("res/custom-secrets.xml"), null))));
      }

      if (orchestrator.getServer().version().isGreaterThanOrEquals(10, 6)) {
        orchestrator.getServer().restoreProfile(
          URLLocation.create(FileLocator.toFileURL(FileLocator.find(FrameworkUtil.getBundle(SonarQubeConnectedModeTest.class), new Path("res/java-sonarlint-dbd.xml"), null))));
        orchestrator.getServer().restoreProfile(
          URLLocation.create(FileLocator.toFileURL(FileLocator.find(FrameworkUtil.getBundle(SonarQubeConnectedModeTest.class), new Path("res/python-sonarlint-dbd.xml"), null))));
      }
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

  protected static WsClient newAdminWsClient(Server server) {
    return WsClientFactories.getDefault().newClient(HttpConnector.newBuilder()
      .url(server.getUrl())
      .credentials(Server.ADMIN_LOGIN, Server.ADMIN_PASSWORD)
      .build());
  }

  /** Create a project on SonarQube via Web API with corresponding quality profile assigned */
  public static void createProjectOnSonarQube(OrchestratorRule orchestrator, String projectKey, String qualityProfile) {
    adminWsClient.projects()
      .create(new CreateRequest()
        .setName(projectKey)
        .setProject(projectKey));
    orchestrator.getServer().associateProjectToQualityProfile(projectKey, "java", qualityProfile);
  }

  /** Run Maven build on specific project in folder with optional additional analysis properties */
  public static void runMavenBuild(OrchestratorRule orchestrator, String projectKey, String folder, String path,
    Map<String, String> analysisProperties) {
    var build = MavenBuild.create(new File(folder, path))
      .setCleanPackageSonarGoals()
      .setProperty("sonar.projectKey", projectKey);
    if (orchestrator.getServer().version().isGreaterThanOrEquals(10, 2)) {
      build = build.setProperty("sonar.token", orchestrator.getDefaultAdminToken());
    } else {
      build = build.setProperty("sonar.login", Server.ADMIN_LOGIN)
        .setProperty("sonar.password", Server.ADMIN_PASSWORD);
    }

    for (var pair : analysisProperties.entrySet()) {
      build = build.setProperty(pair.getKey(), pair.getValue());
    }

    orchestrator.executeBuild(build);
  }

  /** Bind a specific project to SonarQube */
  protected static void createConnectionAndBindProject(OrchestratorRule orchestrator, String projectKey) {
    var wizard = new ServerConnectionWizard();
    wizard.open();
    new ServerConnectionWizard.ServerTypePage(wizard).selectSonarQube();
    wizard.next();

    var serverUrlPage = new ServerConnectionWizard.ServerUrlPage(wizard);
    serverUrlPage.setUrl(orchestrator.getServer().getUrl());
    wizard.next();

    assertThat(wizard.isNextEnabled()).isFalse();
    var authenticationPage = new ServerConnectionWizard.AuthenticationPage(wizard);
    authenticationPage.setToken(token);
    assertThat(wizard.isNextEnabled()).isTrue();
    wizard.next();

    // as login can take time, wait for the next page to appear
    new WaitUntil(new WidgetIsFound(Label.class, new WithTextMatcher("SonarQube Server Connection Identifier")));
    var connectionNamePage = new ServerConnectionWizard.ConnectionNamePage(wizard);

    connectionNamePage.setConnectionName("test");
    assertThat(wizard.isNextEnabled()).isTrue();

    // Sadly we have to invoke sleep here as in the background there is SL communicating with SC regarding the
    // availability of notifications "on the server". As this is not done in a job we could listen to, we wait the
    // 5 seconds. Once we change it in SonarLint to not ask for notifications (for all supported SQ versions and SC
    // they are supported by now), we can somehow circumvent this.
    try {
      Thread.sleep(5000);
    } catch (InterruptedException ignored) {
    }
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

    // Close project binding wizard as we don't have a project opened
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
    serverProjectSelectionPage.setProjectKey(projectKey);
    projectBindingWizard.finish();
  }

  protected static void bindProjectFromContextMenu(Project project, String projectKey) {
    new ContextMenu(project.getTreeItem()).getItem("SonarQube", "Bind to SonarQube (Server, Cloud)...").select();

    var projectBindingWizard = new ProjectBindingWizard();
    projectBindingWizard.next();

    var serverProjectSelectionPage = new ProjectBindingWizard.ServerProjectSelectionPage(projectBindingWizard);
    serverProjectSelectionPage.waitForProjectsToBeFetched();
    serverProjectSelectionPage.setProjectKey(projectKey);
    projectBindingWizard.finish();
  }
}
