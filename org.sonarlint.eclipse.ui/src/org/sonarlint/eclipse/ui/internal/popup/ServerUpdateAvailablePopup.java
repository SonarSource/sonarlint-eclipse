/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.ServerUpdateJob;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;

public class ServerUpdateAvailablePopup extends AbstractNotificationPopup {

  private final IServer server;

  public ServerUpdateAvailablePopup(Display display, IServer server) {
    super(display);
    this.server = server;
  }

  @Override
  protected void createContentArea(Composite composite) {
    composite.setLayout(new GridLayout(1, true));
    Label messageLabel = new Label(composite, SWT.WRAP);
    GridData layoutData = new GridData(GridData.FILL_HORIZONTAL);
    messageLabel.setLayoutData(layoutData);

    messageLabel.setText("Updates are available on SonarQube server '" + server.getId() + "'.\nDo you want to download and update them now?");
    messageLabel.setBackground(composite.getBackground());
    Composite links = new Composite(composite, SWT.NONE);
    links.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
    RowLayout rowLayout = new RowLayout();
    rowLayout.spacing = 20;
    links.setLayout(rowLayout);
    Link detailsLink = new Link(links, SWT.NONE);
    detailsLink.setText("<a>Remind me later</a>");
    detailsLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        ServerUpdateAvailablePopup.this.close();
      }
    });
    Link updateLink = new Link(links, SWT.NONE);
    updateLink.setText("<a>Update now</a>");
    updateLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        ServerUpdateAvailablePopup.this.close();
        ServerUpdateJob job = new ServerUpdateJob(server);
        JobUtils.scheduleAnalysisOfOpenFilesInBoundProjects(job, server, TriggerType.BINDING_CHANGE);
        job.schedule();
      }
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return "Updates for SonarLint";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }
}
