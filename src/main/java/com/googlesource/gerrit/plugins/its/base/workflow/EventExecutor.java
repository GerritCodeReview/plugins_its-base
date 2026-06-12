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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.googlesource.gerrit.plugins.its.base.ExecutionThreadPoolSize;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nonnull;

/**
 * Thread pool that runs event tasks. The pool size (see {@link ExecutionThreadPoolSize}) is
 * injected. A non-positive size disables the pool, so tasks passed to {@link #execute(Runnable)}
 * run synchronously on the calling thread.
 */
@Singleton
public class EventExecutor implements Executor, LifecycleListener {
  private final int poolSize;
  private final ScheduledExecutorService executor;

  @Inject
  public EventExecutor(
      WorkQueue workQueue, @ExecutionThreadPoolSize int poolSize, @PluginName String pluginName) {
    this.poolSize = poolSize;
    this.executor = poolSize > 0 ? workQueue.createQueue(poolSize, pluginName) : null;
  }

  public int getPoolSize() {
    return poolSize;
  }

  @Override
  public void execute(@Nonnull Runnable task) {
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
}
