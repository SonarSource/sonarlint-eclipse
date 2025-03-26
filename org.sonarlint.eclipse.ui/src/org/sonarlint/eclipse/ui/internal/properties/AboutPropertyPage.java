/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.properties;

import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;

public class AboutPropertyPage extends PropertyPage implements IWorkbenchPreferencePage {
  public static final String ABOUT_CONFIGURATION_ID = "org.sonarlint.eclipse.ui.properties.AboutPropertyPage";

  private Button enabledBtn;

  public AboutPropertyPage() {
    setTitle("Miscellaneous");
  }

  @Override
  public void init(IWorkbench workbench) {
    setDescription("Statistics");
    setPreferenceStore(SonarLintUiPlugin.getDefault().getPreferenceStore());
  }

  @Override
  protected Control createContents(final Composite parent) {

    var composite = new Composite(parent, SWT.NONE);
    var gridLayout = new GridLayout(2, false);
    gridLayout.marginWidth = 0;
    gridLayout.marginHeight = 0;
    composite.setLayout(gridLayout);

    var text = new Link(composite, SWT.NONE);
    var textGd = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
    text.setLayoutData(textGd);
    text.setText("By sharing anonymous SonarQube for Eclipse usage statistics, you help us understand how it is used so "
      + "we can improve the plugin to work even better for you.\nWe don't collect source code, IP addresses, or any personally identifying "
      + "information. And we don't share the data with anyone else.\nSee a <a href=\"#\">sample of the data.</a>");
    
    var sampleDataGd = new GridData(SWT.BEGINNING, SWT.CENTER, false, false);
    var sampleDataLink = new Link(composite, SWT.UNDERLINE_LINK);
    sampleDataLink.setForeground(sampleDataLink.getDisplay().getSystemColor(SWT.COLOR_BLUE));
    sampleDataLink.setLayoutData(sampleDataGd);
    sampleDataLink.setText("See a sample of data");
    var handCursor = new Cursor(sampleDataLink.getDisplay(), SWT.CURSOR_HAND);
    sampleDataLink.addListener(SWT.MouseEnter, event -> sampleDataLink.setCursor(handCursor));
    sampleDataLink.addListener(SWT.MouseExit, event -> sampleDataLink.setCursor(null));
     
    final var tip = new DefaultToolTip(sampleDataLink, ToolTip.RECREATE, false);
    tip.setText("{\n"
      + "    \"days_since_installation\": 27,\n"
      + "    \"days_of_use\": 5,\n"
      + "    \"sonarlint_version\": \"11.3.0\",\n"
      + "    \"sonarlint_product\": \"SonarLint Eclipse\",\n"
      + "    \"ide_version\": \"Eclipse IDE 4.12.0.v20190605-1800\",\n"
      + "    \"connected_mode_used\": true,\n"
      + "    \"connected_mode_sonarcloud\": true,\n"
      + "    \"system_time\":\"2025-03-26T11:45:44.513+01:00\",\n"
      + "    \"install_time\":\"2024-01-12T10:49:17.196+01:00\",\n"
      + "    \"os\": \"Linux\",\n"
      + "    \"jre\": \"17.0.9\",\n" +
      "      \"nodejs\": \"20.16.0\",\n" +
      "      \"analyses\":[{\"language\":\"java\",\"rate_per_duration\":{\"0-300\":100,\"300-500\":0,\"500-1000\":0,\"1000-2000\":0,\"2000-4000\":0,\"4000+\":0}}]\n," +
      "      \"server_notifications\": {\n" +
      "            \"disabled\":false,\n" +
      "            \"count_by_type\":{\n" +
      "                  \"NEW_ISSUES\": {\"received\":1,\"clicked\":1},\n" +
      "                  \"QUALITY_GATE\": {\"received\":1,\"clicked\":0}\n" +
      "            }\n" +
      "      },\n" +
      "      \"show_hotspot\": {\n" +
      "            \"requests_count\": 3\n" +
      "      },\n" +
      "      \"taint_vulnerabilities\": {\n" +
      "            \"investigated_locally_count\": 3,\n" +
      "            \"investigated_remotely_count\": 4\n" +
      "      },\n" +
      "      \"rules\": {\n" +
      "            \"raised_issues\": [\n" +
      "                \"secrets:S6290\",\n" +
      "                \"javascript:S3353\",\n" +
      "                \"javascript:S1441\"\n" +
      "            ],\n" +
      "            \"non_default_enabled\": [\n" +
      "                \"javascript:S3513\"\n" +
      "            ],\n" +
      "            \"default_disabled\":  [\n" +
      "                \"javascript:S1994\"\n" +
      "            ],\n" +
      "            \"quick_fix_applied\": [\n" +
      "                \"java:S1656\",\n" +
      "                \"java:S1872\"\n" +
      "            ],\n" +
      "      }\n" +
      "      \"hotspot\": {\n" +
      "            \"open_in_browser_count\": 0,\n" +
      "            \"status_changed_count\": 0\n" +
      "      },\n" +
      "      \"issue\": {\n" +
      "            \"status_changed_rule_keys\": [],\n" +
      "            \"status_changed_count\": 5\n" +
      "      },\n" +
      "      \"help_and_feedback\": {\n" +
      "            \"count_by_link\": {},\n" +
      "      },\n" +
      "      \"cayc\": {\n" +
      "            \"new_code_focus\": {\n" +
      "                   \"enabled\": true\n" +
      "                   \"changes\": 1\n" +
      "            },\n" +
      "      },\n" +
      "      \"shared_connected_mode\": {\n" +
      "            \"manual_bindings_count\": 1,\n" +
      "            \"imported_bindings_count\": 0\n" +
      "            \"auto_bindings_count\": 1\n" +
      "            \"exported_connected_mode_count\": 1\n" +
      "      },\n" +
      "}");

    enabledBtn = new Button(composite, SWT.CHECK);
    enabledBtn.setText("Share anonymous SonarQube for Eclipse statistics");
    enabledBtn.setSelection(SonarLintTelemetry.isEnabled());
    var layoutData = new GridData();
    layoutData.horizontalSpan = 2;
    enabledBtn.setLayoutData(layoutData);

    /** Information on SonarLint for Eclipse user surveys */
    var surveyHeader = new Link(composite, SWT.NONE);
    surveyHeader.setLayoutData(textGd);
    surveyHeader.setText("\nSonarQube for Eclipse user survey");
    var surveyText = new Link(composite, SWT.NONE);
    surveyText.setLayoutData(textGd);
    surveyText.setText("From time to time we might provide you with the link to a user survey as we are interested in "
      + "your feedback to improve SonarQube for\nEclipse. It will pop-up for you on IDE startup and you are free to "
      + "check it out, but there is no obligation to take part in the survey.\nYou can come back here anytime to find "
      + "the link to the survey once again, perhaps if you changed your mind and want to take part.");
    var link = SonarLintGlobalConfiguration.getUserSurveyLastLink();
    if (!link.isBlank()) {
      var surveyLink = new Link(composite, SWT.NONE);
      surveyLink.setText("To access the current survey, <a>click here</a>. Be cautious, the survey might already be "
        + "closed and therefore unavailable!");
      surveyLink.addListener(SWT.Selection, e -> BrowserUtils.openExternalBrowser(link, e.display));
    }

    return composite;
  }

  @Override
  public boolean performOk() {
    SonarLintTelemetry.optOut(!enabledBtn.getSelection());
    return true;
  }

  @Override
  protected void performDefaults() {
    enabledBtn.setSelection(true);
  }
}
