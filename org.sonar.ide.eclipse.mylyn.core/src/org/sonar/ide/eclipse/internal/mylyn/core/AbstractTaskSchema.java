/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.core.data.TaskAttributeMetaData;
import org.eclipse.mylyn.tasks.core.data.TaskData;

import java.util.ArrayList;
import java.util.List;

/**
 * This class partially duplicates class with same name from Mylyn 3.5. This is a workaround to work with lower versions.
 */
public class AbstractTaskSchema {

  public static class Field {
    private final String key;
    private final String label;
    private final String type;
    private final String kind;

    protected Field(String key, String label, String type, String kind) {
      Assert.isNotNull(key);
      Assert.isNotNull(label);
      Assert.isNotNull(type);
      this.key = key;
      this.label = label;
      this.type = type;
      this.kind = kind;
    }

    public TaskAttribute createAttribute(TaskAttribute parent) {
      TaskAttribute attribute = parent.createMappedAttribute(getKey());
      // meta data
      TaskAttributeMetaData metaData = attribute.getMetaData();
      metaData.setLabel(getLabel());
      metaData.setType(getType());
      metaData.setReadOnly(isReadOnly());
      metaData.setKind(getKind());
      return attribute;
    }

    public String getKey() {
      return key;
    }

    public String getLabel() {
      return label;
    }

    public String getType() {
      return type;
    }

    private boolean isReadOnly() {
      return true;
    }

    public String getKind() {
      return kind;
    }
  }

  private final List<Field> fields = new ArrayList<Field>();

  public void initialize(TaskData taskData) {
    for (Field field : fields) {
      field.createAttribute(taskData.getRoot());
    }
  }

  protected Field createField(String key, String label, String type, String kind) {
    Field field = new Field(key, label, type, kind);
    fields.add(field);
    return field;
  }

  protected Field createField(String key, String label, String type) {
    return createField(key, label, type, null);
  }

}
