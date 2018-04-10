/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2018 SonarSource SA
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

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.container.Server;
import org.eclipse.swtbot.eclipse.finder.widgets.SWTBotView;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.BoolResult;
import org.eclipse.swtbot.swt.finder.waits.DefaultCondition;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.wsclient.services.PropertyCreateQuery;
import org.sonarlint.eclipse.its.bots.ServerConnectionWizardBot;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.setting.SetRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class ConnectedModeTest extends AbstractSonarLintTest {

  @ClassRule
  public static TemporaryFolder temp = new TemporaryFolder();

  @ClassRule
  public static Orchestrator orchestrator = Orchestrator.builderEnv()
    // Need at least one plugin to avoid bug SONAR-8918
    .setOrchestratorProperty("javaVersion", "LATEST_RELEASE")
    .addPlugin("java")
    .build();

  private static WsClient adminWsClient;

  @BeforeClass
  public static void prepare() {
    adminWsClient = newAdminWsClient(orchestrator);
    if (orchestrator.getServer().version().isGreaterThanOrEquals("6.3")) {
      adminWsClient.settingsService().set(SetRequest.builder().setKey("sonar.forceAuthentication").setValue("true").build());
    } else {
      orchestrator.getServer().getAdminWsClient().create(new PropertyCreateQuery("sonar.forceAuthentication", "true"));
    }
  }

  @Test
  public void configureServerFromNewWizard() {
    ServerConnectionWizardBot wizardBot = new ServerConnectionWizardBot(bot);
    wizardBot.openFromFileNewWizard();

    wizardBot.assertTitle("Connect to a SonarQube Server");

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
    if (orchestrator.getServer().version().isGreaterThanOrEquals("6.0")) {
      wizardBot.assertErrorMessage("Authentication failed");
    } else {
      wizardBot.assertErrorMessage("Not authorized. Please check server credentials.");
    }

    wizardBot.setPassword(Server.ADMIN_PASSWORD);
    wizardBot.clickNext();

    assertThat(wizardBot.getConnectionName()).isEqualTo("127.0.0.1");
    assertThat(wizardBot.isNextEnabled()).isTrue();

    wizardBot.setConnectionName("");
    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.assertErrorMessage("Connection name must be specified");

    wizardBot.setConnectionName("test");
    wizardBot.clickNext();

    assertThat(wizardBot.isNextEnabled()).isFalse();
    wizardBot.clickFinish();

    SWTBotView serversView = bot.viewById("org.sonarlint.eclipse.ui.ServersView");
    final SWTBotTreeItem serverCell = serversView.bot().tree().getAllItems()[0];
    bot.waitUntil(new DefaultCondition() {
      @Override
      public boolean test() throws Exception {
        return UIThreadRunnable.syncExec(new BoolResult() {
          @Override
          public Boolean run() {
            return serverCell.getText().matches("test \\[Version: " + orchestrator.getServer().version() + "(.*), Last update: (.*)\\]");
          }
        });
      };
      
      @Override
      public String getFailureMessage() {
        return "Server status is: " + serverCell.getText();
      }
    }, 20_000);
  }

}
