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
package org.sonarlint.eclipse.ui.internal.command;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

public class DeactivateRuleCommand extends AbstractIssueCommand {

  @Override
  protected void execute(IMarker selectedMarker) {
    RuleKey ruleKey = MarkerUtils.getRuleKey(selectedMarker);
    if (ruleKey == null) {
      return;
    }

    removeReportIssuesMarkers(selectedMarker.getResource(), ruleKey);

    PreferencesUtils.excludeRule(ruleKey);
    Predicate<ISonarLintFile> filter = f -> !f.getProject().isBound();
    JobUtils.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.EXCLUSION_CHANGE, filter);
  }

  private static void removeReportIssuesMarkers(IResource resource, RuleKey ruleKey) {
    Map<String, Boolean> isBoundCache = new HashMap<>();
    MarkerUtils.findReportIssuesMarkers(resource)
      .stream()
      .filter(m -> ruleKey.equals(MarkerUtils.getRuleKey(m)))
      .filter(m -> {
        ISonarLintProject project = Adapters.adapt(m.getResource(), ISonarLintFile.class).getProject();
        return !isBoundCache.computeIfAbsent(project.getName(), key -> project.isBound());
      })
      .forEach(m -> {
        try {
          m.delete();
        } catch (CoreException e) {
          SonarLintLogger.get().error("Could not delete marker for deactivated rule: " + ruleKey);
        }
      });
  }
}
