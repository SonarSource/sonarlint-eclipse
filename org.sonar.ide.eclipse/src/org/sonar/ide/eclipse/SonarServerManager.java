/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;
import org.sonar.ide.eclipse.preferences.PreferenceConstants;
import org.sonar.ide.shared.DefaultServerManager;
import org.sonar.wsclient.Host;

/**
 * @author Jérémie Lagarde
 */
public class SonarServerManager extends DefaultServerManager {

  protected SonarServerManager() {
    super(SonarPlugin.getDefault().getStateLocation().makeAbsolute().toOSString());
  }

  public Host getDefaultServer() throws Exception {
    String url = SonarPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SONAR_SERVER_URL);
    return findServer(url);
  }

  @Override
  protected void notifyListeners(final int eventType) {
    for (final IServerSetListener listener : serverSetListeners) {
      Display.getDefault().asyncExec(new Runnable() {
        public void run() {
          try {
            listener.serverSetChanged(eventType, serverList);
          } catch (Throwable t) {
            SonarPlugin.getDefault().writeLog(IStatus.ERROR, t.getMessage(), t);
          }
        }
      });
    }
  }
}
