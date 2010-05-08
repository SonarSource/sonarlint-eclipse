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

package org.sonar.ide.eclipse.markers;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * @author Jérémie Lagarde
 */
public class SonarMarkerImageProvider implements IAnnotationImageProvider {

  private static Image         image;
  private static ImageRegistry imageRegistry;

  public ImageDescriptor getImageDescriptor(String imageDescritporId) {
    return null;
  }

  public String getImageDescriptorId(Annotation annotation) {
    return null;
  }

  public Image getManagedImage(Annotation annotation) {
    if (image == null) {
      image = ImageDescriptor.createFromFile(SonarPlugin.class, "/org/sonar/ide/images/violation.png").createImage(); //$NON-NLS-1$
      String key = Integer.toString(image.hashCode());
      getImageRegistry(Display.getCurrent()).put(key, image);
    }
    return image;
  }

  private ImageRegistry getImageRegistry(Display display) {
    if (imageRegistry == null)
      imageRegistry = new ImageRegistry(display);
    return imageRegistry;
  }

}
