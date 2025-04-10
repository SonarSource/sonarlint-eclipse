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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Objects;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.internal.backend.SonarLintRpcClientSupportSynchronizer;
import org.sonarlint.eclipse.ui.internal.popup.SonarLintRpcClientSupportPopup;
import org.sonarlint.eclipse.ui.internal.views.issues.OnTheFlyIssuesView;
import org.sonarlint.eclipse.ui.internal.views.issues.SonarLintReportView;
import org.sonarlint.eclipse.ui.internal.views.issues.TaintVulnerabilitiesView;

/**
 *  This service handles changes based on Sloop being (un-)available. This includes:
 *  - pop-up when Sloop killed unexpectedly
 *  - create information in necessary issue views
 */
public class SonarLintRpcClientSupportService implements PropertyChangeListener {
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (evt.getOldValue().equals(evt.getNewValue())) {
      return;
    }

    var newValue = ((Boolean) evt.getNewValue()).booleanValue();
    if (SonarLintRpcClientSupportSynchronizer.PROPERTY_NAME.equals(evt.getPropertyName())) {
      Display.getDefault().asyncExec(() -> {
        Arrays.asList(OnTheFlyIssuesView.getInstance(),
          TaintVulnerabilitiesView.getInstance(),
          SonarLintReportView.getInstance())
          .stream()
          .filter(Objects::nonNull)
          .forEach(view -> {
            if (newValue) {
              view.resetDefaultText();
            } else {
              view.warnAboutSloopUnavailable();
            }
          });

        if (!newValue) {
          SonarLintRpcClientSupportPopup.displayPopupIfNotAlreadyDisplayed();
        }
      });
    }
  }

  // This way the UI plug-in doesn't have to interact with the core package class
  public static boolean getSloopAvailability() {
    return SonarLintRpcClientSupportSynchronizer.getSloopAvailability();
  }
}
