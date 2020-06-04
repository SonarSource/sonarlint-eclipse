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

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;

public class GenericNotificationPopup extends AbstractSonarLintPopup {

  private String title;
  private String shortMsg;
  private String longMsg;

  public GenericNotificationPopup(Display display, String title, String shortMsg, String longMsg) {
    super(display);
    this.title = title;
    this.shortMsg = shortMsg;
    this.longMsg = longMsg;
  }

  @Override
  protected String getMessage() {
    return shortMsg;
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Dismiss", e -> GenericNotificationPopup.this.close());

    addLink("More details...", e -> {
      DialogWithLink dialog = new DialogWithLink(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "SonarLint - " + title, longMsg);
      dialog.open();
      GenericNotificationPopup.this.closeFade();
    });
  }

  private static class DialogWithLink extends Dialog {

    private String title;
    private String message;

    protected DialogWithLink(Shell parentShell, String title, String message) {
      super(parentShell);
      this.title = title;
      this.message = message;
      setShellStyle(getShellStyle() | SWT.SHEET);
    }

    @Override
    protected void configureShell(Shell shell) {
      super.configureShell(shell);
      shell.setText(title);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
      createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
      Composite composite = (Composite) super.createDialogArea(parent);
      Link messageLink = new Link(composite, SWT.WRAP);
      messageLink.setText(message);
      messageLink.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          try {
            // Open default external browser
            PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
          } catch (PartInitException | MalformedURLException ex) {
            ex.printStackTrace();
          }
        }
      });
      GridDataFactory
        .fillDefaults()
        .align(SWT.FILL, SWT.BEGINNING)
        .grab(true, false)
        .hint(
          convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH),
          SWT.DEFAULT)
        .applyTo(messageLink);
      return composite;
    }

  }

  @Override
  protected String getPopupShellTitle() {
    return "SonarLint - " + title;
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }
}
