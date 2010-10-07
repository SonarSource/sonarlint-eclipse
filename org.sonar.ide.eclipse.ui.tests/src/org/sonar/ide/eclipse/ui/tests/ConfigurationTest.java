/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.ui.tests;

import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.waits.Conditions;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class ConfigurationTest extends UITestCase {

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

  private static void closeWizard(SWTBotShell wizard) {
    wizard.activate();
    bot.button("Finish").click();
    bot.waitUntil(Conditions.shellCloses(wizard));
  }

  @Test
  public void test() throws Exception {
    assertThat(table.rowCount(), is(0));

    assertThat(table.selectionCount(), is(0));
    assertThat("Add button enabled", bot.button("Add...").isEnabled(), is(true));
    assertThat("Edit button enabled", bot.button("Edit...").isEnabled(), is(false));
    assertThat("Remove button enabled", bot.button("Remove").isEnabled(), is(false));

    // test add
    bot.button("Add...").click();
    bot.waitUntil(Conditions.shellIsActive("Add Sonar Server"));
    SWTBotShell wizard = bot.shell("Add Sonar Server");
    wizard.activate();

    testConnection(getSonarServerUrl() + "/", true); // test for SONARIDE-90
    testConnection(getSonarServerUrl(), true);
    testConnection("http://fake", false);
    closeWizard(wizard);

    assertThat(table.rowCount(), is(1));
    assertThat(table.containsItem("http://fake"), is(true));

    assertThat(table.selectionCount(), is(0));
    assertThat("Add button enabled", bot.button("Add...").isEnabled(), is(true));
    assertThat("Edit button enabled", bot.button("Edit...").isEnabled(), is(false));
    assertThat("Remove button enabled", bot.button("Remove").isEnabled(), is(false));

    // test edit
    table.getTableItem(0).select();
    assertThat("Edit button enabled", bot.button("Edit...").isEnabled(), is(true));
    bot.button("Edit...").click();
    bot.waitUntil(Conditions.shellIsActive("Edit Sonar Server"));
    wizard = bot.shell("Edit Sonar Server");
    wizard.activate();
    assertThat(bot.textWithLabel("Sonar server URL :").getText(), is("http://fake"));
    assertThat(bot.textWithLabel("Username :").getText(), is(""));
    assertThat(bot.textWithLabel("Password :").getText(), is(""));
    bot.textWithLabel("Sonar server URL :").setText("http://fake2");
    closeWizard(wizard);

    assertThat(table.rowCount(), is(1));
    assertThat(table.containsItem("http://fake2"), is(true));

    assertThat(table.selectionCount(), is(0));
    assertThat("Add button enabled", bot.button("Add...").isEnabled(), is(true));
    assertThat("Edit button enabled", bot.button("Edit...").isEnabled(), is(false));
    assertThat("Remove button enabled", bot.button("Remove").isEnabled(), is(false));

    // test remove
    table.getTableItem("http://fake2").select();
    assertThat("Remove button enabled", bot.button("Remove").isEnabled(), is(true));
    bot.button("Remove").click();
    bot.waitUntil(Conditions.shellIsActive("Remove sonar server connection"));
    bot.button("OK").click();

    assertThat(table.rowCount(), is(0));

    assertThat("Add button enabled", bot.button("Add...").isEnabled(), is(true));
    assertThat("Edit button enabled", bot.button("Edit...").isEnabled(), is(false));
    assertThat("Remove button enabled", bot.button("Remove").isEnabled(), is(false));
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
