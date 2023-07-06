/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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

import java.net.URL;
import java.util.Locale;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.resource.CompositeImageDescriptor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils.FindingMatchingStatus;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.RuleType;

public final class SonarLintImages {
  public static final ImageDescriptor SONARWIZBAN_IMG = createImageDescriptor("sonarqube-48x200.png"); //$NON-NLS-1$
  public static final ImageDescriptor IMG_WIZBAN_NEW_SERVER = createImageDescriptor("new_server_wiz.png"); //$NON-NLS-1$
  public static final ImageDescriptor SONARLINT_CONSOLE_IMG_DESC = createImageDescriptor("sonarlint-16x16.png"); //$NON-NLS-1$
  public static final ImageDescriptor UPDATE_IMG = createImageDescriptor("update.gif"); //$NON-NLS-1$
  public static final ImageDescriptor SYNCED_IMG = createImageDescriptor("synced.gif"); //$NON-NLS-1$
  public static final ImageDescriptor MARK_OCCURENCES_IMG = createImageDescriptor("mark_occurrences.png"); //$NON-NLS-1$
  public static final ImageDescriptor WIZ_NEW_SERVER = createImageDescriptor("wiz_new_server.gif"); //$NON-NLS-1$
  public static final ImageDescriptor SQ_LABEL_DECORATOR = createImageDescriptor("onde-label-decorator.gif"); //$NON-NLS-1$
  public static final ImageDescriptor EDIT_SERVER = createImageDescriptor("full/menu16/editconfig.png"); //$NON-NLS-1$
  public static final ImageDescriptor UNBIND = createImageDescriptor("full/menu16/disconnect_co.png"); //$NON-NLS-1$

