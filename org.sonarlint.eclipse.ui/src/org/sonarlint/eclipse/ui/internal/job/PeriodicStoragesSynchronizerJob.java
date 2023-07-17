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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.binding.actions.AnalysisJobsScheduler;

public class PeriodicStoragesSynchronizerJob extends Job {

  private final long syncPeriod;

  public PeriodicStoragesSynchronizerJob() {
    super("Synchronize local storage with SonarQube/SonarCloud");
    setPriority(DECORATE);
    setSystem(true);
    syncPeriod = Long.parseLong(StringUtils.defaultIfBlank(System.getenv("SONARLINT_INTERNAL_SYNC_PERIOD"), "3600"));
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    try {
      var subMonitor = SubMonitor.convert(monitor, SonarLintCorePlugin.getServersManager().getServers().size());
      subMonitor.setTaskName("Checking SonarLint Binding Updates");
      for (final var connection : SonarLintCorePlugin.getServersManager().getServers()) {
        subMonitor.subTask("Checking SonarLint Binding Updates for connection '" + connection.getId() + "'");
        var serverMonitor = subMonitor.newChild(1);

        try {
          var boundProjectKeys = connection.getBoundProjectKeys();
          connection.scheduledSync(boundProjectKeys, serverMonitor);
          AnalysisJobsScheduler.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.BINDING_CHANGE, f -> isBoundToConnection(f, connection));
          // TODO Refresh taints
        } catch (Exception e) {
          SonarLintLogger.get().error("Unable to synchronize local storage for connection '" + connection.getId() + "'", e);
        }

        serverMonitor.done();
      }
      return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
    } finally {
      schedule(syncPeriod * 1000);
    }
  }

  private static boolean isBoundToConnection(ISonarLintFile f, IConnectedEngineFacade facade) {
    var config = SonarLintCorePlugin.loadConfig(f.getProject());
    return config.isBound() && facade.getId().equals(config.getProjectBinding().get().connectionId());
  }

}
