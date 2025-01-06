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

import java.lang.reflect.InvocationTargetException;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;

public class DeactivateRuleUtils {

  private DeactivateRuleUtils() {
    // Utility class
  }

  /**
   * Deactivate the rule associated with a marker.
   */
  public static void deactivateRule(IMarker marker) {
    var ruleKey = MarkerUtils.getRuleKey(marker);
    if (ruleKey == null) {
      return;
    }

    SonarLintGlobalConfiguration.disableRule(ruleKey);

    var op = new WorkspaceModifyOperation() {
      @Override
      protected void execute(IProgressMonitor monitor) throws CoreException {
        removeReportIssuesMarkers(ruleKey);
        removeAnnotations(marker);
      }
    };

    try {
      op.run(null);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    } catch (InvocationTargetException e) {
      SonarLintLogger.get().error("Could not get remove markers for deactivated rule", e);
    }

    Predicate<ISonarLintFile> filter = f -> !SonarLintCorePlugin.loadConfig(f.getProject()).isBound();

    // Assuming the user is willingly changing the rules for a standalone project, don't check for unsupported
    // languages as this is not the correct trigger in this moment!
    AnalysisJobsScheduler.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.STANDALONE_CONFIG_CHANGE, filter);
  }

  private static void removeAnnotations(IMarker marker) {
    if (marker.equals(SonarLintUiPlugin.getSonarlintMarkerSelectionService().getLastSelectedMarker().orElse(null))) {
      SonarLintUiPlugin.getSonarlintMarkerSelectionService().markerSelected(null, false, false);
    }
  }

  private static void removeReportIssuesMarkers(String ruleKey) {
    SonarLintUtils.allProjects().stream()
      .filter(p -> p.isOpen() && !SonarLintCorePlugin.loadConfig(p).isBound())
      .forEach(p -> Stream.concat(findSonarLintMarkers(p, SonarLintCorePlugin.MARKER_REPORT_ID), findSonarLintMarkers(p, SonarLintCorePlugin.MARKER_ON_THE_FLY_ID))
        .filter(m -> ruleKey.equals(MarkerUtils.getRuleKey(m)))
        .forEach(m -> {
          try {
            m.delete();
          } catch (CoreException e) {
            SonarLintLogger.get().error("Could not delete marker for deactivated rule: " + ruleKey);
          }
        }));
  }

  private static Stream<IMarker> findSonarLintMarkers(ISonarLintProject project, String id) {
    try {
      var markers = project.getResource().findMarkers(id, false, IResource.DEPTH_INFINITE);
      return Stream.of(markers);
    } catch (CoreException e) {
      SonarLintLogger.get().error("Could not get report markers for project: " + project.getName());
      return Stream.empty();
    }
  }
}
