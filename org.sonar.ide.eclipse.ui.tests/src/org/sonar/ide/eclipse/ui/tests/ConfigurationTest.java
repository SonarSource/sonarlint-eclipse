package org.sonar.ide.eclipse.ui.tests;

import static org.junit.Assert.fail;

import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
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

    testConnection("http://fake", false);
    testConnection(server.getBaseUrl() + "/", true); // test for SONARIDE-90
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
    SWTBotButton button = bot.button("Test connection");
    button.click();
    bot.waitUntil(Conditions.widgetIsEnabled(button));

    String message = expectedSuccess ? "Successfully connected!" : "Unable to connect.";
    try {
      bot.text(" " + message);
    } catch (WidgetNotFoundException e) {
      fail("Expected '" + message + "'");
    }
  }
}
