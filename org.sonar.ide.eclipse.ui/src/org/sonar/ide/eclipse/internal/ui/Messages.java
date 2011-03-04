/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.ide.eclipse.internal.ui;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.sonar.ide.eclipse.internal.ui.messages"; //$NON-NLS-1$

  static {
    // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }

  public static String SonarConsole_title;

  public static String SonarProjectPropertyPage_title;

  public static String ViolationsView_action_refresh;
  public static String ViolationsView_action_refresh_tooltip;

  public static String ConfigureProjectsWizard_action_autoconfig;

  public static String ServerLocationWizardPage_label_host;
  public static String ServerLocationWizardPage_label_username;
  public static String ServerLocationWizardPage_label_password;
  public static String ServerLocationWizardPage_action_test;
  public static String ServerLocationWizardPage_action_test_tooltip;
  public static String ServerLocationWizardPage_msg_connected;
  public static String ServerLocationWizardPage_msg_error;

  public static String SonarProjectPropertyBlock_label_host;
  public static String SonarProjectPropertyBlock_label_groupId;
  public static String SonarProjectPropertyBlock_label_artifactId;
  public static String SonarProjectPropertyBlock_label_branch;
  public static String SonarProjectPropertyBlock_action_server;

  public static String SonarPreferencePage_title;
  public static String SonarPreferencePage_description;
  public static String SonarPreferencePage_action_add;
  public static String SonarPreferencePage_action_add_tooltip;
  public static String SonarPreferencePage_action_edit;
  public static String SonarPreferencePage_action_edit_tooltip;
  public static String SonarPreferencePage_action_delete;
  public static String SonarPreferencePage_action_delete_tooltip;

  public static String IgnoreMarkerResolver_label;
  public static String IgnoreMarkerResolver_description;

}
