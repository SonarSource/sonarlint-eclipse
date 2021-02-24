/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.engine.connected.IConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.jobs.ServerUpdateJob;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.binding.actions.JobUtils;

public class ServerUpdateAvailablePopup extends AbstractSonarLintPopup {

  private final IConnectedEngineFacade server;

  public ServerUpdateAvailablePopup(IConnectedEngineFacade server) {
    this.server = server;
  }

  @Override
  protected String getMessage() {
    return "We detected some changes on the server '" + server.getId() + "'.\nDo you want to update the SonarLint bindings now?";
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Remind me later", e -> ServerUpdateAvailablePopup.this.close());

    addLink("Update now", e -> {
      ServerUpdateJob job = new ServerUpdateJob(server);
      JobUtils.scheduleAnalysisOfOpenFilesInBoundProjects(job, server, TriggerType.BINDING_CHANGE);
      job.schedule();
      close();
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarLint - Binding updates available";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }
}
