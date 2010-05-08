/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.views;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.sonar.ide.eclipse.views.model.TreeFile;
import org.sonar.ide.eclipse.views.model.TreeObject;
import org.sonar.ide.eclipse.views.model.TreeParent;
import org.sonar.ide.eclipse.views.model.TreeProject;

/**
 * @author Jérémie Lagarde
 */
public class NavigatorLabelProvider extends LabelProvider {

  @Override
  public String getText(Object obj) {
    if (obj instanceof TreeObject)
      return ((TreeObject) obj).getName();
    return obj.toString();
  }

  @Override
  public Image getImage(Object obj) {
    String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
    if (obj instanceof TreeParent)
      imageKey = ISharedImages.IMG_OBJ_FOLDER;
    if (obj instanceof TreeProject)
      imageKey = org.eclipse.ui.ide.IDE.SharedImages.IMG_OBJ_PROJECT;
    if (obj instanceof TreeFile)
      imageKey = ISharedImages.IMG_OBJ_FILE;
    return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
  }

}
