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
import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

/**
 * Runs ITS event tasks. The pool size is read from the {@code plugin.<plugin>.threadPoolSize}
 * setting in {@code etc/gerrit.config}. A non-positive size disables asynchronous processing,
 * meaning tasks will then run synchronously on the calling (event-dispatch) thread.
 *
 * <p>When asynchronous processing is enabled, tasks run on a core {@link WorkQueue} pool. Before
 * running, a task with an ordering key acquires the {@link KeyLock} for that key, ensuring that two
 * tasks with the same key never run concurrently, while tasks with different keys can run on
 * different pool threads in parallel. Tasks submitted without an ordering key are not serialized.
 *
 * <p>To prevent the queue from becoming unbounded, a maximum of {@value #MAX_TASKS_IN_FLIGHT} tasks
 * are allowed to be in-flight at any given time. Once this maximum is reached, subsequent tasks are
 * dropped and logged.
 */
@Singleton
public class EventExecutor implements LifecycleListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String THREAD_POOL_SIZE = "threadPoolSize";
  static final int DEFAULT_THREAD_POOL_SIZE = 0;
  static final int MAX_TASKS_IN_FLIGHT = 300;

  private static final String PLUGIN_SECTION = "plugin";

  private final ScheduledExecutorService executor;

  private final Semaphore admissions = new Semaphore(MAX_TASKS_IN_FLIGHT);
  private final KeyLock<String> keyLock = new KeyLock<>();

  @Inject
  EventExecutor(WorkQueue workQueue, SitePaths sitePaths, @PluginName String pluginName) {
    int threadPoolSize = getThreadPoolSize(sitePaths, pluginName);
    this.executor = threadPoolSize > 0 ? workQueue.createQueue(threadPoolSize, pluginName) : null;
  }

  public void execute(@Nullable String orderingKey, Runnable task) {
    if (executor == null) {
      task.run();
      return;
    }
    if (!admissions.tryAcquire()) {
      logger.atSevere().log(
          "ITS event backlog full (%d tasks in flight). Dropping event task %s",
          MAX_TASKS_IN_FLIGHT, task);
      return;
    }
    submit(orderingKey, task);
  }

  @Override
  public void start() {}

  @Override
  public void stop() {
    if (executor != null) {
      executor.shutdown();
    }
  }

  private void submit(@Nullable String orderingKey, Runnable task) {
    try {
      executor.execute(new QueuedTask(orderingKey, task));
    } catch (RuntimeException ignored) {
      admissions.release();
    }
  }

  private final class QueuedTask implements Runnable {
    @Nullable private final String orderingKey;
    private final Runnable task;

    QueuedTask(@Nullable String orderingKey, Runnable task) {
      this.orderingKey = orderingKey;
      this.task = task;
    }

    @Override
    public void run() {
      try {
        if (orderingKey != null) {
          keyLock.run(orderingKey, task);
        } else {
          task.run();
        }
      } finally {
        admissions.release();
      }
    }

    @Override
    public String toString() {
      return task.toString();
    }
  }

  private static int getThreadPoolSize(SitePaths sitePaths, String pluginName) {
    FileBasedConfig gerritConfig =
        new FileBasedConfig(sitePaths.gerrit_config.toFile(), FS.DETECTED);
    try {
      gerritConfig.load();
    } catch (IOException | ConfigInvalidException e) {
      logger.atWarning().withCause(e).log(
          "Cannot read %s. Disabling asynchronous ITS event processing", sitePaths.gerrit_config);
      return DEFAULT_THREAD_POOL_SIZE;
    }
    return gerritConfig.getInt(
        PLUGIN_SECTION, pluginName, THREAD_POOL_SIZE, DEFAULT_THREAD_POOL_SIZE);
  }
}
