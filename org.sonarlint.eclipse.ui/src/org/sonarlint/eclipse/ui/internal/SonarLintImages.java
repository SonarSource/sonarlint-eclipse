/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.ui.internal;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;

public final class SonarLintImages {

  private static final URL BASE_URL = SonarLintUiPlugin.getDefault().getBundle().getEntry("/icons/"); //$NON-NLS-1$

  public static final ImageDescriptor SONARWIZBAN_IMG = createImageDescriptor("sonarqube-48x200.png"); //$NON-NLS-1$
  public static final ImageDescriptor IMG_WIZBAN_NEW_SERVER = createImageDescriptor("new_server_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor SONARLINT_CONSOLE_IMG_DESC = createImageDescriptor("sonarlint-16x16.png"); //$NON-NLS-1$
  public static final ImageDescriptor UPDATE_IMG = createImageDescriptor("update.gif"); //$NON-NLS-1$
  public static final ImageDescriptor SYNCED_IMG = createImageDescriptor("synced.gif"); //$NON-NLS-1$
  public static final ImageDescriptor WIZ_NEW_SERVER = createImageDescriptor("wiz_new_server.gif"); //$NON-NLS-1$
  public static final ImageDescriptor SQ_LABEL_DECORATOR = createImageDescriptor("onde-label-decorator.gif"); //$NON-NLS-1$

  public static final Image RUN_IMG = createImage("sonarlint-run-16x16.png"); //$NON-NLS-1$
  public static final Image BALLOON_IMG = createImage("sonarlint-16x16.png"); //$NON-NLS-1$
  public static final Image SERVER_ICON_IMG = createImage("onde-sonar-16.png"); //$NON-NLS-1$
  public static final Image IMG_ISSUE = createImage("issue_annotation.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_BLOCKER = createImage("severity/blocker.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_CRITICAL = createImage("severity/critical.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_MAJOR = createImage("severity/major.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_MINOR = createImage("severity/minor.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_INFO = createImage("severity/info.png"); //$NON-NLS-1$

  public static final Image NOTIFICATION_CLOSE = createImage("popup/notification-close.gif"); //$NON-NLS-1$
  public static final Image NOTIFICATION_CLOSE_HOVER = createImage("popup/notification-close-active.gif"); //$NON-NLS-1$

  public static final ImageDescriptor DEBUG = createImageDescriptor("debug.gif"); //$NON-NLS-1$
  public static final ImageDescriptor SHOW_CONSOLE = createImageDescriptor("showConsole.gif"); //$NON-NLS-1$

  private SonarLintImages() {
  }

  private static URL getUrl(String key) throws MalformedURLException {
    return new URL(BASE_URL, key);
  }

  private static Image createImage(String key) {
    createImageDescriptor(key);
    ImageRegistry imageRegistry = getImageRegistry();
    return imageRegistry != null ? imageRegistry.get(key) : null;
  }

  private static ImageDescriptor createImageDescriptor(String key) {
    ImageRegistry imageRegistry = getImageRegistry();
    if (imageRegistry != null) {
      ImageDescriptor imageDescriptor = imageRegistry.getDescriptor(key);
      if (imageDescriptor == null) {
        try {
          imageDescriptor = ImageDescriptor.createFromURL(getUrl(key));
        } catch (MalformedURLException e) {
          imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
        }
        imageRegistry.put(key, imageDescriptor);
      }
      return imageDescriptor;
    }
    return null;
  }

  private static ImageRegistry getImageRegistry() {
    // "org.eclipse.swt.SWTError: Invalid thread access" might be thrown during unit tests
    if (PlatformUI.isWorkbenchRunning()) {
      return SonarLintUiPlugin.getDefault().getImageRegistry();
    }
    return null;
  }

}
