/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.preferences.SonarLintPreferencePage;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;

public class GenericNotificationPopup extends AbstractSonarLintPopup {

  private final String title;
  private final String shortMsg;
  private final String longMsg;
  private final String learnMoreUrl;

  public GenericNotificationPopup(String title, String shortMsg, @Nullable String longMsg,
    @Nullable String learnMoreUrl) {
    this.title = title;
    this.shortMsg = shortMsg;
    this.longMsg = longMsg;
    this.learnMoreUrl = learnMoreUrl;
  }

  @Override
  protected String getMessage() {
    return shortMsg;
  }

  @Override
  protected void createContentArea(Composite composite) {
    super.createContentArea(composite);

    addLink("Dismiss", e -> close());

    if (learnMoreUrl != null) {
      addLink("Learn more",
        e -> BrowserUtils.openExternalBrowser(learnMoreUrl, PlatformUI.getWorkbench().getDisplay()));
    }

    if (longMsg != null) {
      addLink("More details...", e -> {
        var dialog = new DialogWithLink(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
          "SonarQube - " + title, longMsg);
        dialog.open();
      });
    }
  }

  private static class DialogWithLink extends Dialog {

    private final String title;
    private final String message;

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
      var composite = (Composite) super.createDialogArea(parent);
      var messageLink = new Link(composite, SWT.WRAP);
      messageLink.setText(message);
      messageLink.addSelectionListener(new SelectionAdapter() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          if ("#edit-settings".equals(e.text)) {
            var pref = PreferencesUtil.createPreferenceDialogOn(DialogWithLink.this.getShell(), SonarLintPreferencePage.ID, null, null);
            if (pref != null) {
              pref.open();
            }
          } else {
            BrowserUtils.openExternalBrowser(e.text, e.display);
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
    return "SonarQube - " + title;
  }

  @Override
  protected Image getPopupShellImage(int maximumHeight) {
    return SonarLintImages.BALLOON_IMG;
  }
}
