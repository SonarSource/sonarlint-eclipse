package org.sonar.ide.eclipse.views.model;

import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.ResourceQuery;

/**
 * @author Jérémie Lagarde
 */
public class TreeServer extends TreeParent {

  private final Sonar server;
  private final Host host;

  public TreeServer(Host host) {
    super(null);
    this.host = host;
    this.server = Sonar.create(host.getHost());
  }

  @Override
  public String getName() {
    return host.getHost();
  }


  @Override
  public Sonar getServer() {
    return server;
  }


  @Override
  protected String getRemoteRootURL() {
    return host.getHost();
  }


  @Override
  protected ResourceQuery createResourceQuery() {
    return new ResourceQuery();
  }

  @Override
  public String getRemoteURL() {
    return getRemoteRootURL();
  }
}
