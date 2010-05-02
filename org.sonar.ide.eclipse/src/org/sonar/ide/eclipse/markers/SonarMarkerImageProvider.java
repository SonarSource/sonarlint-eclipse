package org.sonar.ide.eclipse.markers;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;
import org.sonar.ide.eclipse.SonarPlugin;

/**
 * @author Jérémie Lagarde
 */
public class SonarMarkerImageProvider implements IAnnotationImageProvider {

  public ImageDescriptor getImageDescriptor(String imageDescritporId) {
    return null;
  }

  public String getImageDescriptorId(Annotation annotation) {
    return null;
  }

  public Image getManagedImage(Annotation annotation) {
    return ImageDescriptor.createFromFile(SonarPlugin.class, "/org/sonar/ide/images/violation.png").createImage();
  }

}
