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
package org.sonarlint.eclipse.its.reddeer.conditions;

import org.eclipse.reddeer.common.condition.AbstractWaitCondition;
import org.sonarlint.eclipse.its.reddeer.views.BindingsView;

public class ServerStorageIsUpToDate extends AbstractWaitCondition {
  private final BindingsView bindingsView;
  private final String connectionName;
  private final String version;

  public ServerStorageIsUpToDate(BindingsView bindingsView, String connectionName, String version) {
    this.bindingsView = bindingsView;
    this.connectionName = connectionName;
    this.version = version;
  }

  @Override
  public boolean test() {
    bindingsView.open();
    String serverDescription = bindingsView.getTree().getItems().get(0).getText();
    return serverDescription.matches(connectionName + " \\[" +
      (version != null ? "Version: " + substringBefore(version, '-') + "(.*), " : "")
      + "Last storage update: (.*)\\]");
  }

  private static String substringBefore(String string, char separator) {
    int indexOfSeparator = string.indexOf(separator);
    if (indexOfSeparator == -1) {
      return string;
    }
    return string.substring(0, indexOfSeparator);
  }

}
