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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.Semaphore;

/**
 * Runs ITS event tasks on the {@link EventExecutor} pool, bounding the backlog and serializing
 * tasks that share an ordering key. Tasks with the same key run one at a time in submission order;
 * tasks with different keys (or a {@code null} key) run in parallel.
 */
@Singleton
public class OrderedExecutor {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final EventExecutor executor;
  private final Semaphore admissions;
  private final RuntimeQueueMap<Object> runtimeQueueMap;

  @Inject
  OrderedExecutor(EventExecutor executor) {
    this.executor = executor;
    int poolSize = executor.getPoolSize();
    this.admissions = poolSize > 0 ? new Semaphore(poolSize) : null;
    this.runtimeQueueMap = poolSize > 0 ? new RuntimeQueueMap<>() : null;
  }

  public void execute(@Nullable Object orderingKey, Runnable task) {
    if (admissions == null) {
      task.run();
      return;
    }
    admissions.acquireUninterruptibly();
    Runnable admitted = new AdmittedTask(task);
    try {
      executor.execute(
          orderingKey != null ? runtimeQueueMap.wrap(orderingKey, admitted) : admitted);
    } catch (RuntimeException e) {
      admissions.release();
      logger.atWarning().withCause(e).log("Failed to queue ITS task %s", task);
    }
  }

  private final class AdmittedTask implements Runnable {
    private final Runnable task;

    AdmittedTask(Runnable task) {
      this.task = task;
    }

    @Override
    public void run() {
      try {
        task.run();
      } finally {
        admissions.release();
      }
    }

    @Override
    public String toString() {
      return task.toString();
    }
  }
}
