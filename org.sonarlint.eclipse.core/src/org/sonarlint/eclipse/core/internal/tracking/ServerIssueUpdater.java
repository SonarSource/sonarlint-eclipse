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
package org.sonarlint.eclipse.core.internal.tracking;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.eclipse.core.resources.IResource;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.MarkerUpdaterJob;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;
import org.sonarsource.sonarlint.core.client.api.exceptions.DownloadException;

public class ServerIssueUpdater {

  private static final Logger LOGGER = new Logger();

  public static final String PATH_SEPARATOR_PATTERN = Pattern.quote(File.separator);

  private static final int THREADS_NUM = 5;
  private static final int QUEUE_LIMIT = 20;

  private final ExecutorService executorService;

  private final IssueTrackerRegistry issueTrackerRegistry;

  private final Console console = new Console();

  public ServerIssueUpdater(IssueTrackerRegistry issueTrackerRegistry) {
    final BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(QUEUE_LIMIT);
    this.executorService = new ThreadPoolExecutor(THREADS_NUM, THREADS_NUM, 0L, TimeUnit.MILLISECONDS, queue);
    this.issueTrackerRegistry = issueTrackerRegistry;
  }

  public void update(ServerConfiguration serverConfiguration, ConnectedSonarLintEngine engine, SonarLintProject project, String localModuleKey, String serverModuleKey,
    Collection<IResource> resources, TriggerType triggerType) {
    Runnable task = new IssueUpdateRunnable(serverConfiguration, engine, project, localModuleKey, serverModuleKey, resources, triggerType);
    try {
      this.executorService.submit(task);
    } catch (RejectedExecutionException e) {
      LOGGER.debug("fetch and match server issues rejected for server moduleKey=" + serverModuleKey, e);
    }
  }

  public void shutdown() {
    List<Runnable> rejected = executorService.shutdownNow();
    if (!rejected.isEmpty()) {
      LOGGER.debug("rejected " + rejected.size() + " pending tasks");
    }
  }

  private class IssueUpdateRunnable implements Runnable {
    private final ServerConfiguration serverConfiguration;
    private final ConnectedSonarLintEngine engine;
    private final String localModuleKey;
    private final String serverModuleKey;
    private final Collection<IResource> resources;
    private final SonarLintProject project;
    private final TriggerType triggerType;

    private IssueUpdateRunnable(ServerConfiguration serverConfiguration, ConnectedSonarLintEngine engine, SonarLintProject project, String localModuleKey,
      String serverModuleKey, Collection<IResource> resources, TriggerType triggerType) {
      this.serverConfiguration = serverConfiguration;
      this.engine = engine;
      this.project = project;
      this.localModuleKey = localModuleKey;
      this.serverModuleKey = serverModuleKey;
      this.resources = resources;
      this.triggerType = triggerType;
    }

    @Override
    public void run() {
      Map<IResource, Collection<Trackable>> trackedIssues = new HashMap<>();
      try {
        for (IResource resource : resources) {
          List<ServerIssue> serverIssues = fetchServerIssues(serverConfiguration, engine, serverModuleKey, resource);
          Collection<Trackable> serverIssuesTrackable = serverIssues.stream().map(ServerIssueTrackable::new).collect(Collectors.toList());
          String relativePath = resource.getProjectRelativePath().toString();
          IssueTracker issueTracker = issueTrackerRegistry.getOrCreate(project.getProject(), localModuleKey);
          Collection<Trackable> tracked = issueTracker.matchAndTrackAsBase(relativePath, serverIssuesTrackable);
          trackedIssues.put(resource, tracked);
        }
        new MarkerUpdaterJob("Update SonarLint markers", project, trackedIssues, triggerType).schedule();
      } catch (Throwable t) {
        // note: without catching Throwable, any exceptions raised in the thread will not be visible
        console.error("error while fetching and matching server issues", t);
      }
    }

    private List<ServerIssue> fetchServerIssues(ServerConfiguration serverConfiguration, ConnectedSonarLintEngine engine, String moduleKey, IResource resource) {
      String fileKey = toFileKey(resource);

      try {
        LOGGER.debug("fetchServerIssues moduleKey=" + moduleKey + ", filepath=" + fileKey);
        return engine.downloadServerIssues(serverConfiguration, moduleKey, fileKey);
      } catch (DownloadException e) {
        console.info(e.getMessage());
        return engine.getServerIssues(moduleKey, fileKey);
      }
    }
  }

  /**
   * Convert relative path to SonarQube file key
   *
   * @param relativePath relative path string in the local OS
   * @return SonarQube file key
   */
  public static String toFileKey(IResource resource) {
    String relativePath = resource.getProjectRelativePath().toString();
    if (File.separatorChar != '/') {
      return relativePath.replaceAll(PATH_SEPARATOR_PATTERN, "/");
    }
    return relativePath;
  }
}
