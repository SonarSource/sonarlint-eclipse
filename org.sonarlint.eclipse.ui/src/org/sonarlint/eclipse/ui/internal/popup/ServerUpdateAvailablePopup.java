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
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.internal.jobs.ServerUpdateJob;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

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
    messageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    messageLabel.setText("Some updates are available on SonarQube server '" + server.getId() + "':");
    messageLabel.setBackground(composite.getBackground());
    Link detailsLink = new Link(composite, SWT.NONE);
    detailsLink.setText("<a>Show details...</a>");
    detailsLink.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        ServerUpdateAvailablePopup.this.close();
        new UpdateDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), server).open();
      }
    });
  }

  private static class UpdateDialog extends Dialog {

    private final IServer server;

    public UpdateDialog(Shell parentShell, IServer server) {
      super(parentShell);
      this.server = server;
    }

    // overriding this methods allows you to set the
    // title of the custom dialog
    @Override
    protected void configureShell(Shell newShell) {
      super.configureShell(newShell);
      newShell.setText("Update available for SonarQube server '" + server.getId() + "'");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
      Composite container = (Composite) super.createDialogArea(parent);
      Label txt = new Label(container, SWT.NONE);
      txt.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
      StringBuilder sb = new StringBuilder();
      sb.append("Apply these changes?\n");
      for (String change : server.changelog()) {
        sb.append("  - " + change + "\n");
      }
      txt.setText(sb.toString());

      return container;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
      createButton(parent, IDialogConstants.OK_ID, "Update now", true);
      createButton(parent, IDialogConstants.CANCEL_ID, "Remind me later", false);
    }

    @Override
    protected void okPressed() {
      Job j = new ServerUpdateJob(server);
      j.schedule();
      super.okPressed();
    }

  }

  @Override
  protected String getPopupShellTitle() {
    return "New updates on SonarQube server";
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }
}
