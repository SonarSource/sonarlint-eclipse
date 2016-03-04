package org.sonarlint.eclipse.core.internal.server;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.sonarsource.sonarlint.core.client.api.analysis.AnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteModule;

public interface IServer {

  enum State {
    STOPPED,
    STARTING,
    SYNCING,
    STARTED_NOT_SYNCED,
    STARTED_SYNCED
  }

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

  /**
   * Only if {@link #getState()} returns {@link State#STARTED_SYNCED}
   */
  String getSyncDate();

  /**
   * Only if {@link #getState()} returns {@link State#STARTED_SYNCED}
   */
  String getServerVersion();

  State getState();

  void sync(IProgressMonitor monitor);

  IStatus testConnection();

  /**
   * Adds the given server state listener to this server.
   * Once registered, a listener starts receiving notification of 
   * state changes to this server. The listener continues to receive
   * notifications until it is removed.
   * Has no effect if an identical listener is already registered.
   *
   * @param listener the server listener
   * @see #removeServerListener(IServerListener)
   */
  public void addServerListener(IServerListener listener);

  /**
   * Removes the given server state listener from this server. Has no
   * effect if the listener is not registered.
   * 
   * @param listener the listener
   * @see #addServerListener(IServerListener)
   */
  public void removeServerListener(IServerListener listener);

  List<RemoteModule> findModules(String keyOrPartialName);

  void startAnalysis(AnalysisConfiguration config, IssueListener issueListener);

  String getHtmlRuleDescription(String ruleKey);

  void syncProject(String moduleKey);

  void setVerbose(boolean verbose);

}
