/*
 * Copyright © 2015 Siemens Medical Solutions USA, Inc.
 * All Rights Reserved.
 */
package org.sonar.ide.eclipse.ui.internal.wizards.associate;

import org.eclipse.osgi.util.NLS;

/**
 * TODO Insert description sentence here.
 *
 * @author z0035kms/Hemantkumar Chigadani
 */
public class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.sonar.ide.eclipse.ui.internal.wizards.associate.messages"; //$NON-NLS-1$
  public static String ConfigureProjectsPage_check_conn_settings;
  public static String ConfigureProjectsPage_description;
  public static String ConfigureProjectsPage_no_live_servers;
  public static String ConfigureProjectsPage_no_servers;
  public static String ConfigureProjectsPage_only_few_servers_live;
  public static String ConfigureProjectsPage_project;
  public static String ConfigureProjectsPage_sonarqube_project;
  public static String ConfigureProjectsPage_taskName;
  public static String ConfigureProjectsPage_title;
  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  private Messages() {
  }
}
