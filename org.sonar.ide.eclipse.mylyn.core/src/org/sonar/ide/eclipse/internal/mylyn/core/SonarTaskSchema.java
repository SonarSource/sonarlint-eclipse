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

import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

/**
 * Everything should be read-only for the moment - see http://jira.codehaus.org/browse/SONARIDE-228
 */
public class SonarTaskSchema extends AbstractTaskSchema {
  private static final SonarTaskSchema instance = new SonarTaskSchema();

  public static SonarTaskSchema getDefault() {
    return instance;
  }

  public final Field ID = createField(TaskAttribute.TASK_KEY, Messages.SonarTaskSchema_ID_Label,
      TaskAttribute.TYPE_SHORT_TEXT);

  /**
   * Permalink, for example: http://nemo.sonarsource.org/reviews/view/1
   */
  public final Field URL = createField(TaskAttribute.TASK_URL, Messages.SonarTaskSchema_URL_Label,
      TaskAttribute.TYPE_URL);

  public final Field SUMMARY = createField(TaskAttribute.SUMMARY, Messages.SonarTaskSchema_Summary_Label,
      TaskAttribute.TYPE_SHORT_RICH_TEXT);

  public final Field DESCRIPTION = createField(TaskAttribute.DESCRIPTION, Messages.SonarTaskSchema_Description_Label,
      TaskAttribute.TYPE_LONG_RICH_TEXT);

  public final Field STATUS = createField(TaskAttribute.STATUS, Messages.SonarTaskSchema_Status_Label,
      TaskAttribute.TYPE_SHORT_TEXT);

  /**
   * Possible values: BLOCKER, CRITICAL, MAJOR, MINOR, INFO.
   */
  public final Field PRIORITY = createField(TaskAttribute.PRIORITY, Messages.SonarTaskSchema_Priority_Label,
      TaskAttribute.TYPE_SINGLE_SELECT);

  public final Field USER_REPORTER = createField(TaskAttribute.USER_REPORTER, Messages.SonarTaskSchema_Reporter_Label,
      TaskAttribute.TYPE_PERSON, TaskAttribute.KIND_DEFAULT);

  public final Field USER_ASSIGNED = createField(TaskAttribute.USER_ASSIGNED, Messages.SonarTaskSchema_Owner_Label,
      TaskAttribute.TYPE_PERSON, TaskAttribute.KIND_DEFAULT);

  public final Field DATE_CREATION = createField(TaskAttribute.DATE_CREATION, Messages.SonarTaskSchema_Created_Label,
      TaskAttribute.TYPE_DATE);

  public final Field DATE_MODIFICATION = createField(TaskAttribute.DATE_MODIFICATION, Messages.SonarTaskSchema_Modified_Label,
      TaskAttribute.TYPE_DATE);

  public final Field DATE_COMPLETION = createField(TaskAttribute.DATE_COMPLETION, Messages.SonarTaskSchema_Completion_Label,
      TaskAttribute.TYPE_DATE);

  public final Field TASK_KIND = createField(TaskAttribute.TASK_KIND, Messages.SonarTaskSchema_Kind_Label,
      TaskAttribute.TYPE_SINGLE_SELECT, TaskAttribute.KIND_DEFAULT);

}
