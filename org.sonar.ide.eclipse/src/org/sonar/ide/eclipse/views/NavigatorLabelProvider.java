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
