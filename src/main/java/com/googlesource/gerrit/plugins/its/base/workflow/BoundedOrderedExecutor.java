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
import java.util.concurrent.Executor;

/**
 * Runs ITS event tasks on the {@link LifecycleExecutor} pool, bounding the backlog and serializing
 * tasks that share an ordering key. Tasks with the same key run one at a time in submission order;
 * tasks with different keys (or a {@code null} key) run in parallel.
 */
public class BoundedOrderedExecutor {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final LifecycleExecutor executor;
  private final ExecutorThrottle throttle;
  private final RuntimeQueueMap<Object> runtimeQueueMap;

  public BoundedOrderedExecutor(LifecycleExecutor executor) {
    this.executor = executor;
    int poolSize = executor.getPoolSize();
    this.throttle = poolSize > 0 ? new ExecutorThrottle(poolSize) : null;
    this.runtimeQueueMap = poolSize > 0 ? new RuntimeQueueMap<>() : null;
  }

  public void execute(@Nullable Object orderingKey, Runnable task) {
    if (throttle == null) {
      task.run();
      return;
    }

    Executor taskRunner =
        orderingKey == null
            ? executor
            : runnable -> executor.execute(runtimeQueueMap.wrap(orderingKey, runnable));
    try {
      throttle.submit(taskRunner, task);
    } catch (RuntimeException e) {
      logger.atWarning().withCause(e).log("Failed to queue ITS task %s", task);
    }
  }
}
