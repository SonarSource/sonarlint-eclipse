/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.popup;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.ServerUpdateJob;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;

public class ServerStorageNeedUpdatePopup extends AbstractSonarLintPopup {

  private final IServer server;

  public ServerStorageNeedUpdatePopup(Display display, IServer server) {
    super(display);
    this.server = server;
    // Don't close this popup
    setDelayClose(0);
  }

  @Override
  protected String getMessage() {
    return "Local storage of the binding to the server '" + server.getId() + "' is missing or outdated.";
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Update all project bindings from '" + server.getId() + "'", e -> {
      Job job = new ServerUpdateJob(server);
      JobUtils.scheduleAnalysisOfOpenFilesInBoundProjects(job, server, TriggerType.BINDING_CHANGE);
      job.schedule();
      ServerStorageNeedUpdatePopup.this.close();
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarLint - Invalid binding storage";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }
}
