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

import org.eclipse.mylyn.tasks.core.data.AbstractTaskSchema;
import org.eclipse.mylyn.tasks.core.data.DefaultTaskSchema;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;

public class SonarTaskSchema extends AbstractTaskSchema {
  private static final SonarTaskSchema instance = new SonarTaskSchema();

  public static SonarTaskSchema getDefault() {
    return instance;
  }

  private final DefaultTaskSchema parent = DefaultTaskSchema.getInstance();

  public final Field KEY = inheritFrom(parent.TASK_KEY).create();

  public final Field SUMMARY = inheritFrom(parent.SUMMARY).flags(Flag.READ_ONLY).create();

  public final Field DESCRIPTION = inheritFrom(parent.DESCRIPTION).flags(Flag.READ_ONLY).create();

  public final Field OWNER = inheritFrom(parent.USER_ASSIGNED).flags(Flag.READ_ONLY, Flag.ATTRIBUTE).create();

  public final Field CREATOR = inheritFrom(parent.USER_REPORTER).create();

  public final Field CREATED = inheritFrom(parent.DATE_CREATION).create();

  public final Field RULE = createField("org.sonar.custom", "Rule", TaskAttribute.TYPE_SHORT_TEXT, Flag.READ_ONLY, Flag.ATTRIBUTE);

  public final Field STATUS = inheritFrom(parent.STATUS).create();

  public final Field TASK_URL = inheritFrom(parent.TASK_URL).create();

  public final Field PRIORITY = inheritFrom(parent.PRIORITY).flags(Flag.READ_ONLY).create();

  public final Field DATE_COMPLETION = inheritFrom(parent.DATE_COMPLETION).create();

}
