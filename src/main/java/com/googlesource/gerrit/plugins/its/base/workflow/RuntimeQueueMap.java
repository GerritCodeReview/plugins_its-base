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

/**
 * Serializes {@link Runnable}s that share a key while letting tasks with different keys run
 * concurrently.
 *
 * <p>A caller {@link #wrap(Object, Runnable) wraps} a task together with its key and hands the
 * returned {@link Runnable} to an executor. Tasks that share a key never run concurrently. A task
 * that arrives while another with the same key is running is queued and executed after it, in
 * arrival order. Tasks with different keys are independent and may run in parallel.
 *
 * <p>Ordering is achieved without blocking executor threads. The first task to arrive for an
 * otherwise idle key runs on the executor thread that picked it up and then drains any tasks that
 * queued up behind it. While that chain is draining, further submissions for the same key hand
 * themselves off to the running chain and return immediately, so at most one thread is ever
 * occupied per key. Once a key's chain empties, its entry is removed, so the map only contains keys
 * with work in flight.
 *
 * @param <K> type of the key that groups tasks which must run sequentially
 */
public class RuntimeQueueMap<K> {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ConcurrentMap<K, CompletableFuture<Task>> map = new ConcurrentHashMap<>();

  /**
   * Wraps a task so that, once the returned {@link Runnable} is executed, it runs sequentially with
   * respect to every other task wrapped under the same key.
   *
   * @param k key whose tasks must run one at a time
   * @param mine task to run
   * @return a {@link Runnable} to hand to the backing executor
   */
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
      if (offerIfNotFirst()) {
        return;
      }

      Task current = this;
      while (true) {
        try {
          current.task.run();
        } catch (Throwable t) {
          logger.atSevere().withCause(t).log("Error executing task for key: %s", key);
        }

        if (map.remove(key, current.nextFuture)) {
          break;
        }
        this.activeTask = current = getAlways(current.nextFuture);
      }
    }

    private boolean offerIfNotFirst() {
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
          // A successor task has already claimed this slot, so the future is guaranteed to
          // complete. Keep waiting for the hand-off. Any interrupt observed here was aimed at the
          // previous task, which has already finished running, so it does not apply to this
          // internal wait and is cleared and ignored.
          Thread.interrupted();
        } catch (ExecutionException e) {
          // should never reach here
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
