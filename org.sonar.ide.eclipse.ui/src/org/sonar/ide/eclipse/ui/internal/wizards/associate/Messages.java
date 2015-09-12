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
package org.sonar.ide.eclipse.ui.internal.wizards.associate;

import org.eclipse.osgi.util.NLS;

/**
 * Messages.
 *
 * @author Hemantkumar Chigadani
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.sonar.ide.eclipse.ui.internal.wizards.associate.messages"; //$NON-NLS-1$
  public static String ConfigureProjectsPage_description;
  public static String ConfigureProjectsPage_one_of_servers_not_reachable;
  public static String ConfigureProjectsPage_no_servers;
  public static String ConfigureProjectsPage_project;
  public static String ConfigureProjectsPage_sonar_project;
  public static String ConfigureProjectsPage_taskname;
  public static String ConfigureProjectsPage_title;
  public static String ConfigureProjectsPage_check_conn_settings;
  public static String ConfigureProjectsPage_only_few_servers_live;
  public static String ConfigureProjectsPage_no_live_servers;

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
