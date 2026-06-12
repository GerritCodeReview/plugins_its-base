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

/** Runs ITS event-handling tasks on background threads. */
public interface EventExecutor {

  /**
   * Schedules a task for asynchronous execution.
   *
   * <p>Tasks submitted with equal {@code orderingKey}s are guaranteed to run sequentially, in the
   * order they were submitted. Tasks with different keys may run concurrently. Callers use the key
   * to keep the events of a single change in order so that the resulting issue tracker state
   * transitions are never reordered, while still allowing unrelated changes to be processed in
   * parallel.
   *
   * @param orderingKey identifies the sequence the task belongs to.
   * @param task the work to run.
   */
  void execute(String orderingKey, Runnable task);
}
