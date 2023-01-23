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
package org.sonarlint.eclipse.ui.internal.extension;

import java.util.Collection;
import java.util.List;
import org.sonarlint.eclipse.core.internal.extension.AbstractSonarLintExtensionTracker;
import org.sonarlint.eclipse.ui.quickfixes.IMarkerResolutionEnhancer;

public class SonarLintUiExtensionTracker extends AbstractSonarLintExtensionTracker {

  private static SonarLintUiExtensionTracker singleInstance = null;

  private final SonarLintEP<IMarkerResolutionEnhancer> markerResolutionEnhancerEp = new SonarLintEP<>("org.sonarlint.eclipse.ui.markerResolutionEnhancer"); //$NON-NLS-1$

  private final Collection<SonarLintEP<?>> allEps = List.of(markerResolutionEnhancerEp);

  private SonarLintUiExtensionTracker() {
    init(allEps);
  }

  public static synchronized SonarLintUiExtensionTracker getInstance() {
    if (singleInstance == null) {
      singleInstance = new SonarLintUiExtensionTracker();
    }
    return singleInstance;
  }

  public static void close() {
    if (singleInstance != null) {
      singleInstance.unregister();
    }
  }

  public Collection<IMarkerResolutionEnhancer> getMarkerResolutionEnhancers() {
    return markerResolutionEnhancerEp.getInstances();
  }

}
