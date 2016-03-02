package org.sonarlint.eclipse.core.internal.server;

public interface IServer {

  /**
   * Returns the displayable name for this server.
   * <p>
   * Note that this name is appropriate for the current locale.
   * </p>
   *
   * @return a displayable name
   */
  String getName();

  /**
   * Returns the id of this server.
   * Each server (of a given type) has a distinct id, fixed for
   * its lifetime. Ids are intended to be used internally as keys;
   * they are not intended to be shown to end users.
   * 
   * @return the server id
   */
  String getId();

  /**
   * Returns the host for the server.
   * The format of the host can be either a qualified or unqualified hostname,
   * or an IP address and must conform to RFC 2732.
   * 
   * @return a host string conforming to RFC 2732
   * @see java.net.URL#getHost()
   */
  String getHost();

  boolean hasAuth();

  void delete();

  String getUsername();

  String getPassword();

  String getSyncState();

}
