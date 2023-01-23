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
package org.sonarlint.eclipse.core.internal.jobs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class StorageSynchronizerJob extends Job {

  private List<ISonarLintProject> projectsToSync;

  public StorageSynchronizerJob(List<ISonarLintProject> projectsToSync) {
    super("Synchronize local storage with SonarQube/SonarCloud");
    this.projectsToSync = projectsToSync;
    setPriority(DECORATE);
    setSystem(true);
  }

  @Override
  protected IStatus run(IProgressMonitor monitor) {
    Map<String, Set<String>> projectsToSyncByConnectionId = new HashMap<>();
    for (ISonarLintProject project : projectsToSync) {
      Optional<ResolvedBinding> bindingOpt = SonarLintCorePlugin.getServersManager().resolveBinding(project);
      if (bindingOpt.isEmpty()) {
        continue;
      }
      projectsToSyncByConnectionId.computeIfAbsent(bindingOpt.get().getProjectBinding().connectionId(), id -> new HashSet<>())
        .add(bindingOpt.get().getProjectBinding().projectKey());
    }

    var subMonitor = SubMonitor.convert(monitor, projectsToSyncByConnectionId.size());
    subMonitor.setTaskName("Sync SonarLint Storages");
    for (Map.Entry<String, Set<String>> entry : projectsToSyncByConnectionId.entrySet()) {
      String connectionId = entry.getKey();
      Set<String> projectKeys = entry.getValue();
      SonarLintCorePlugin.getServersManager().findById(connectionId).ifPresent(connection -> {
        subMonitor.subTask("Sync SonarLint Storage for connection '" + connectionId + "'");
        var connectionMonitor = subMonitor.newChild(1);
        try {
          connection.autoSync(projectKeys, connectionMonitor);
        } catch (Exception e) {
          SonarLintLogger.get().error("Unable to synchronize local storage for connection '" + connection.getId() + "'", e);
        }
        connectionMonitor.done();
      });

    }
    return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
  }

}
