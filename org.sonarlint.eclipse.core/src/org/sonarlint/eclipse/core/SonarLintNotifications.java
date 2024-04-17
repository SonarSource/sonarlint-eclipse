/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.core;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.NotificationListener;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;

public class SonarLintNotifications {
  private static final SonarLintNotifications instance = new SonarLintNotifications();
  private final List<NotificationListener> notifListeners = new ArrayList<>();

  private SonarLintNotifications() {
    // singleton
  }

  public static SonarLintNotifications get() {
    return instance;
  }

  public void addNotificationListener(NotificationListener listener) {
    notifListeners.add(listener);
  }

  public void removeNotificationListener(NotificationListener listener) {
    notifListeners.remove(listener);
  }

  public void showNotification(Notification notification) {
    for (var listener : notifListeners) {
      listener.showNotification(notification);
    }
  }

  public void showNotificationIfFirstSecretDetected() {
    if (SonarLintGlobalConfiguration.secretsNeverDetected()) {
      SonarLintNotifications.get().showNotification(new SonarLintNotifications.Notification(
        "Secret(s) detected", "SonarLint detected some secrets in one of the open files.",
        "SonarLint detected some secrets in one of the open files. " +
          "We strongly advise you to review those secrets " +
          "and ensure they are not committed into repositories. " +
          "Please refer to the SonarLint On-The-Fly view for more information."));
      SonarLintGlobalConfiguration.setSecretsWereDetected();
    }
  }

  public static class Notification {
    private final String title;
    private final String shortMsg;
    private final String longMsg;

    public Notification(String title, String shortMsg, @Nullable String longMsg) {
      this.title = title;
      this.shortMsg = shortMsg;
      this.longMsg = longMsg;
    }

    public String getTitle() {
      return title;
    }

    public String getShortMsg() {
      return shortMsg;
    }

    @Nullable
    public String getLongMsg() {
      return longMsg;
    }

    @Override
    public String toString() {
      return String.format("Notification{title=%s,shortMsg=%s,longMsg=%s}", title, shortMsg, longMsg);
    }
  }
}
