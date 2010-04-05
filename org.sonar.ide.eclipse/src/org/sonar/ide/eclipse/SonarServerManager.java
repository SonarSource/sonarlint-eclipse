package org.sonar.ide.eclipse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.widgets.Display;
import org.sonar.ide.client.SonarClient;
import org.sonar.ide.eclipse.preferences.PreferenceConstants;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;

/**
 * @author Jérémie Lagarde
 */
public class SonarServerManager {

  private static final String SERVER_CACHE_NAME = ".serverlist";        //$NON-NLS-1$

  private final ArrayList<Host> serverList = new ArrayList<Host>();

  SonarServerManager() {
    try {
      load();
    } catch (Exception e) {
      SonarPlugin.getDefault().writeLog(IStatus.ERROR, e.getMessage(), e);
    }
  }

  public void addServer(String location) throws Exception {
    addServer(new Host(location));
  }

  public void addServer(Host server) throws Exception {
    if (findServer(server.getHost()) != null) {
      throw new Exception("Duplicate server: " + server.getHost()); //$NON-NLS-1$
    }
    try {
      serverList.add(server);
      notifyListeners(IServerSetListener.SERVER_ADDED);
      commit();
    } catch (Exception e) {
      SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
    }
  }

  public List<Host> getServers() {
    return serverList;
  }
  
  public boolean removeServer(String host) {
    Host server = findServer(host);
    if (server == null) {
      return false;
    }
    boolean result = false;
    try {
      result = serverList.remove(server);
      notifyListeners(IServerSetListener.SERVER_REMOVED);
      commit();
    } catch (Exception e) {
      SonarPlugin.getDefault().displayError(IStatus.ERROR, e.getMessage(), e, true);
    }
    return result;
  }

  
  public Host createServer(String url)  throws Exception {
    if(StringUtils.isBlank(url))
      return null;
    Host host = findServer(url);
    if(host==null) {
      host = new Host(url);
      addServer(host);
      commit();
    }
    return host;
  }
  
  public Host findServer(String host) {
    Host server = null;
    for (Host element : serverList) {
      if (element.getHost().equals(host)) {
        server = element;
        break;
      }
    }
    return server;
  }

  public Host getDefaultServer() throws Exception {
    String url = SonarPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_SONAR_SERVER_URL);
    return findServer(url);
  }
  
  public Sonar getSonar(String url) throws Exception {
    final Host server = createServer(url);
    // return new Sonar(new HttpClient4Connector(server));
    return new SonarClient(server.getHost(),server.getUsername(),server.getPassword());
  }
  
  private void commit() throws Exception {
    File serverListFile = SonarPlugin.getDefault().getStateLocation().append(SERVER_CACHE_NAME).toFile();
    FileOutputStream fos = null;
    PrintWriter writer = null;
    try {
      fos = new FileOutputStream(serverListFile);
      writer = new PrintWriter(fos);
      for (Host server : serverList) {
        writer.println(server.getHost());
      }
      writer.flush();
      fos.flush();
    } finally {
      if (writer != null) {
        writer.close();
      }
      if (fos != null) {
        fos.close();
      }
    }
  }

  private void load() throws Exception {
    serverList.clear();
    File serverListFile = SonarPlugin.getDefault().getStateLocation().append(SERVER_CACHE_NAME).toFile();
    if (!serverListFile.exists()) {
      return;
    }
    FileInputStream fis = null;
    BufferedReader reader = null;
    try {
      fis = new FileInputStream(serverListFile);
      reader = new BufferedReader(new InputStreamReader(fis));
      String host = null;
      do {
        host = reader.readLine();
        if (host != null && host.trim().length() > 0) {
          serverList.add(new Host(host));
        }
      } while (host != null);
    } finally {
      if (fis != null) {
        fis.close();
      }
      if (reader != null) {
        reader.close();
      }
    }
  }

  public interface IServerSetListener {
    public static final int SERVER_ADDED = 0;
    public static final int SERVER_REMOVED = 1;

    public void serverSetChanged(int type, List<Host> serverList);
  }

  private final List<IServerSetListener> serverSetListeners = new ArrayList<IServerSetListener>();

  public boolean addServerSetListener(IServerSetListener listener) {
    return serverSetListeners.add(listener);
  }

  public boolean removeRepositorySetListener(IServerSetListener listener) {
    return serverSetListeners.remove(listener);
  }

  private void notifyListeners(final int eventType) {
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