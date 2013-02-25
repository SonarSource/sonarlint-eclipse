/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.sonar.ide.eclipse.ui.internal.messages"; //$NON-NLS-1$

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

  public static String ServerLocationWizardPage_label_host;
  public static String ServerLocationWizardPage_label_username;
  public static String ServerLocationWizardPage_label_password;
  public static String ServerLocationWizardPage_action_test;
  public static String ServerLocationWizardPage_action_test_tooltip;
  public static String ServerLocationWizardPage_msg_connected;
  public static String ServerLocationWizardPage_msg_error;
  public static String ServerLocationWizardPage_msg_connection_error;
  public static String ServerLocationWizardPage_msg_authentication_error;

  public static String SonarProjectPropertyBlock_label_host;
  public static String SonarProjectPropertyBlock_label_key;
  public static String SonarProjectPropertyBlock_label_language;
  public static String SonarProjectPropertyBlock_label_analysis_date;

  public static String SonarServerPreferencePage_title;
  public static String SonarServerPreferencePage_description;
  public static String SonarServerPreferencePage_action_add;
  public static String SonarServerPreferencePage_action_add_tooltip;
  public static String SonarServerPreferencePage_action_edit;
  public static String SonarServerPreferencePage_action_edit_tooltip;
  public static String SonarServerPreferencePage_action_delete;
  public static String SonarServerPreferencePage_action_delete_tooltip;

  public static String SonarPreferencePage_title;
  public static String SonarPreferencePage_description;
  public static String SonarPreferencePage_label_marker_severity;
  public static String SonarPreferencePage_label_new_violations_marker_severity;

  public static String SonarDebugOutputAction_tooltip;

  public static String SonarShowConsoleAction_tooltip;
  public static String SonarShowConsoleAction_never_text;
  public static String SonarShowConsoleAction_onOutput_text;
  public static String SonarShowConsoleAction_onError_text;

  public static String IgnoreMarkerResolver_label;
  public static String IgnoreMarkerResolver_description;

}
