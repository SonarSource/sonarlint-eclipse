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
package org.sonarlint.eclipse.core.internal.jobs;

import java.util.Collection;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintIssuable;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

/**
 *  Job to update taint issues after synchronization while also fetching them from the server in contrast to
 *  {@link TaintIssuesUpdateOnFileOpenedJob}, where it was already fetched before!
 */
public class TaintIssuesUpdateOnFileOpenedJob extends Job {
  private final Collection<ISonarLintIssuable> issuables;
  private final ConnectionFacade engineFacade;

  public TaintIssuesUpdateOnFileOpenedJob(ConnectionFacade engineFacade,
    ISonarLintProject project,
    Collection<ISonarLintIssuable> issuables) {
    super("Fetch server taint issues for " + project.getName());
    this.engineFacade = engineFacade;
    setPriority(DECORATE);
    this.issuables = issuables;
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      // To access the preference service only once and not per issue
      var issuesIncludingResolved = SonarLintGlobalConfiguration.issuesIncludingResolved();
      var issuesOnlyNewCode = SonarLintGlobalConfiguration.issuesOnlyNewCode();

      for (var issuable : issuables) {
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        if (issuable instanceof ISonarLintFile) {
          var file = ((ISonarLintFile) issuable);
          SonarLintMarkerUpdater.refreshMarkersForTaint(file, engineFacade, issuesIncludingResolved, issuesOnlyNewCode, monitor);
        }
      }
      return Status.OK_STATUS;
    } catch (Throwable t) {
      // note: without catching Throwable, any exceptions raised in the thread will not be visible
      SonarLintLogger.get().error("Error while fetching server taint issues", t);
      return new Status(IStatus.ERROR, SonarLintCorePlugin.PLUGIN_ID, t.getMessage());
    }
  }
}
