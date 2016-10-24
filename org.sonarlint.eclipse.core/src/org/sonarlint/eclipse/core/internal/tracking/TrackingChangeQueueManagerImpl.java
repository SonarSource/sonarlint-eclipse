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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TrackingChangeQueueManagerImpl implements TrackingChangeQueueManager {

  private static final int QUEUE_LIMIT = 20;

  private final ExecutorService executor;

  private final List<TrackingChangeListener> listeners = new ArrayList<>();

  public TrackingChangeQueueManagerImpl() {
    BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(QUEUE_LIMIT);
    this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, workQueue);
  }

  @Override
  public void post(String moduleKey, String file, Collection<MutableTrackable> issues) {
    Runnable task = new NotifyListenersRunnable(moduleKey, file, issues);
    this.executor.execute(task);
  }

  private class NotifyListenersRunnable implements Runnable {
    private final String moduleKey;
    private final String file;
    private final Collection<MutableTrackable> issues;

    public NotifyListenersRunnable(String moduleKey, String file, Collection<MutableTrackable> issues) {
      this.moduleKey = moduleKey;
      this.file = file;
      this.issues = issues;
    }

    @Override
    public void run() {
      for (TrackingChangeListener listener : listeners) {
        listener.onTrackingChange(moduleKey, file, issues);
      }
    }
  }

  @Override
  public void subscribe(TrackingChangeListener listener) {
    listeners.add(listener);
  }

  @Override
  public void shutdown() {
    // TODO log this?
    executor.shutdownNow();
  }

}
