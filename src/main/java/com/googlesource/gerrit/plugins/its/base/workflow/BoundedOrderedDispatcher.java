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

import java.util.Optional;
import java.util.concurrent.Executor;
import javax.annotation.Nonnull;

/**
 * Runs ITS event tasks on the {@link LifecycleThreadPool}, bounding the backlog and serializing
 * tasks that share an ordering key. Tasks that implement {@link OrderedTask} and return the same
 * key run one at a time in submission order and tasks that are not {@link OrderedTask} (or return
 * an empty key) run in parallel.
 */
public class BoundedOrderedDispatcher implements Executor {
  private final LifecycleThreadPool pool;
  private final Throttle throttle;
  private final RuntimeQueueMap<Object> runtimeQueueMap;

  public BoundedOrderedDispatcher(LifecycleThreadPool pool, int maxInFlight) {
    this.pool = pool;
    this.throttle = new Throttle(maxInFlight);
    this.runtimeQueueMap = new RuntimeQueueMap<>();
  }

  @Override
  public void execute(@Nonnull Runnable task) {
    Optional<?> orderingKey =
        task instanceof OrderedTask orderedTask ? orderedTask.key() : Optional.empty();
    Executor taskRunner =
        orderingKey.isEmpty()
            ? pool
            : runnable -> pool.execute(runtimeQueueMap.wrap(orderingKey.get(), runnable));
    throttle.execute(taskRunner, task);
  }

  public interface OrderedTask extends Runnable {
    Optional<?> key();
  }
}
