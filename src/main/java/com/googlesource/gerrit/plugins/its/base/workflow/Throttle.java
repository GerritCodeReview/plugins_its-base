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
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bounds the number of tasks in flight on an executor, so that a caller submitting tasks is made to
 * wait once the backlog reaches the limit.
 */
public class Throttle {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Semaphore inflight;

  Throttle(int permits) {
    this.inflight = new Semaphore(permits);
  }

  void execute(Executor executor, Runnable task) {
    ThrottledTask throttledTask = new ThrottledTask(task);
    try {
      executor.execute(throttledTask);
    } catch (RuntimeException e) {
      logger.atWarning().withCause(e).log("Failed to queue ITS task %s", task);
      return;
    }
    inflight.acquireUninterruptibly();
    throttledTask.releaseIfReady();
  }

  private final class ThrottledTask implements Runnable {
    private final Runnable task;
    private final AtomicBoolean readyForRelease = new AtomicBoolean();

    private ThrottledTask(Runnable task) {
      this.task = task;
    }

    @Override
    public void run() {
      try {
        task.run();
      } finally {
        releaseIfReady();
      }
    }

    private void releaseIfReady() {
      if (readyForRelease.getAndSet(true)) {
        inflight.release();
      }
    }

    @Override
    public String toString() {
      try {
        try {
          return task.toString();
        } catch (Exception e) {
          logger.atWarning().withCause(e).log("Cannot describe task");
          return task.getClass().getName();
        }
      } catch (Exception e) {
        return "unknown task";
      }
    }
  }
}
