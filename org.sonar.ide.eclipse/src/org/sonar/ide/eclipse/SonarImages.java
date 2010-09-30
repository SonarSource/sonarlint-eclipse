package org.sonar.ide.eclipse;

import org.eclipse.jface.resource.ImageDescriptor;

import java.net.MalformedURLException;
import java.net.URL;

public class SonarImages {

  public static ImageDescriptor SONARWIZBAN_IMG;
  public static ImageDescriptor SONAR16_IMG;
  public static ImageDescriptor SONAR32_IMG;
  public static ImageDescriptor SONARCONSOLE_IMG;
  public static ImageDescriptor SONARSYNCHRO_IMG;
  public static ImageDescriptor SONARREFRESH_IMG;
  public static ImageDescriptor SONARCLOSE_IMG;

  public static final String IMG_SONARWIZBAN = "sonar_wizban.gif"; //$NON-NLS-1$
  public static final String IMG_SONAR16 = "sonar.png"; //$NON-NLS-1$
  public static final String IMG_SONAR32 = "sonar32.png"; //$NON-NLS-1$
  public static final String IMG_SONARCONSOLE = "sonar.png"; //$NON-NLS-1$
  public static final String IMG_SONARSYNCHRO = "synced.gif"; //$NON-NLS-1$
  public static final String IMG_SONARREFRESH = "refresh.gif"; //$NON-NLS-1$
  public static final String IMG_SONARCLOSE = "close.gif"; //$NON-NLS-1$

  public static ImageDescriptor getImageDescriptor(final String id) {
    ImageDescriptor img = SonarImages.getCachedImageDescriptor(id);
    if (img == null) {
      img = SonarImages.loadImageDescriptor(id);
    }
    return img;
  }

  private static ImageDescriptor getCachedImageDescriptor(final String id) {
    ImageDescriptor img = null;
    if (id.equals(SonarImages.IMG_SONARWIZBAN)) {
      if (SONARWIZBAN_IMG == null) {
        SONARWIZBAN_IMG = SonarImages.loadImageDescriptor(SonarImages.IMG_SONARWIZBAN);
      }
      img = SONARWIZBAN_IMG;
    }
    if (id.equals(SonarImages.IMG_SONAR16)) {
      if (SONAR16_IMG == null) {
        SONAR16_IMG = SonarImages.loadImageDescriptor(SonarImages.IMG_SONAR16);
      }
      img = SONARCONSOLE_IMG;
    }
    if (id.equals(SonarImages.IMG_SONAR32)) {
      if (SONAR32_IMG == null) {
        SONAR32_IMG = SonarImages.loadImageDescriptor(SonarImages.IMG_SONAR32);
      }
      img = SONARCONSOLE_IMG;
    }
    if (id.equals(SonarImages.IMG_SONARCONSOLE)) {
      if (SONARCONSOLE_IMG == null) {
        SONARCONSOLE_IMG = SonarImages.loadImageDescriptor(SonarImages.IMG_SONARCONSOLE);
      }
      img = SONARCONSOLE_IMG;
    }
    if (id.equals(SonarImages.IMG_SONARSYNCHRO)) {
      if (SONARSYNCHRO_IMG == null) {
        SONARSYNCHRO_IMG = SonarImages.loadImageDescriptor(SonarImages.IMG_SONARSYNCHRO);
      }
      img = SONARSYNCHRO_IMG;
    }
    if (id.equals(SonarImages.IMG_SONARREFRESH)) {
      if (SONARREFRESH_IMG == null) {
        SONARREFRESH_IMG = SonarImages.loadImageDescriptor(SonarImages.IMG_SONARREFRESH);
      }
      img = SONARREFRESH_IMG;
    }
    if (id.equals(SonarImages.IMG_SONARCLOSE)) {
      if (SONARCLOSE_IMG == null) {
        SONARCLOSE_IMG = SonarImages.loadImageDescriptor(SonarImages.IMG_SONARCLOSE);
      }
      img = SONARCLOSE_IMG;
    }
    return img;
  }

  private static ImageDescriptor loadImageDescriptor(final String id) {
    final String iconPath = "icons/"; //$NON-NLS-1$

    try {
      final URL installURL = SonarPlugin.getDefault().getBundle().getEntry("/"); //$NON-NLS-1$
      final URL url = new URL(installURL, iconPath + id);
      return ImageDescriptor.createFromURL(url);
    } catch (final MalformedURLException e) {
      return ImageDescriptor.getMissingImageDescriptor();
    }
  }

}
