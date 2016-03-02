package org.sonarlint.eclipse.core.internal.server;

import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

public class Server implements IServer {

  private final String id;
  private final String name;
  private final String host;
  private final String username;
  private final String password;

  public Server(String id, String name, String host, String username, String password) {
    this.id = id;
    this.name = name;
    this.host = host;
    this.username = username;
    this.password = password;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getHost() {
    return host;
  }

  @Override
  public boolean hasAuth() {
    return StringUtils.isNotBlank(getUsername());
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public String getSyncState() {
    return "TODO";
  }

  @Override
  public void delete() {
    SonarLintCorePlugin.getDefault().removeServer(this);
  }

}
