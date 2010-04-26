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