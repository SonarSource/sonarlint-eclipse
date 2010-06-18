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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTableItem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * TODO test remove
 * 
 * @author Evgeny Mandrikov
 */
public class ConfigurationTest extends UITestCase {

  private static final String DEFAULT_HOST = "http://localhost:9000";
  private static SWTBotShell shell;
  private static SWTBotTable table;

  @BeforeClass
  public static void openProperties() throws Exception {
    shell = showGlobalSonarPropertiesPage();
    table = bot.table();
  }

  @AfterClass
  public static void closeProperties() {
    shell.bot().button("OK").click();
    bot.waitUntil(Conditions.shellCloses(shell));
  }

  @Test
  public void defaultServerDefined() throws Exception {
    // assertThat(bot.button("Edit...").isEnabled(), is(false));
    // assertThat(bot.button("Remove").isEnabled(), is(false));

    assertThat(table.rowCount(), greaterThanOrEqualTo(1));

    SWTBotTableItem item = table.getTableItem(DEFAULT_HOST);
    assertThat(item.isChecked(), is(true));

    // item.click();
    // assertThat(bot.button("Edit...").isEnabled(), is(true));
    // assertThat(bot.button("Remove").isEnabled(), is(true));
  }

  private void select(String host) {
    SWTBotTableItem item = table.getTableItem(host);
    item.select();
  }

  private static void closeWizard(SWTBotShell wizard) {
    wizard.activate();
    bot.button("Cancel").click();
    bot.waitUntil(Conditions.shellCloses(wizard));
  }

  @Test
  public void editWorks() throws Exception {
    select(DEFAULT_HOST);
    assertThat(bot.button("Edit...").isEnabled(), is(true));
    bot.button("Edit...").click();
    bot.waitUntil(Conditions.shellIsActive("Edit Sonar server connection"));
    SWTBotShell shell2 = bot.shell("Edit Sonar server connection");
    shell2.activate();
    assertThat(bot.textWithLabel("Sonar server URL :").getText(), is(DEFAULT_HOST));
    assertThat(bot.textWithLabel("Username :").getText(), is(""));
    assertThat(bot.textWithLabel("Password :").getText(), is(""));
    closeWizard(shell2);
  }

  @Test
  public void addWorks() throws Exception {
    bot.button("Add...").click();
    bot.waitUntil(Conditions.shellIsActive("New Sonar server connection"));
    SWTBotShell shell2 = bot.shell("New Sonar server connection");
    shell2.activate();
    testConnection("http://fake", false);
    testConnection(getSonarServerUrl() + "/", true); // test for SONARIDE-90
    testConnection(getSonarServerUrl(), true);
    closeWizard(shell2);
  }

  private void testConnection(String serverUrl, boolean expectedSuccess) {
    bot.textWithLabel("Sonar server URL :").setText(serverUrl);
    SWTBotButton button = bot.button("Test connection");
    button.click();
    bot.waitUntil(Conditions.widgetIsEnabled(button), 1000 * 30);

    String message = expectedSuccess ? "Successfully connected!" : "Unable to connect.";
    try {
      bot.text(" " + message);
    } catch (WidgetNotFoundException e) {
      fail("Expected '" + message + "'");
    }
  }
}
