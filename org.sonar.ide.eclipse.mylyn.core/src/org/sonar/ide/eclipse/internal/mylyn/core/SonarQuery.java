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

import org.apache.commons.lang.StringUtils;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;

/**
 * Constants and utility methods to work with {@link IRepositoryQuery}.
 * 
 * @since 2.3
 */
public final class SonarQuery {

  private SonarQuery() {
  }

  public static final String PROJECT = "project"; //$NON-NLS-1$

  public static final String REPORTER = "reporter"; //$NON-NLS-1$
  public static final String REPORTER_USER = "reporter_user"; //$NON-NLS-1$

  public static final String ASSIGNEE = "assignee"; //$NON-NLS-1$
  public static final String ASSIGNEE_USER = "assignee_user"; //$NON-NLS-1$

  public static final String STATUSES = "statuses"; //$NON-NLS-1$

  public static final String SEVERITIES = "severities"; //$NON-NLS-1$

  public static final String ANY_USER = "Any"; //$NON-NLS-1$
  public static final String CURRENT_USER = "Current user"; //$NON-NLS-1$
  public static final String SPECIFIED_USER = "Specified user"; //$NON-NLS-1$

  public static String[] getStatuses(IRepositoryQuery query) {
    return StringUtils.split(query.getAttribute(SonarQuery.STATUSES), ',');
  }

  public static String[] getSeverities(IRepositoryQuery query) {
    return StringUtils.split(query.getAttribute(SonarQuery.SEVERITIES), ',');
  }

  public static String[] getReporter(IRepositoryQuery query, String currentUser) {
    return getUser(query.getAttribute(REPORTER), currentUser, query.getAttribute(REPORTER_USER));
  }

  public static String[] getAssignee(IRepositoryQuery query, String currentUser) {
    return getUser(query.getAttribute(ASSIGNEE), currentUser, query.getAttribute(ASSIGNEE_USER));
  }

  private static String[] getUser(String type, String currentUser, String specifiedUser) {
    if (ANY_USER.equalsIgnoreCase(type)) {
      return null;
    } else if (CURRENT_USER.equalsIgnoreCase(type)) {
      return new String[] { currentUser };
    } else if (SPECIFIED_USER.equalsIgnoreCase(type)) {
      return new String[] { specifiedUser };
    } else {
      throw new IllegalStateException();
    }
  }

}
