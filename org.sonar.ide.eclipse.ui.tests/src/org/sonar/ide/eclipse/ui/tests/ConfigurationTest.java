package org.sonar.ide.eclipse.ui.tests;

import static org.junit.Assert.fail;

import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.Test;
import org.sonar.ide.test.SonarTestServer;

public class ConfigurationTest extends UITestCase {

  @Test
  public void test() throws Exception {
    SonarTestServer server = getTestServer();

    SWTBotShell shell = showGlobalSonarPropertiesPage();
    bot.button("Edit").click();

    bot.waitUntil(Conditions.shellIsActive("Edit sonar server connection"));
    SWTBotShell shell2 = bot.shell("Edit sonar server connection");
    shell2.activate();

    // TODO see SONARIDE-90
    testConnection("http://fake", false);
    testConnection(server.getBaseUrl() + "/", false); // TODO should be successful
    testConnection(server.getBaseUrl(), true);

    // Close wizard
    shell2.activate();
    bot.button("Finish").click();
    bot.waitUntil(Conditions.shellCloses(shell2));

    // Close properties
    shell.bot().button("OK").click();
    bot.waitUntil(Conditions.shellCloses(shell));
  }

  private void testConnection(String serverUrl, boolean expectedSuccess) {
    bot.textWithLabel("Sonar server URL :").setText(serverUrl);
    bot.button("Test connection").click();

    bot.waitUntil(Conditions.shellIsActive("Test sonar server connection"));
    SWTBotShell dialog = bot.shell("Test sonar server connection");
    dialog.activate();

    try {
      bot.label("Successfully connected!");
      if ( !expectedSuccess) {
        fail("Expected 'Unable to connect'");
      }
    } catch (WidgetNotFoundException e) {
      if (expectedSuccess) {
        fail("Expected 'Successfully connected'");
      }
    }

    bot.button("OK").click();
    bot.waitUntil(Conditions.shellCloses(dialog));
  }

}
