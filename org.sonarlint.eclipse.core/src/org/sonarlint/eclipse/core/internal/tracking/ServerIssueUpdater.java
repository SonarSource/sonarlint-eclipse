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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.sonarsource.sonarlint.core.client.api.connected.ConnectedSonarLintEngine;
import org.sonarsource.sonarlint.core.client.api.connected.ServerConfiguration;
import org.sonarsource.sonarlint.core.client.api.connected.ServerIssue;
import org.sonarsource.sonarlint.core.client.api.exceptions.DownloadException;

public class ServerIssueUpdater {

  private static final Logger LOGGER = new Logger();

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

  public void updateFor(ServerConfiguration serverConfiguration, ConnectedSonarLintEngine engine, String localModuleKey, String serverModuleKey, String relativePath) {
    Runnable task = new IssueUpdateRunnable(serverConfiguration, engine, localModuleKey, serverModuleKey, relativePath);
    try {
      this.executorService.submit(task);
    } catch (RejectedExecutionException e) {
      LOGGER.debug("fetch and match server issues rejected for moduleKey=" + serverModuleKey + ", filepath=" + relativePath, e);
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
    private final String relativePath;

    private IssueUpdateRunnable(ServerConfiguration serverConfiguration, ConnectedSonarLintEngine engine, String localModuleKey, String serverModuleKey, String relativePath) {
      this.serverConfiguration = serverConfiguration;
      this.engine = engine;
      this.localModuleKey = localModuleKey;
      this.serverModuleKey = serverModuleKey;
      this.relativePath = relativePath;
    }

    @Override
    public void run() {
      try {
        ConnectedSonarLintEngine.class.getProtectionDomain().getCodeSource().getLocation();
        Iterator<ServerIssue> serverIssues = fetchServerIssues(serverConfiguration, engine, serverModuleKey, relativePath);
        Collection<Trackable> serverIssuesTrackable = toStream(serverIssues).map(ServerIssueTrackable::new).collect(Collectors.toList());

        issueTrackerRegistry.getOrCreate(localModuleKey).matchAndTrackAsBase(relativePath, serverIssuesTrackable);
      } catch (Throwable t) {
        // note: without catching Throwable, any exceptions raised in the thread will not be visible
        console.error("error while fetching and matching server issues", t);
      }
    }

    private <T> Stream<T> toStream(Iterator<T> iterator) {
      Iterable<T> iterable = () -> iterator;
      return StreamSupport.stream(iterable.spliterator(), false);
    }

    private Iterator<ServerIssue> fetchServerIssues(ServerConfiguration serverConfiguration, ConnectedSonarLintEngine engine, String moduleKey, String relativePath) {
      try {
        LOGGER.debug("fetchServerIssues moduleKey=" + moduleKey + ", filepath=" + relativePath);
        return engine.downloadServerIssues(serverConfiguration, moduleKey, relativePath);
      } catch (DownloadException e) {
        console.info(e.getMessage());
        return engine.getServerIssues(moduleKey, relativePath);
      }
    }
  }
}
