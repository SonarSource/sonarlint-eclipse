/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * sonarqube@googlegroups.com
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
package org.sonar.ide.eclipse.core.internal;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;

public class PreferencesUtils {

  public static final String PREF_MARKER_SEVERITY = "markerSeverity"; //$NON-NLS-1$
  public static final int PREF_MARKER_SEVERITY_DEFAULT = IMarker.SEVERITY_WARNING;
  public static final String PREF_NEW_ISSUE_MARKER_SEVERITY = "newViolationMarkerSeverity"; //$NON-NLS-1$
  public static final int PREF_NEW_ISSUE_MARKER_SEVERITY_DEFAULT = IMarker.SEVERITY_ERROR;
  public static final String PREF_EXTRA_ARGS = "extraArgs"; //$NON-NLS-1$
  public static final String PREF_EXTRA_ARGS_DEFAULT = ""; //$NON-NLS-1$
  public static final String PREF_JVM_ARGS = "jvmArgs"; //$NON-NLS-1$
  public static final String PREF_JVM_ARGS_DEFAULT = ""; //$NON-NLS-1$
  public static final String PREF_FORCE_FULL_PREVIEW = "fullPreview"; //$NON-NLS-1$
  public static final boolean PREF_FORCE_FULL_PREVIEW_DEFAULT = false; // $NON-NLS-1$
  public static final String PREF_TEST_FILE_REGEXPS = "testFileRegexps"; //$NON-NLS-1$
  public static final String PREF_TEST_FILE_REGEXPS_DEFAULT = "**/*Test.*,**/test/**/*"; //$NON-NLS-1$

  public static String getTestFileRegexps() {
    return Platform.getPreferencesService().getString(SonarCorePlugin.UI_PLUGIN_ID, PREF_TEST_FILE_REGEXPS, PREF_TEST_FILE_REGEXPS_DEFAULT, null);
  }

  public static int getMarkerSeverity() {
    return Platform.getPreferencesService().getInt(SonarCorePlugin.UI_PLUGIN_ID, PREF_MARKER_SEVERITY, PREF_MARKER_SEVERITY_DEFAULT, null);
  }

  public static int getMarkerSeverityNewIssues() {
    return Platform.getPreferencesService().getInt(SonarCorePlugin.UI_PLUGIN_ID, PREF_NEW_ISSUE_MARKER_SEVERITY, PREF_NEW_ISSUE_MARKER_SEVERITY_DEFAULT, null);
  }

  public static List<SonarProperty> getExtraPropertiesForLocalAnalysis(IProject project) {
    List<SonarProperty> props = new ArrayList<SonarProperty>();
    // First add all global properties
    String globalExtraArgs = Platform.getPreferencesService().getString(SonarCorePlugin.UI_PLUGIN_ID, PREF_EXTRA_ARGS, PREF_EXTRA_ARGS_DEFAULT, null);
    String[] keyValuePairs = StringUtils.split(globalExtraArgs, "\n\r");
    for (String keyValuePair : keyValuePairs) {
      String[] keyValue = keyValuePair.split("=");
      props.add(new SonarProperty(keyValue[0], keyValue[1]));
    }

    // Then add project properties
    SonarProject sonarProject = SonarProject.getInstance(project);
    if (sonarProject.getExtraProperties() != null) {
      props.addAll(sonarProject.getExtraProperties());
    }

    return props;
  }

  public static String getSonarJvmArgs() {
    return Platform.getPreferencesService().getString(SonarCorePlugin.UI_PLUGIN_ID, PREF_JVM_ARGS, PREF_JVM_ARGS_DEFAULT, null);
  }

  public static boolean isForceFullPreview() {
    return Platform.getPreferencesService().getBoolean(SonarCorePlugin.UI_PLUGIN_ID, PREF_FORCE_FULL_PREVIEW, PREF_FORCE_FULL_PREVIEW_DEFAULT, null);
  }

  private PreferencesUtils() {
    // Utility class
  }
}
