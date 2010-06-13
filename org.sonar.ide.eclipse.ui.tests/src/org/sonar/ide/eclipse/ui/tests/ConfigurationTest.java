/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

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
