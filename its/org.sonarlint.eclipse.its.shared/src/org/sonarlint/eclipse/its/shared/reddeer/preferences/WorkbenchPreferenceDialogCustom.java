/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonarlint.eclipse.its.shared.reddeer.preferences;

import org.eclipse.reddeer.common.logging.Logger;
import org.eclipse.reddeer.common.matcher.RegexMatcher;
import org.eclipse.reddeer.common.platform.RunningPlatform;
import org.eclipse.reddeer.common.util.Display;
import org.eclipse.reddeer.core.matcher.WithTextMatcher;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.impl.shell.WorkbenchShell;
import org.eclipse.reddeer.workbench.workbenchmenu.WorkbenchMenuPreferencesDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class WorkbenchPreferenceDialogCustom extends WorkbenchMenuPreferencesDialog {

  private final Logger log = Logger.getLogger(WorkbenchPreferenceDialogCustom.class);

  public WorkbenchPreferenceDialogCustom() {
    super(new WithTextMatcher(new RegexMatcher("Preferences.*")), "Window", "Preferences...");
  }

  @Override
  public void open() {
    if (!isOpen()) {
      if (RunningPlatform.isOSX()) {
        log.info("Open Preferences directly on Mac OSX");
        new WorkbenchShell();
        handleMacMenu();
        setShell(new DefaultShell(matcher));
      } else {
        super.open();
      }
    }
  }

  private void handleMacMenu() {
    Display.asyncExec(new Runnable() {
      @Override
      public void run() {
        var dialog = PreferencesUtil.createPreferenceDialogOn(null,
          null, null, null);
        dialog.open();
      }
    });
    Display.syncExec(new Runnable() {
      @Override
      public void run() {
        // do nothing just process UI events
      }
    });
  }

}
