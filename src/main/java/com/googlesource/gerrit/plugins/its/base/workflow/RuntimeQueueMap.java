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

  private final ConcurrentMap<K, CompletableFuture<Task>> map = new ConcurrentHashMap<>();

  public Runnable wrap(K k, Runnable mine) {
    return new Task(k, mine);
  }

  private class Task implements Runnable {
    private final K key;
    private final Runnable task;

    final CompletableFuture<Task> nextFuture = new CompletableFuture<>();
    private volatile Task activeTask;

    Task(K key, Runnable task) {
      this.key = key;
      this.task = task;
      this.activeTask = this;
    }

    @Override
    public void run() {
      if (offer()) {
        return;
      }

      Task current = this;
      while (true) {
        try {
          current.task.run();
        } catch (Throwable t) {
          logger.atSevere().log("Error executing task for key: " + key, t);
        }

        if (map.remove(key, current.nextFuture)) {
          break;
        }
        current = getAlways(current.nextFuture);
        this.activeTask = current;
      }
    }

    private boolean offer() {
      CompletableFuture<Task> prevFuture = map.put(key, nextFuture);
      if (prevFuture == null) {
        return false;
      }

      prevFuture.complete(this);
      return true;
    }

    private Task getAlways(CompletableFuture<Task> future) {
      while (true) {
        try {
          return future.get();
        } catch (InterruptedException e) {
          Thread.interrupted();
        } catch (ExecutionException e) {
          logger.atSevere().withCause(e).log("Pipeline future failed unexpectedly");
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public String toString() {
      return activeTask.task.toString();
    }
  }
}
