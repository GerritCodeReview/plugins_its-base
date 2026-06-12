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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

public class RuntimeQueueMap<K> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ConcurrentMap<K, CompletableFuture<TaskRunner>> map = new ConcurrentHashMap<>();

  public Runnable wrap(K k, Runnable mine) {
    return new TaskRunner(k, mine);
  }

  private class TaskRunner implements Runnable {
    private final K key;
    private final Runnable task;

    final CompletableFuture<TaskRunner> nextFuture = new CompletableFuture<>();
    private volatile TaskRunner activeTask;

    TaskRunner(K key, Runnable task) {
      this.key = key;
      this.task = task;
      this.activeTask = this;
    }

    @Override
    public void run() {
      if (offer()) {
        return;
      }

      TaskRunner current = this;
      boolean processing = true;

      while (processing) {
        try {
          current.task.run();
        } catch (Throwable t) {
          logger.atSevere().log("Error executing task for key: " + key, t);
        }

        if (map.remove(key, current.nextFuture)) {
          processing = false;
        } else {
          current = getAlways(current.nextFuture);

          this.activeTask = current;
        }
      }
    }

    private boolean offer() {
      CompletableFuture<TaskRunner> prevFuture = map.put(key, nextFuture);
      if (prevFuture == null) {
        return false;
      }

      prevFuture.complete(this);
      return true;
    }

    private TaskRunner getAlways(CompletableFuture<TaskRunner> future) {
      boolean interrupted = false;
      try {
        while (true) {
          try {
            return future.get();
          } catch (InterruptedException e) {
            interrupted = true;
          } catch (ExecutionException e) {
            logger.atSevere().withCause(e).log("Pipeline future failed unexpectedly");
            throw new RuntimeException(e);
          }
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }

    @Override
    public String toString() {
      return activeTask.task.toString();
    }
  }
}
