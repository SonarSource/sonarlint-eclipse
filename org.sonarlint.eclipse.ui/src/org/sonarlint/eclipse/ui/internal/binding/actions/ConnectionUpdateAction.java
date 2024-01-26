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
package org.sonarlint.eclipse.ui.internal.binding.actions;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectionFacade;
import org.sonarlint.eclipse.core.internal.jobs.ConnectionStorageUpdateJob;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public class ConnectionUpdateAction extends SelectionProviderAction {
  private List<ConnectionFacade> connections;

  public ConnectionUpdateAction(ISelectionProvider selectionProvider) {
    super(selectionProvider, Messages.actionUpdate);
    setImageDescriptor(SonarLintImages.UPDATE_IMG);
  }

  @Override
  public void selectionChanged(IStructuredSelection sel) {
    if (sel.isEmpty()) {
      setEnabled(false);
      return;
    }
    connections = new ArrayList<>();
    var enabled = false;
    var iterator = sel.iterator();
    while (iterator.hasNext()) {
      var obj = iterator.next();
      if (obj instanceof ConnectionFacade) {
        var connection = (ConnectionFacade) obj;
        connections.add(connection);
        enabled = true;
      } else {
        setEnabled(false);
        return;
      }
    }
    setEnabled(enabled);
  }

  @Override
  public void run() {
    // It is possible that the connection is created and added to the connection view on workbench
    // startup. As a result, when the user switches to the connection view, the connection is
    // selected, but the selectionChanged event is not called, which results in connections
    // being null. When connections is null the connection will not be deleted and the error log
    // will have an IllegalArgumentException.
    //
    // To handle the case where connections is null, the selectionChanged method is called
    // to ensure connections will be populated.
    if (connections == null) {
      var sel = getStructuredSelection();
      if (sel != null) {
        selectionChanged(sel);
      }
    }

    if (connections != null) {
      for (final var connection : connections) {
        var job = new ConnectionStorageUpdateJob(connection);
        AnalysisJobsScheduler.scheduleAnalysisOfOpenFilesInBoundProjects(job, connection, TriggerType.BINDING_CHANGE);
        job.schedule();
      }
    }
  }
}
