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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TrackingChangeQueueManagerImpl implements TrackingChangeQueueManager {

  private static final int QUEUE_LIMIT = 20;

  private final ExecutorService executor;

  private final Set<TrackingChangeListener> listeners = new HashSet<>();

  public TrackingChangeQueueManagerImpl() {
    BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(QUEUE_LIMIT);
    this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, workQueue);
  }

  @Override
  public void post(String localModuleKey, String file, Collection<Trackable> trackables) {
    Runnable task = new NotifyListenersRunnable(localModuleKey, file, trackables);
    this.executor.execute(task);
  }

  private class NotifyListenersRunnable implements Runnable {
    private final String localModuleKey;
    private final String file;
    private final Collection<Trackable> trackables;

    public NotifyListenersRunnable(String localModuleKey, String file, Collection<Trackable> trackables) {
      this.localModuleKey = localModuleKey;
      this.file = file;
      this.trackables = trackables;
    }

    @Override
    public void run() {
      for (TrackingChangeListener listener : listeners) {
        listener.onTrackingChange(localModuleKey, file, trackables);
      }
    }
  }

  @Override
  public void subscribe(TrackingChangeListener listener) {
    listeners.add(listener);
  }

  @Override
  public void shutdown() {
    executor.shutdownNow();
  }

}
