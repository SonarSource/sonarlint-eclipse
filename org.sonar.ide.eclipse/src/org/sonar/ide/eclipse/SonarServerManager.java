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

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;
import org.sonar.ide.client.SonarClient;
import org.sonar.ide.eclipse.preferences.PreferenceConstants;
import org.sonar.ide.shared.DefaultServerManager;
import org.sonar.ide.ui.ConsoleManager;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Model;
import org.sonar.wsclient.services.Query;

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

  @Override
  public Sonar getSonar(String url) {
    final Host server = createServer(url);
    return new EclipseSonarClient(server.getHost(), server.getUsername(), server.getPassword());
  }

  static final class EclipseSonarClient extends SonarClient {

    public EclipseSonarClient(String host) {
      super(host);
    }

    public EclipseSonarClient(String host, String username, String password) {
      super(host, username, password);
    }

    @Override
    public <MODEL extends Model> MODEL find(Query<MODEL> query) {
      ConsoleManager.getConsole().logRequest("find : " + query.getUrl());
      MODEL model = super.find(query);
      ConsoleManager.getConsole().logResponse(model != null ? model.toString() : null);
      return model;
    }

    @Override
    public <MODEL extends Model> List<MODEL> findAll(Query<MODEL> query) {
      ConsoleManager.getConsole().logRequest("find : " + query.getUrl());
      List<MODEL> result = super.findAll(query);
      ConsoleManager.getConsole().logResponse("Retrieved " + result.size() + " elements.");
      return result;
    }
  }
}