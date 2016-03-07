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
package org.sonarlint.eclipse.ui.internal.server;

import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.sonarlint.eclipse.core.internal.server.IServer;

public class ServerDecorator extends LabelProvider implements ILightweightLabelDecorator {

  static final String ID = "org.sonarlint.eclipse.ui.navigatorDecorator";

  @Override
  public void decorate(Object element, IDecoration decoration) {
    if (element instanceof IServer) {
      IServer server = (IServer) element;

      switch (server.getState()) {
        case STOPPED:
          addSuffix(decoration, "Unknown");
          break;
        case STARTED_NOT_SYNCED:
          addSuffix(decoration, "Not synced");
          break;
        case STARTED_SYNCED:
          addSuffix(decoration, "Version: " + server.getServerVersion() + ", Last sync: " + server.getSyncDate());
          break;
        case STARTING:
          addSuffix(decoration, "Loading...");
          break;
        case SYNCING:
          addSuffix(decoration, "Synchonizing...");
          break;
        default:
          throw new IllegalArgumentException(server.getState().name());
      }

    }
  }

  private void addSuffix(IDecoration decoration, String suffix) {
    decoration.addSuffix(" [" + suffix + "]");
  }

}
