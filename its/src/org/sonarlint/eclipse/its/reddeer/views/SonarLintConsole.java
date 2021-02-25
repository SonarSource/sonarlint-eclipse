/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonarlint.eclipse.its.reddeer.views;

import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.eclipse.condition.ConsoleHasLabel;
import org.eclipse.reddeer.eclipse.ui.console.ConsoleView;
import org.eclipse.reddeer.swt.impl.menu.ToolItemMenuItem;
import org.eclipse.reddeer.swt.impl.toolbar.DefaultToolItem;
import org.hamcrest.Matcher;
import org.hamcrest.core.StringEndsWith;

public class SonarLintConsole extends ConsoleView {

  public void openConsole(String text) {
    // Console name starts with a sequence number
    openConsole(StringEndsWith.endsWith(text + " Console"));
  }

  @SuppressWarnings("unchecked")
  public void openConsole(Matcher<String> textMatcher) {
    activate();
    ToolItemMenuItem menu = new ToolItemMenuItem(new DefaultToolItem(cTabItem.getFolder(), "Open Console"), textMatcher);
    menu.select();
    new WaitUntil(new ConsoleHasLabel(textMatcher));
  }

  public void enableVerboseOutput() {
    activate();
    ToolItemMenuItem menu = new ToolItemMenuItem(new DefaultToolItem(cTabItem.getFolder(), "Configure logs"), "Verbose output");
    if (!menu.isSelected()) {
      menu.select();
    }
  }

  public void enableAnalysisLogs() {
    activate();
    ToolItemMenuItem menu = new ToolItemMenuItem(new DefaultToolItem(cTabItem.getFolder(), "Configure logs"), "Analysis logs");
    if (!menu.isSelected()) {
      menu.select();
    }
  }

  public void showConsole(ShowConsoleOption option) {
    activate();
    ToolItemMenuItem menu = new ToolItemMenuItem(new DefaultToolItem(cTabItem.getFolder(), "Show Console"), option.label);
    if (!menu.isSelected()) {
      menu.select();
    }
  }

  public enum ShowConsoleOption {

    NEVER("Never"),
    ON_OUTPUT("On output"),
    ON_ERROR("On error");

    private final String label;

    private ShowConsoleOption(String label) {
      this.label = label;
    }
  }

}
