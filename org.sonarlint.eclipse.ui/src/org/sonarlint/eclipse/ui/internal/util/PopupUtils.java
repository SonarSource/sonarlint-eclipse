/*
 * SonarLint for Eclipse
 * Copyright (C) SonarSource Sàrl
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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.eclipse.swt.widgets.Display;
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

  /**
   * @return {@code true} if the popup was not already marked as displayed
   */
  public static boolean addCurrentlyDisplayedPopup(Class<? extends AbstractNotificationPopup> popupClass) {
    return popupsCurrentlyDisplayed.add(popupClass);
  }

  public static void removeCurrentlyDisplayedPopup(Class<? extends AbstractNotificationPopup> popupClass) {
    popupsCurrentlyDisplayed.remove(popupClass);
  }

  /**
   * Schedule showing a popup on the SWT UI thread if it is not already displayed.
   * <p>
   * Conditions are re-checked when the runnable actually runs, because
   * {@link Display#asyncExec(Runnable)} is invoked at the next reasonable opportunity and the
   * application state may have changed in the meantime (e.g. multiple analysis events queuing the
   * same popup).
   */
  public static void scheduleAsyncDisplay(Class<? extends AbstractNotificationPopup> popupClass, Runnable openPopup) {
    scheduleAsyncDisplay(popupClass, () -> true, openPopup);
  }

  /**
   * Schedule showing a popup on the SWT UI thread if {@code shouldShow} is true and the popup is
   * not already displayed. Both conditions are re-checked when the runnable actually runs.
   */
  public static void scheduleAsyncDisplay(Class<? extends AbstractNotificationPopup> popupClass, BooleanSupplier shouldShow,
    Runnable openPopup) {
    scheduleAsyncDisplay(popupClass, shouldShow, openPopup, Display.getDefault()::asyncExec);
  }

  /**
   * Same as {@link #scheduleAsyncDisplay(Class, BooleanSupplier, Runnable)} but with an injectable UI
   * executor so callers (including tests) can simulate {@code Display.asyncExec} without an SWT Display.
   */
  public static void scheduleAsyncDisplay(Class<? extends AbstractNotificationPopup> popupClass, BooleanSupplier shouldShow,
    Runnable openPopup, Consumer<Runnable> uiExecutor) {
    if (!canShow(popupClass, shouldShow)) {
      return;
    }
    uiExecutor.accept(() -> {
      if (!shouldShow.getAsBoolean() || !addCurrentlyDisplayedPopup(popupClass)) {
        return;
      }
      openPopup.run();
    });
  }

  private static boolean canShow(Class<? extends AbstractNotificationPopup> popupClass, BooleanSupplier shouldShow) {
    return shouldShow.getAsBoolean() && !popupCurrentlyDisplayed(popupClass);
  }
}
