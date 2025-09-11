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
package org.sonarlint.eclipse.ui.internal;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.sonarlint.eclipse.core.internal.event.AnalysisEvent;
import org.sonarlint.eclipse.core.internal.event.AnalysisListener;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.ui.internal.notifications.Notification;

/** Service to handle analysis accuracy -> automatic workspace build should be enabled */
public class SonarLintNoAutomaticBuildWarningService implements AnalysisListener {
  private boolean notifiedOnce;

  @Override
  public void usedAnalysis(AnalysisEvent event) {
    if (ResourcesPlugin.getWorkspace().getDescription().isAutoBuilding() || SonarLintGlobalConfiguration.noAutomaticBuildWarning() || notifiedOnce) {
      return;
    }
    Notification.newNotification()
      .setTitle("Automatic build of workspace disabled")
      .setBody("The accuracy of analysis results might be slightly impacted as some rules require the context of the "
        + "compiled bytecode provided by the automatic build of workspace.")
      .addAction("Enable automatic build of workspace", shell -> PreferencesUtil.createPreferenceDialogOn(shell, "org.eclipse.ui.preferencePages.BuildOrder", null, null).open())
      .addDoNotShowAgainAction(SonarLintGlobalConfiguration::setNoAutomaticBuildWarning)
      .show();
  }
}
