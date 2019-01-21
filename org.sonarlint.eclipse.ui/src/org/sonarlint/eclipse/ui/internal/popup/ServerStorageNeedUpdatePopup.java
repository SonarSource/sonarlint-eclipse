/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
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
  protected void createContentArea(Composite composite) {
    Label messageLabel = new Label(composite, SWT.WRAP);

    messageLabel.setText("Binding data from the server '" + server.getId() + "' is missing or outdated.");
    messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    messageLabel.setBackground(composite.getBackground());
    Link updateServerLink = new Link(composite, SWT.NONE);
    updateServerLink.setText("<a>Update all project bindings from '" + server.getId() + "'</a>");
    updateServerLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        Job job = new ServerUpdateJob(server);
        JobUtils.scheduleAnalysisOfOpenFilesInBoundProjects(job, server, TriggerType.BINDING_CHANGE);
        job.schedule();
        ServerStorageNeedUpdatePopup.this.close();
      }
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarLint binding data missing/outdated";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }
}
