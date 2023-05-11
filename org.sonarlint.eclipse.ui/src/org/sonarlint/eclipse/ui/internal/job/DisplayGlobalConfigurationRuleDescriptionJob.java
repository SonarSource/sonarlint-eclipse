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
package org.sonarlint.eclipse.ui.internal.job;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.jobs.AbstractSonarGlobalConfigurationJob;
import org.sonarlint.eclipse.ui.internal.rule.RuleDetailsPanel;

/** Update rule details view for the global configuration rule description in a separate thread */
public class DisplayGlobalConfigurationRuleDescriptionJob extends AbstractSonarGlobalConfigurationJob {
  private final String ruleKey;
  private final RuleDetailsPanel ruleDetailsPanel;

  public DisplayGlobalConfigurationRuleDescriptionJob(String ruleKey, RuleDetailsPanel ruleDetailsPanel) {
    super("Fetching rule description for rule '" + ruleKey + "'...");
    this.ruleKey = ruleKey;
    this.ruleDetailsPanel = ruleDetailsPanel;
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor) throws CoreException {
    try {
      Display.getDefault().syncExec(ruleDetailsPanel::displayLoadingIndicator);
      // Getting the CompletableFuture<...> object before running the UI update to not block the UI thread
      var ruleDetails = SonarLintBackendService.get().getStandaloneRuleDetails(ruleKey);
      Display.getDefault().syncExec(() -> ruleDetailsPanel.updateRule(ruleDetails));
    } catch (Exception e) {
      SonarLintLogger.get().error("Unable to display global configuration rule description for rule " + ruleKey, e);
      Display.getDefault().syncExec(ruleDetailsPanel::clearRule);
      return Status.error(e.getMessage(), e);
    }

    return Status.OK_STATUS;
  }

}
