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
package org.sonar.ide.eclipse.internal.mylyn.ui.editor;

import org.eclipse.mylyn.tasks.core.data.TaskAttribute;
import org.eclipse.mylyn.tasks.ui.editors.AbstractAttributeEditor;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPage;
import org.eclipse.mylyn.tasks.ui.editors.AttributeEditorFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarConnector;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarTaskSchema;

public class SonarTaskEditorPage extends AbstractTaskEditorPage {

  public SonarTaskEditorPage(TaskEditor editor) {
    super(editor, SonarConnector.CONNECTOR_KIND);
    setNeedsPrivateSection(true);
    setNeedsSubmitButton(true);
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
