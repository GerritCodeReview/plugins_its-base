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
import java.util.concurrent.Semaphore;

/**
 * {@link OrderedExecutor} that bounds the number of in-flight tasks with a {@link Semaphore},
 * providing backpressure over a delegate executor.
 */
public class AdmissionControl implements OrderedExecutor {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final OrderedExecutor delegate;
  private final Semaphore admissions;

  public AdmissionControl(OrderedExecutor delegate, int permits) {
    this.delegate = delegate;
    this.admissions = new Semaphore(permits);
  }

  @Override
  public void execute(@Nullable Object orderingKey, Runnable task) {
    admissions.acquireUninterruptibly();
    try {
      delegate.execute(orderingKey, new AdmittedTask(task));
    } catch (RuntimeException e) {
      admissions.release();
      logger.atWarning().withCause(e).log("Failed to queue ITS task %s", task);
    }
  }

  /**
   * Runs a task and releases its admission slot afterwards. Same-key tasks may be drained by
   * another task's thread (see {@link RuntimeQueueMap}), so the slot is released here, when the
   * task actually runs, rather than when it is submitted.
   */
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
