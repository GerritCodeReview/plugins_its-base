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

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bounds the number of tasks in flight on an executor, so that a caller submitting tasks is made to
 * wait once the backlog reaches the limit.
 */
class ExecutorThrottle {
  private final Semaphore inflight;

  ExecutorThrottle(int permits) {
    this.inflight = new Semaphore(permits);
  }

  AdmittedTask admit(Runnable task) {
    return new AdmittedTask(task);
  }

  void awaitAdmission(AdmittedTask task) {
    inflight.acquireUninterruptibly();
    task.releaseIfReady();
  }

  final class AdmittedTask implements Runnable {
    private final Runnable task;
    private final AtomicBoolean readyForRelease = new AtomicBoolean();

    private AdmittedTask(Runnable task) {
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
      return task.toString();
    }
  }
}
