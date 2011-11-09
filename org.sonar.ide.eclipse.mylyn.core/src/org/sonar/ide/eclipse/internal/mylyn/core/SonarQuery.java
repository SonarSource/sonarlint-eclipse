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
package org.sonar.ide.eclipse.internal.mylyn.core;

import org.apache.commons.lang.StringUtils;
import org.eclipse.mylyn.tasks.core.IRepositoryQuery;

/**
 * Constants and utility methods to work with {@link IRepositoryQuery}.
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

  public static String[] getStatuses(IRepositoryQuery query) {
    return StringUtils.split(query.getAttribute(SonarQuery.STATUSES), ',');
  }

  public static String[] getSeverities(IRepositoryQuery query) {
    return StringUtils.split(query.getAttribute(SonarQuery.SEVERITIES), ',');
  }

}
