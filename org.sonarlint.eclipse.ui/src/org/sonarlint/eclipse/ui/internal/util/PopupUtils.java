/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.sonarlint.eclipse.ui.internal.notifications.AbstractNotificationPopup;

/** Utility used for reducing stacked up notifications */
public class PopupUtils {
  private PopupUtils() {
    // utility class
  }

  private static Set<Class<? extends AbstractNotificationPopup>> popupsCurrentlyDisplayed = Collections.synchronizedSet(new HashSet<>());

  public static boolean popupCurrentlyDisplayed(Class<? extends AbstractNotificationPopup> popupClass) {
    return popupsCurrentlyDisplayed.contains(popupClass);
  }

  public static void addCurrentlyDisplayedPopup(Class<? extends AbstractNotificationPopup> popupClass) {
    popupsCurrentlyDisplayed.add(popupClass);
  }

  public static void removeCurrentlyDisplayedPopup(Class<? extends AbstractNotificationPopup> popupClass) {
    popupsCurrentlyDisplayed.remove(popupClass);
  }
}
