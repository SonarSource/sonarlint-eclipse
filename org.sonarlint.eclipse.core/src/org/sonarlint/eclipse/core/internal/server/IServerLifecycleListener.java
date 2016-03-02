package org.sonarlint.eclipse.core.internal.server;

/**
 * Listener interface for changes to servers.
 * <p>
 * This interface is fired whenever a server is added, modified, or removed.
 * All events are fired post-change, so that all server tools API called as a
 * result of the event will return the updated results. (for example, on
 * serverAdded the new server will be in the global list of servers
 * ({@link ServerCore#getServers()}), and on serverRemoved the server will
 * not be in the list.
 * </p>
 * 
 * @see ServerCore
 * @see IServer
 * @since 1.0
 */
public interface IServerLifecycleListener {
  /**
   * A new server has been created.
   *
   * @param server the new server
   */
  public void serverAdded(IServer server);

  /**
   * An existing server has been updated or modified.
   *
   * @param server the modified server
   */
  public void serverChanged(IServer server);

  /**
   * A existing server has been removed.
   *
   * @param server the removed server
   */
  public void serverRemoved(IServer server);
}
