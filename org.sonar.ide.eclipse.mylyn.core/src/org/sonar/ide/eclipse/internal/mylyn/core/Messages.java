/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
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
package org.sonar.ide.eclipse.internal.mylyn.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

  private static final String BUNDLE_NAME = "org.sonar.ide.eclipse.internal.mylyn.core.messages"; //$NON-NLS-1$

  static {
    // initialize resource bundle
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  public static String SonarConnector_Label;

  public static String SonarConnector_Executing_query;

  public static String SonarTaskDataHandler_Downloading_task;
  public static String SonarTaskDataHandler_ConnectionException;

  public static String SonarTaskSchema_Completion_Label;

  public static String SonarTaskSchema_Created_Label;

  public static String SonarTaskSchema_ID_Label;

  public static String SonarTaskSchema_Kind_Label;

  public static String SonarTaskSchema_Modified_Label;

  public static String SonarTaskSchema_Owner_Label;

  public static String SonarTaskSchema_Priority_Label;

  public static String SonarTaskSchema_Reporter_Label;

  public static String SonarTaskSchema_Resource_Label;

  public static String SonarTaskSchema_Status_Label;

  public static String SonarTaskSchema_Resolution_Label;

  public static String SonarTaskSchema_Summary_Label;

  public static String SonarTaskSchema_URL_Label;

  public static String Workflow_Default_Label;
  public static String Workflow_ResolveAsFixed_Label;
  public static String Workflow_ResolveAsFalsePositive_Label;
  public static String Workflow_Reopen_Label;
  public static String Workflow_CommentRequired_Error;

}
