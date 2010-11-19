package org.sonar.ide.eclipse.core;

import java.util.Collection;
import java.util.List;

import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;

/**
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ISonarServersManager {

  Collection<SonarServer> getServers();

  // TODO From old implementation, so should be reviewed :

  public List<Host> getHosts();

  void removeServer(String host);

  void addServer(String location, String username, String password);

  /**
   * @deprecated since 0.3 use {@link #findServer(String)} instead
   */
  Host createServer(String url);

  Host findServer(String host);

  Sonar getSonar(String url);

  boolean testSonar(String url, String user, String password) throws Exception;

}
