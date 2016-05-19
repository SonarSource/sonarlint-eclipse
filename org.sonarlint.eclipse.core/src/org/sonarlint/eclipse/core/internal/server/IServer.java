/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.internal.server;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteModule;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;

public interface IServer {

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

  String getUpdateDate();

  String getServerVersion();

  String getSonarLintEngineState();

  void update(IProgressMonitor monitor);

  IStatus testConnection(String username, String password);

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
  void addServerListener(IServerListener listener);

  /**
   * Removes the given server state listener from this server. Has no
   * effect if the listener is not registered.
   * 
   * @param listener the listener
   * @see #addServerListener(IServerListener)
   */
  void removeServerListener(IServerListener listener);

  TextSearchIndex<RemoteModule> getModuleIndex();

  void startAnalysis(ConnectedAnalysisConfiguration config, IssueListener issueListener);

  String getHtmlRuleDescription(String ruleKey);

  void updateProject(String moduleKey);

  boolean isUpdated();

  List<SonarLintProject> getBoundProjects();

  void notifyAllListeners();

  boolean isUpdating();

  void updateConfig(String url, String username, String password);

}
