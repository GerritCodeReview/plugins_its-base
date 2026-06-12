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

import com.google.gerrit.common.Nullable;
import java.util.concurrent.Executor;

/**
 * {@link OrderedExecutor} that serializes tasks sharing an ordering key through {@link
 * RuntimeQueueMap} before handing them to a delegate {@link Executor}.
 *
 * <p>Tasks submitted with a non-null key are wrapped so that the first task for a key drains any
 * same-key tasks that arrive while it runs, keeping them in submission order and on one thread at a
 * time, while tasks with different keys run in parallel. Tasks submitted with a {@code null} key
 * are passed through unwrapped and are not serialized.
 */
public class SerializingExecutor implements OrderedExecutor {
  private final Executor executor;
  private final RuntimeQueueMap<Object> runtimeQueueMap = new RuntimeQueueMap<>();

  public SerializingExecutor(Executor executor) {
    this.executor = executor;
  }

  @Override
  public void execute(@Nullable Object orderingKey, Runnable task) {
    executor.execute(orderingKey != null ? runtimeQueueMap.wrap(orderingKey, task) : task);
  }
}
