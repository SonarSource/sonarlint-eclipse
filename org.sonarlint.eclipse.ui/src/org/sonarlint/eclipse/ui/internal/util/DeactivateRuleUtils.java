/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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

import java.util.function.Predicate;
import java.util.stream.Stream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

public class DeactivateRuleUtils {

  private DeactivateRuleUtils() {
    // Utility class
  }

  /**
   * Deactivate the rule associated with a marker.
   */
  public static void deactivateRule(IMarker marker) {
    RuleKey ruleKey = MarkerUtils.getRuleKey(marker);
    if (ruleKey == null) {
      return;
    }

    removeReportIssuesMarkers(ruleKey);

    PreferencesUtils.excludeRule(ruleKey);
    Predicate<ISonarLintFile> filter = f -> !f.getProject().isBound();
    JobUtils.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.EXCLUSION_CHANGE, filter);
  }

  private static void removeReportIssuesMarkers(RuleKey ruleKey) {
    ProjectsProviderUtils.allProjects().stream()
      .filter(p -> p.isOpen() && !p.isBound())
      .forEach(p -> findReportMarkers(p)
        .filter(m -> ruleKey.equals(MarkerUtils.getRuleKey(m)))
        .forEach(m -> {
          try {
            m.delete();
          } catch (CoreException e) {
            SonarLintLogger.get().error("Could not delete marker for deactivated rule: " + ruleKey);
          }
        }));
  }

  private static Stream<IMarker> findReportMarkers(ISonarLintProject project) {
    try {
      IMarker[] markers = project.getResource().findMarkers(SonarLintCorePlugin.MARKER_REPORT_ID, false, IResource.DEPTH_INFINITE);
      return Stream.of(markers);
    } catch (CoreException e) {
      SonarLintLogger.get().error("Could not get report markers for project: " + project.getName());
      return Stream.empty();
    }
  }
}
