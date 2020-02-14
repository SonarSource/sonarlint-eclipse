/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.analysis.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine.State;
import org.sonarsource.sonarlint.core.client.api.connected.ProjectBinding;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteProject;
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

  @CheckForNull
  String getOrganization();

  boolean hasAuth();

  void delete();

  String getUpdateDate();

  String getServerVersion();

  String getSonarLintStorageStateLabel();

  void updateStorage(IProgressMonitor monitor);

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

  TextSearchIndex<RemoteProject> computeProjectIndex();

  Map<String, RemoteProject> getCachedRemoteProjects();

  Optional<RemoteProject> getRemoteProject(String projectKey, IProgressMonitor monitor);

  AnalysisResults runAnalysis(ConnectedAnalysisConfiguration config, IssueListener issueListener, IProgressMonitor monitor);

  RuleDetails getRuleDescription(String ruleKey);

  void updateProjectStorage(String moduleKey, IProgressMonitor monitor);

  State getStorageState();

  List<ISonarLintProject> getBoundProjects();

  void notifyAllListeners();

  void updateConfig(String url, @Nullable String organization, String username, String password, boolean notificationsEnabled);

  void checkForUpdates(IProgressMonitor progress);

  boolean hasUpdates();

  void updateProjectList(IProgressMonitor monitor);

  boolean isSonarCloud();

  boolean areNotificationsEnabled();

  List<ISonarLintFile> getServerFileExclusions(ProjectBinding binding, Collection<ISonarLintFile> files, Predicate<ISonarLintFile> testFilePredicate);

}
