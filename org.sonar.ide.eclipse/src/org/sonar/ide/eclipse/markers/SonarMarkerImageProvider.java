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
