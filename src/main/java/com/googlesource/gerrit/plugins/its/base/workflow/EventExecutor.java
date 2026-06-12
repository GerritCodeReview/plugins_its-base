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

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.git.WorkQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import javax.annotation.Nonnull;

/**
 * Thread pool that runs event tasks. A non-positive pool size disables the pool, so tasks passed to
 * {@link #execute(Runnable)} run synchronously on the calling thread.
 */
public class EventExecutor implements Executor, LifecycleListener {
  private final int poolSize;
  private final ScheduledExecutorService executor;

  public EventExecutor(WorkQueue workQueue, int poolSize, String queueName) {
    this.poolSize = poolSize;
    this.executor = poolSize > 0 ? workQueue.createQueue(poolSize, queueName) : null;
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
