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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.mylyn.tasks.ui.ITasksUiConstants;
import org.eclipse.mylyn.tasks.ui.TasksUiImages;
import org.eclipse.mylyn.tasks.ui.TasksUiUtil;
import org.eclipse.mylyn.tasks.ui.editors.AbstractTaskEditorPageFactory;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditor;
import org.eclipse.mylyn.tasks.ui.editors.TaskEditorInput;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.IFormPage;
import org.sonar.ide.eclipse.internal.mylyn.core.SonarConnector;

public class SonarTaskEditorPageFactory extends AbstractTaskEditorPageFactory {

  @Override
  public boolean canCreatePageFor(TaskEditorInput input) {
    return input.getTask().getConnectorKind().equals(SonarConnector.CONNECTOR_KIND)
      || TasksUiUtil.isOutgoingNewTask(input.getTask(), SonarConnector.CONNECTOR_KIND);
  }

  @Override
  public IFormPage createPage(TaskEditor parentEditor) {
    return new SonarTaskEditorPage(parentEditor);
  }

  @Override
  public String[] getConflictingIds(TaskEditorInput input) {
    // Hide "planning" page, because we use "private" section
    return new String[] {ITasksUiConstants.ID_PAGE_PLANNING};
  }

  @Override
  public Image getPageImage() {
    // Use reflection to return CommonImages.getImage(TasksUiImages.REPOSITORY_SMALL);
    // Because the package changed in mylyn 3.7 and was removed in mylyn 3.8
    Class<?> clazzCommonImages;
    try {
      clazzCommonImages = Class.forName("org.eclipse.mylyn.internal.provisional.commons.ui.CommonImages");
    } catch (ClassNotFoundException e) {
      try {
        clazzCommonImages = Class.forName("org.eclipse.mylyn.commons.ui.CommonImages");
      } catch (ClassNotFoundException ex) {
        throw new IllegalStateException("Unable to load image");
      }
    }

    try {
      return (Image) clazzCommonImages.getMethod("getImage", ImageDescriptor.class).invoke(null, TasksUiImages.REPOSITORY_SMALL);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to load image");
    }
  }

  @Override
  public String getPageText() {
    return "Sonar"; //$NON-NLS-1$
  }

  @Override
  public int getPriority() {
    return PRIORITY_TASK;
  }

}
