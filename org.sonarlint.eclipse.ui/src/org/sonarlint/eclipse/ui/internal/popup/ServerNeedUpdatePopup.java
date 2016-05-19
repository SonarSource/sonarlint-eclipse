/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.sonarlint.eclipse.core.internal.jobs.ServerUpdateJob;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public class ServerNeedUpdatePopup extends AbstractNotificationPopup {

  private final IServer server;

  public ServerNeedUpdatePopup(Display display, IServer server) {
    super(display);
    this.server = server;
  }

  @Override
  protected void createContentArea(Composite composite) {
    composite.setLayout(new GridLayout(1, true));
    Label messageLabel = new Label(composite, SWT.WRAP);
    messageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    messageLabel.setText("Local configuration from SonarQube server '" + server.getId() + "' is missing or outdated.");
    messageLabel.setBackground(composite.getBackground());
    Link updateServerLink = new Link(composite, SWT.NONE);
    updateServerLink.setText("<a>Update all project bindings from '" + server.getId() + "'</a>");
    updateServerLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        Job j = new ServerUpdateJob(server);
        j.schedule();
        ServerNeedUpdatePopup.this.close();
      }
    });
  }

  @Override
  protected String getPopupShellTitle() {
    return "Local SonarQube configuration outdated";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }
}
