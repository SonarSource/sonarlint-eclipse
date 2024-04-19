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
package org.sonarlint.eclipse.core.internal.jobs;

import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.internal.backend.ConfigScopeSynchronizer;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/**
 *  Job to enable all binding suggestions of specific projects based on their own configuration which is coming from
 *  the IDE side (stored in Eclipse settings). Is is useful (when scheduled for some time after the binding) in order
 *  to prevent stuck "Suggest Binding" messages to appear in the IDE when a project is already bound!
 */
public class EnableBindingSuggestionsJob extends Job {
  private final Collection<ISonarLintProject> projects;

  public EnableBindingSuggestionsJob(Collection<ISonarLintProject> projects) {
    super("Try to enable binding suggestions on project(s) ...");
    this.projects = projects;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    for (var project : projects) {
      ConfigScopeSynchronizer.enableAllBindingSuggestions(project);
    }
    return Status.OK_STATUS;
  }
}
