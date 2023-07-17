/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.engine.connected;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.analysis.api.AnalysisResults;
import org.sonarsource.sonarlint.core.client.api.common.analysis.IssueListener;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedAnalysisConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ProjectBranches;
import org.sonarsource.sonarlint.core.client.api.util.TextSearchIndex;
import org.sonarsource.sonarlint.core.serverapi.component.ServerProject;
import org.sonarsource.sonarlint.core.serverconnection.ProjectBinding;

public interface IConnectedEngineFacade {

  /**
   * Returns the id of this connection.
   * Each connection has a distinct id, fixed for
   * its lifetime.
   *
   * @return the connection id
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

  @Nullable
  String getOrganization();

  boolean hasAuth();

  void delete();

  /**
   * Adds the given state listener to this engine.
   * Once registered, a listener starts receiving notification of
   * state changes to this engine. The listener continues to receive
   * notifications until it is removed.
   * Has no effect if an identical listener is already registered.
   *
   * @param listener the engine listener
   * @see #removeConnectedEngineListener(IConnectedEngineFacadeListener)
   */
  void addConnectedEngineListener(IConnectedEngineFacadeListener listener);

  /**
   * Removes the given state listener from this engine. Has no
   * effect if the listener is not registered.
   *
   * @param listener the listener
   * @see #addConnectedEngineListener(IConnectedEngineFacadeListener)
   */
  void removeConnectedEngineListener(IConnectedEngineFacadeListener listener);

  TextSearchIndex<ServerProject> computeProjectIndex();

  Optional<ServerProject> getCachedRemoteProject(String projectKey);

  @Nullable
  AnalysisResults runAnalysis(ConnectedAnalysisConfiguration config, IssueListener issueListener, IProgressMonitor monitor);

  void updateProjectStorage(String projectKey, IProgressMonitor monitor);

  Set<String> getBoundProjectKeys();

  List<ISonarLintProject> getBoundProjects();

  List<ISonarLintProject> getBoundProjects(String projectKey);

  void notifyAllListenersStateChanged();

  void updateConfig(String url, @Nullable String organization, String username, String password, boolean notificationsDisabled);

  void updateProjectList(IProgressMonitor monitor);

  boolean isSonarCloud();

  boolean areNotificationsDisabled();

  List<ISonarLintFile> getServerFileExclusions(ProjectBinding binding, Collection<ISonarLintFile> files, Predicate<ISonarLintFile> testFilePredicate);

  void scheduledSync(Set<String> projectKeys, IProgressMonitor monitor);

  void manualSync(Set<String> projectKeysToUpdate, IProgressMonitor monitor);

  ProjectBranches getServerBranches(String projectKey);

  void subscribeForEventsForBoundProjects();

  void addServerEventListener(IServerEventListener listener);

  void removeServerEventListener(IServerEventListener listener);
}
