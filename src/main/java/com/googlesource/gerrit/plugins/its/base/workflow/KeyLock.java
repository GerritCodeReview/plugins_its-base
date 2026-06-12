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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

/**
 * Mutual-exclusion lock keyed by an arbitrary value, implemented by chaining a {@link
 * CountDownLatch} per key.
 *
 * <p>Each acquisition installs a fresh latch as the tail of the key's chain and, when a previous
 * holder exists, blocks on that holder's latch until it is released. Releasing counts down the
 * holder's own latch (unblocking the next waiter) and, while it is still the tail, drops the key
 * from the map so an idle key retains nothing.
 */
public class KeyLock<K> {
  private final ConcurrentMap<K, CountDownLatch> latchByKey = new ConcurrentHashMap<>();

  /**
   * Runs {@code task} while holding the lock for {@code key}, blocking the calling thread until the
   * lock is free.
   *
   * <p>If the thread is interrupted while waiting for the lock, {@code task} is skipped and the
   * interrupt status is restored.
   */
  public void run(final K key, final Runnable task) {
    try (AutoCloseable ignored = lock(key)) {
      task.run();
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    } catch (Exception failure) {
      throw new RuntimeException(failure);
    }
  }

  private AutoCloseable lock(final K key) throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final CountDownLatch predecessor = latchByKey.put(key, latch);
    awaitPredecessor(key, latch, predecessor);
    return () -> release(key, latch);
  }

  private void awaitPredecessor(
      final K key, final CountDownLatch latch, final CountDownLatch predecessor)
      throws InterruptedException {
    if (predecessor == null) {
      return;
    }
    try {
      predecessor.await();
    } catch (InterruptedException interrupted) {
      release(key, latch);
      throw interrupted;
    }
  }

  private void release(final K key, final CountDownLatch latch) {
    latchByKey.remove(key, latch);
    latch.countDown();
  }
}
