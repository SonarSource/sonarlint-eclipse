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
package org.sonar.ide.eclipse.internal.mylyn.ui.editor;

import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.*;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarConnector;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarTaskSchema;

import java.util.Iterator;
import java.util.Set;

public class SonarTaskEditorPage extends AbstractTaskEditorPage {

  public SonarTaskEditorPage(TaskEditor editor) {
    super(editor, SonarConnector.CONNECTOR_KIND);
    setNeedsPrivateSection(true);
    setNeedsSubmitButton(true);
  }

  @Override
  protected Set<TaskEditorPartDescriptor> createPartDescriptors() {
    Set<TaskEditorPartDescriptor> descriptors = super.createPartDescriptors();
    // remove unnecessary default editor parts
    for (Iterator<TaskEditorPartDescriptor> it = descriptors.iterator(); it.hasNext();) {
      TaskEditorPartDescriptor descriptor = it.next();
      if (PATH_PEOPLE.equals(descriptor.getPath())) {
        it.remove();
      }
    }
    return descriptors;
  }

  @Override
  protected AttributeEditorFactory createAttributeEditorFactory() {
    AttributeEditorFactory factory = new AttributeEditorFactory(getModel(), getTaskRepository(), getEditorSite()) {
      @Override
      public AbstractAttributeEditor createEditor(String type, TaskAttribute taskAttribute) {
        if (SonarTaskSchema.getDefault().RESOURCE.getKey().equals(taskAttribute.getId())) {
          return new ResourceAttributeEditor(getModel(), taskAttribute);
        }
        return super.createEditor(type, taskAttribute);
      }
    };
    return factory;
  }
}
