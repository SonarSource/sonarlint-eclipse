/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
package org.sonar.ide.eclipse.internal.mylyn.core;

import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

public class SonarTaskSchema extends AbstractTaskSchema {
  private static final SonarTaskSchema INSTANCE = new SonarTaskSchema();

  public static SonarTaskSchema getDefault() {
    return INSTANCE;
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

  public final Field STATUS = createField(TaskAttribute.STATUS, Messages.SonarTaskSchema_Status_Label,
      TaskAttribute.TYPE_SHORT_TEXT);

  /**
   * Possible values: FIXED, FALSE-POSITIVE.
   * 
   * @since Sonar 2.9
   */
  public final Field RESOLUTION = createField(TaskAttribute.RESOLUTION, Messages.SonarTaskSchema_Resolution_Label,
      TaskAttribute.TYPE_SHORT_TEXT);

  /**
   * Possible values: BLOCKER, CRITICAL, MAJOR, MINOR, INFO.
   */
  public final Field PRIORITY = createField(TaskAttribute.PRIORITY, Messages.SonarTaskSchema_Priority_Label,
      TaskAttribute.TYPE_SINGLE_SELECT);

  public final Field USER_REPORTER = createField(TaskAttribute.USER_REPORTER, Messages.SonarTaskSchema_Reporter_Label,
      TaskAttribute.TYPE_PERSON);

  public final Field USER_ASSIGNED = createField(TaskAttribute.USER_ASSIGNED, Messages.SonarTaskSchema_Owner_Label,
      TaskAttribute.TYPE_PERSON);

  public final Field DATE_CREATION = createField(TaskAttribute.DATE_CREATION, Messages.SonarTaskSchema_Created_Label,
      TaskAttribute.TYPE_DATE);

  public final Field DATE_MODIFICATION = createField(TaskAttribute.DATE_MODIFICATION, Messages.SonarTaskSchema_Modified_Label,
      TaskAttribute.TYPE_DATE);

  public final Field DATE_COMPLETION = createField(TaskAttribute.DATE_COMPLETION, Messages.SonarTaskSchema_Completion_Label,
      TaskAttribute.TYPE_DATE);

  public final Field TASK_KIND = createField(TaskAttribute.TASK_KIND, Messages.SonarTaskSchema_Kind_Label,
      TaskAttribute.TYPE_SINGLE_SELECT, TaskAttribute.KIND_DEFAULT);

  public final Field RESOURCE = createField("sonarResource", Messages.SonarTaskSchema_Resource_Label, //$NON-NLS-1$
      TaskAttribute.TYPE_LONG_TEXT, TaskAttribute.KIND_DEFAULT);

  public final Field LINE = createField("sonarLine", "", TaskAttribute.TYPE_INTEGER); //$NON-NLS-1$ //$NON-NLS-2$

  public final Field VIOLATION_ID = createField("violationId", "", TaskAttribute.TYPE_LONG); //$NON-NLS-1$ //$NON-NLS-2$

}
