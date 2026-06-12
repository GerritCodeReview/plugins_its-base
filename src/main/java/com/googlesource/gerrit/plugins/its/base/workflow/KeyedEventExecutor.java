// Copyright (C) 2026 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.its.base.workflow;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import org.eclipse.jgit.lib.Config;

/**
 * {@link EventExecutor} that runs tasks on a shared thread pool while serializing the tasks that
 * share an ordering key.
 *
 * <p>A single {@link WorkQueue} pool of {@code plugin.@PLUGIN@.threadPoolSize} threads runs every
 * task. For each ordering key a FIFO queue ensures that at most one of its tasks runs at a time, in
 * submission order; while a key has pending tasks one pool thread drains them, and the queue is
 * discarded once empty. Tasks with different keys can run on different pool threads concurrently,
 * so up to {@code threadPoolSize} keys are processed in parallel.
 *
 * <p>This keeps the events of a single change in order (so their issue tracker state transitions
 * are never reordered) while letting unrelated changes be processed in parallel.
 */
@Singleton
public class KeyedEventExecutor implements EventExecutor, LifecycleListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String CONFIG_THREAD_POOL_SIZE = "threadPoolSize";
  private static final int DEFAULT_THREAD_POOL_SIZE = 1;

  private final ExecutorService executor;
  private final ConcurrentHashMap<String, KeyQueue> queuesByKey = new ConcurrentHashMap<>();

  @Inject
  KeyedEventExecutor(
      WorkQueue workQueue, @PluginName String pluginName, @GerritServerConfig Config gerritConfig) {
    int size =
        gerritConfig.getInt(
            "plugin", pluginName, CONFIG_THREAD_POOL_SIZE, DEFAULT_THREAD_POOL_SIZE);
    if (size < 1) {
      logger.atWarning().log(
          "Ignoring invalid plugin.%s.%s=%d, using %d instead",
          pluginName, CONFIG_THREAD_POOL_SIZE, size, DEFAULT_THREAD_POOL_SIZE);
      size = DEFAULT_THREAD_POOL_SIZE;
    }
    this.executor = workQueue.createQueue(size, pluginName);
  }

  @Override
  public void execute(String orderingKey, Runnable task) {
    while (true) {
      KeyQueue queue = queuesByKey.computeIfAbsent(orderingKey, KeyQueue::new);
      if (queue.offer(task)) {
        return;
      }
    }
  }

  @Override
  public void start() {}

  @Override
  public void stop() {
    executor.shutdown();
  }

  private class KeyQueue implements Runnable {
    private enum State {
      /** Live, but no pool thread is draining it yet; the next {@link #offer} schedules one. */
      IDLE,
      /** A pool thread is draining the queued tasks (or is about to start). */
      DRAINING,
      /** Drained and removed from {@link #queuesByKey}. Callers must use a fresh queue. */
      REMOVED
    }

    private final String key;
    private final Deque<Runnable> tasks = new ArrayDeque<>();
    private State state = State.IDLE;

    KeyQueue(String key) {
      this.key = key;
    }

    /**
     * Appends a task to the queue.
     *
     * @return {@code false} if this queue has already drained and removed itself from the map, in
     *     which case the caller must obtain a fresh queue and retry.
     */
    boolean offer(Runnable task) {
      synchronized (this) {
        if (state == State.REMOVED) {
          return false;
        }
        tasks.add(task);
        if (state == State.DRAINING) {
          // A pool thread is already draining this queue and will pick up the task.
          return true;
        }
        state = State.DRAINING;
      }
      // Start draining outside the lock so we never hold the monitor while using the executor.
      executor.execute(this);
      return true;
    }

    @Override
    public void run() {
      while (true) {
        Runnable task;
        synchronized (this) {
          task = tasks.poll();
          if (task == null) {
            state = State.REMOVED;
            queuesByKey.remove(key, this);
            return;
          }
        }
        try {
          task.run();
        } catch (RuntimeException e) {
          logger.atSevere().withCause(e).log("Error running ITS event task for key %s", key);
        }
      }
    }
  }
}
