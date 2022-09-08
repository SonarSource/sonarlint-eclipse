/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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

import java.util.concurrent.TimeUnit;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.jobs.AbstractSonarProjectJob;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.util.SonarLintRuleBrowser;

public class AsyncDisplayRuleDescriptionJob extends AbstractSonarProjectJob {
  private final ResolvedBinding binding;
  private final String ruleKey;
  private final SonarLintRuleBrowser browser;

  public AsyncDisplayRuleDescriptionJob(ISonarLintProject project, ResolvedBinding binding, String ruleKey, SonarLintRuleBrowser browser) {
    super("Fetching rule description for rule '" + ruleKey + "'...", project);
    this.binding = binding;
    this.ruleKey = ruleKey;
    this.browser = browser;
  }

  @Override
  protected IStatus doRun(IProgressMonitor monitor) throws CoreException {
    try {
      var ruleDetails = binding.getEngineFacade().getRuleDescription(ruleKey, binding.getProjectBinding().projectKey()).get(1, TimeUnit.MINUTES);
      if (ruleDetails != null) {
        Display.getDefault().syncExec(() -> browser.updateRule(ruleDetails));
      } else {
        SonarLintLogger.get().error("Cannot fetch rule description for rule" + ruleKey);
      }
    } catch (Exception e) {
      SonarLintLogger.get().error("Unable to display rule description for rule " + ruleKey, e);
    }
    return Status.OK_STATUS;
  }
}
