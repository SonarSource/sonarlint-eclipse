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

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.sonar.ide.eclipse.core.internal.messages"; //$NON-NLS-1$

  static {
    // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }

  public static String AnalyseProjectJob_title;
  public static String AnalyseProjectJob_task_analyzing;
  public static String AnalyseProjectJob_task_analyzing_file;

  public static String No_matching_server_in_configuration_for_project;
  public static String No_matching_server_in_configuration;
}
