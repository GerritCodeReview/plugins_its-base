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
import com.google.gerrit.server.config.SitePaths;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

/**
 * Thread pool that runs ITS event tasks. The pool size is read from the {@code
 * plugin.<plugin>.executionThreadPoolSize} setting in {@code etc/gerrit.config}. A non-positive
 * size disables asynchronous processing ({@link #isAsync()} returns {@code false}). Tasks passed to
 * {@link #execute(Runnable)} then run synchronously on the calling thread.
 */
@Singleton
public class EventExecutor implements Executor, LifecycleListener {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String EXECUTION_THREAD_POOL_SIZE = "executionThreadPoolSize";
  static final int DEFAULT_EXECUTION_THREAD_POOL_SIZE = 0;

  private static final String PLUGIN_SECTION = "plugin";

  private final int poolSize;
  private final ScheduledExecutorService executor;

  @Inject
  EventExecutor(WorkQueue workQueue, SitePaths sitePaths, @PluginName String pluginName) {
    this.poolSize = getExecutionThreadPoolSize(sitePaths, pluginName);
    this.executor = poolSize > 0 ? workQueue.createQueue(poolSize, pluginName) : null;
  }

  public boolean isAsync() {
    return executor != null;
  }

  public int getPoolSize() {
    return poolSize;
  }

  @Override
  public void execute(Runnable task) {
    if (executor == null) {
      task.run();
      return;
    }
    executor.execute(task);
  }

  @Override
  public void start() {}

  @Override
  public void stop() {
    if (executor != null) {
      executor.shutdown();
    }
  }

  private static int getExecutionThreadPoolSize(SitePaths sitePaths, String pluginName) {
    FileBasedConfig gerritConfig =
        new FileBasedConfig(sitePaths.gerrit_config.toFile(), FS.DETECTED);
    try {
      gerritConfig.load();
    } catch (IOException | ConfigInvalidException e) {
      logger.atWarning().withCause(e).log(
          "Cannot read %s. Disabling asynchronous ITS event processing", sitePaths.gerrit_config);
      return DEFAULT_EXECUTION_THREAD_POOL_SIZE;
    }
    return gerritConfig.getInt(
        PLUGIN_SECTION, pluginName, EXECUTION_THREAD_POOL_SIZE, DEFAULT_EXECUTION_THREAD_POOL_SIZE);
  }
}
