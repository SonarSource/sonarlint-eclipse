/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

package org.sonar.ide.eclipse;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.resource.ImageDescriptor;

public class SonarImages {

  public static ImageDescriptor SONARWIZBAN_IMG;
  public static ImageDescriptor SONAR16_IMG;
  public static ImageDescriptor SONAR32_IMG;
  public static ImageDescriptor SONARCONSOLE_IMG;
  public static ImageDescriptor SONARSYNCHRO_IMG;
  public static ImageDescriptor SONARREFRESH_IMG;
  public static ImageDescriptor SONARCLOSE_IMG;
  public static ImageDescriptor STAR_IMG;
  public static ImageDescriptor STAR_OFF_IMG;

  public static final String IMG_SONARWIZBAN = "sonar_wizban.gif"; //$NON-NLS-1$
  public static final String IMG_SONAR16 = "sonar.png"; //$NON-NLS-1$
  public static final String IMG_SONAR32 = "sonar32.png"; //$NON-NLS-1$
  public static final String IMG_SONARCONSOLE = "sonar.png"; //$NON-NLS-1$
  public static final String IMG_SONARSYNCHRO = "synced.gif"; //$NON-NLS-1$
  public static final String IMG_SONARREFRESH = "refresh.gif"; //$NON-NLS-1$
  public static final String IMG_SONARCLOSE = "close.gif"; //$NON-NLS-1$
  public static final String IMG_STAR = "star.png"; //$NON-NLS-1$
  public static final String IMG_STAR_OFF = "star_off.png"; //$NON-NLS-1$

  public static ImageDescriptor getImageDescriptor(final String id) {
    ImageDescriptor img = SonarImages.getCachedImageDescriptor(id);
    if (img == null) {
      img = SonarImages.loadImageDescriptor(id);
    }
    return img;
  }

  private static ImageDescriptor getCachedImageDescriptor(final String id) {
    ImageDescriptor img = null;
    if (id.equals(IMG_STAR)) {
      if (STAR_IMG == null) {
        STAR_IMG = SonarImages.loadImageDescriptor(IMG_STAR);
      }
      img = STAR_IMG;
    }
    if (id.equals(IMG_STAR_OFF)) {
      if (STAR_OFF_IMG == null) {
        STAR_OFF_IMG = SonarImages.loadImageDescriptor(IMG_STAR_OFF);
      }
      img = STAR_OFF_IMG;
    }
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