  public static final Image ISSUE_ANNOTATION = createImage("full/annotation16/issue.png"); //$NON-NLS-1$
  public static final Image HOTSPOT_ANNOTATION = createImage("full/annotation16/hotspot.png"); //$NON-NLS-1$
  public static final Image VULNERABILITY_ANNOTATION = createImage("full/annotation16/vulnerability.png"); //$NON-NLS-1$
  public static final Image RESOLUTION_SHOW_RULE = createImage("full/marker_resolution16/showrule.png"); //$NON-NLS-1$
  public static final Image RESOLUTION_SHOW_LOCATIONS = createImage("full/marker_resolution16/showlocations.png"); //$NON-NLS-1$
  public static final Image RESOLUTION_DISABLE_RULE = createImage("full/marker_resolution16/disablerule.png"); //$NON-NLS-1$
  public static final Image RESOLUTION_QUICKFIX_CHANGE = createImage("full/marker_resolution16/quickfix_change.png"); //$NON-NLS-1$
  public static final Image BALLOON_IMG = createImage("sonarlint-16x16.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_BLOCKER = createImage("severity/blocker.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_CRITICAL = createImage("severity/critical.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_MAJOR = createImage("severity/major.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_MINOR = createImage("severity/minor.png"); //$NON-NLS-1$
  public static final Image IMG_SEVERITY_INFO = createImage("severity/info.png"); //$NON-NLS-1$
  public static final Image IMG_HOTSPOT_HIGH = createImage("priority/high.png"); //$NON-NLS-1$
  public static final Image IMG_HOTSPOT_MEDIUM = createImage("priority/medium.png"); //$NON-NLS-1$
  public static final Image IMG_HOTSPOT_LOW = createImage("priority/low.png"); //$NON-NLS-1$

  public static final Image IMG_TYPE_BUG = createImage("type/bug.png"); //$NON-NLS-1$
  public static final Image IMG_TYPE_CODE_SMELL = createImage("type/code_smell.png"); //$NON-NLS-1$
  public static final Image IMG_TYPE_VULNERABILITY = createImage("type/vulnerability.png"); //$NON-NLS-1$

  public static final Image SONARQUBE_SERVER_ICON_IMG = createImage("logo/sonarqube-16px.png"); //$NON-NLS-1$
  public static final Image SONARQUBE_PROJECT_ICON_IMG = createImage("project-16x16.png"); //$NON-NLS-1$
  public static final Image SONARCLOUD_SERVER_ICON_IMG = createImage("logo/sonarcloud-16px.png"); //$NON-NLS-1$
  public static final Image IMG_SONARQUBE_LOGO = createImage("logo/sonarqube-black-256px.png"); //$NON-NLS-1$
  public static final Image IMG_SONARCLOUD_LOGO = createImage("logo/sonarcloud-black-256px.png"); //$NON-NLS-1$

  public static final ImageDescriptor STANDALONE_16 = createImageDescriptor("standalone-16.png"); //$NON-NLS-1$
  public static final ImageDescriptor SONARCLOUD_16 = createImageDescriptor("sonarcloud-16.png"); //$NON-NLS-1$
  public static final ImageDescriptor SONARQUBE_16 = createImageDescriptor("sonarqube-16.png"); //$NON-NLS-1$

  public static final Image IMG_OPEN_EXTERNAL = createImage("external-link-16.png"); //$NON-NLS-1$

  public static final ImageDescriptor DEBUG = createImageDescriptor("debug.gif"); //$NON-NLS-1$
  public static final ImageDescriptor SHOW_CONSOLE = createImageDescriptor("showConsole.gif"); //$NON-NLS-1$

  public static final ImageDescriptor IMG_EXPAND_ALL = createImageDescriptor("full/tool16/expandall.png"); //$NON-NLS-1$
  public static final ImageDescriptor IMG_COLLAPSE_ALL = createImageDescriptor("full/tool16/collapseall.png"); //$NON-NLS-1$

  public static final Image NOTIFICATION_CLOSE = createImage("notifications/eview16/notification-close.png"); //$NON-NLS-1$
  public static final Image NOTIFICATION_CLOSE_HOVER = createImage("notifications/eview16/notification-close-active.png"); //$NON-NLS-1$

  private SonarLintImages() {
  }
  
  /**
   *  Mapping of the matching status of an issue to the specific image
   *  
   *  @param matchingStatus specific matching status of an issue
   *  @return the corresponding connection mode icon (including standalone)
   */
  private static ImageDescriptor matchingStatusToImageDescriptor(FindingMatchingStatus matchingStatus) {
    if (matchingStatus == FindingMatchingStatus.NOT_MATCHED) {
      return STANDALONE_16;
    } else if (matchingStatus == FindingMatchingStatus.MATCHED_WITH_SC) {
      return SONARCLOUD_16;
    }
    return SONARQUBE_16;
  }

  /**
   *  Create a composite image with the markers' project connection mode, issue severity and type
   *  
   *  @param matchingStatus matching status of an issue
   *  @param severity issue severity
   *  @param type issue type
   *  @return composite image if found, null otherwise
   */
  @Nullable
  public static Image getIssueImage(@Nullable FindingMatchingStatus matchingStatus, String severity, @Nullable String type) {
    var key = matchingStatus + "/" + severity + "/" + type;
    var imageRegistry = SonarLintUiPlugin.getDefault().getImageRegistry();
    var image = imageRegistry.get(key);
    if (image == null) {
      ImageDescriptor matchingStatusImage = null;
      if (matchingStatus != null) {
        matchingStatusImage = matchingStatusToImageDescriptor(matchingStatus);
      }
      var severityImage = createImageDescriptor("severity/" + severity.toLowerCase(Locale.ENGLISH) + ".png");
      ImageDescriptor typeImage = null;
      if (type != null) {
        typeImage = createImageDescriptor("type/" + type.toLowerCase(Locale.ENGLISH) + ".png");
      }
      imageRegistry.put(key, new CompositeSeverityTypeImage(matchingStatusImage, severityImage, typeImage));
    }
    return imageRegistry.get(key);
  }

  @Nullable
  public static Image getSeverityImage(IssueSeverity severity) {
    return createImage("severity/" + severity.name().toLowerCase(Locale.ENGLISH) + ".png");
  }

  @Nullable
  public static Image getTypeImage(RuleType type) {
    return createImage("type/" + type.name().toLowerCase(Locale.ENGLISH) + ".png");
  }

  private static class CompositeSeverityTypeImage extends CompositeImageDescriptor {
    @Nullable
    private final ImageDescriptor matchingStatus;
    private final ImageDescriptor severity;
    @Nullable
    private final ImageDescriptor type;

    public CompositeSeverityTypeImage(@Nullable ImageDescriptor matchingStatus, ImageDescriptor severity,
      @Nullable ImageDescriptor type) {
      this.matchingStatus = matchingStatus;
      this.severity = severity;
      this.type = type;
    }

    @Override
    protected void drawCompositeImage(int width, int height) {
      var severityDataProvider = createCachedImageDataProvider(severity);
      if (matchingStatus != null) {
        var matchingStatusDataProvider = createCachedImageDataProvider(matchingStatus);
        if (type != null) {
          var typeDataProvider = createCachedImageDataProvider(type);
          drawImage(matchingStatusDataProvider, 0, 0);
          drawImage(typeDataProvider, 16, 0);
          drawImage(severityDataProvider, 32, 0);
        } else {
          drawImage(matchingStatusDataProvider, 0, 0);
          drawImage(severityDataProvider, 16, 0);
        }
      } else {
        if (type != null) {
          var typeDataProvider = createCachedImageDataProvider(type);
          drawImage(typeDataProvider, 0, 0);
          drawImage(severityDataProvider, 16, 0);
        } else {
          drawImage(severityDataProvider, 0, 0);
        }
      }
    }

    @Override
    protected Point getSize() {
      return new Point(48, 16);
    }
  }

  @Nullable
  private static URL getIconUrl(String key) {
    return SonarLintUiPlugin.getDefault().getBundle().getEntry("icons/" + key);
  }

  private static Image createImage(String key) {
    createImageDescriptor(key);
    var imageRegistry = SonarLintUiPlugin.getDefault().getImageRegistry();
    return imageRegistry.get(key);
  }

  private static ImageDescriptor createImageDescriptor(String key) {
    var imageRegistry = SonarLintUiPlugin.getDefault().getImageRegistry();
    var imageDescriptor = imageRegistry.getDescriptor(key);
    if (imageDescriptor == null) {
      var url = getIconUrl(key);
      if (url != null) {
        imageDescriptor = ImageDescriptor.createFromURL(url);
      } else {
        imageDescriptor = ImageDescriptor.getMissingImageDescriptor();
      }
      imageRegistry.put(key, imageDescriptor);
    }
    return imageDescriptor;
  }

}
