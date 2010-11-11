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
import org.sonar.ide.eclipse.core.ISonarMeasure;

public class SonarImages {

  private static final URL baseUrl = SonarPlugin.getDefault().getBundle().getEntry("/icons/"); //$NON-NLS-1$

  public static ImageDescriptor STAR = create("star.png"); //$NON-NLS-1$
  public static ImageDescriptor STAR_OFF = create("star_off.png"); //$NON-NLS-1$

  public static ImageDescriptor SONARWIZBAN_IMG = create("sonar_wizban.gif"); //$NON-NLS-1$
  public static ImageDescriptor SONAR16_IMG = create("sonar.png"); //$NON-NLS-1$
  public static ImageDescriptor SONAR32_IMG = create("sonar32.png"); //$NON-NLS-1$
  public static ImageDescriptor SONARSYNCHRO_IMG = create("synced.gif"); //$NON-NLS-1$
  public static ImageDescriptor SONARREFRESH_IMG = create("refresh.gif"); //$NON-NLS-1$
  public static ImageDescriptor SONARCLOSE_IMG = create("close.gif"); //$NON-NLS-1$

  private static ImageDescriptor[][] TENDENCY = {
      { createTendency("-2-red"), createTendency("-1-red"), createTendency("1-red"), createTendency("2-red") },
      { createTendency("-2-black"), createTendency("-1-black"), createTendency("1-black"), createTendency("2-black") },
      { createTendency("-2-green"), createTendency("-1-green"), createTendency("1-green"), createTendency("2-green") } };

  public static ImageDescriptor forTendency(ISonarMeasure measure) {
    int trend = measure.getTrend(); // color
    int var = measure.getVar(); // value
    if (var == 0) {
      return null;
    }
    return TENDENCY[trend + 1][var + 2];
  }

  private static ImageDescriptor createTendency(String name) {
    return create("tendency/" + name + ".png");
  }

  public static ImageDescriptor create(String name) {
    try {
      return ImageDescriptor.createFromURL(getUrl(name));
    } catch (MalformedURLException e) {
      return ImageDescriptor.getMissingImageDescriptor();
    }
  }

  private static URL getUrl(String name) throws MalformedURLException {
    return new URL(baseUrl, name);
  }

}
