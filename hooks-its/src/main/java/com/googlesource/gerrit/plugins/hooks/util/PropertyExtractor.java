//Copyright (C) 2013 The Android Open Source Project
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

package com.googlesource.gerrit.plugins.hooks.util;

import java.util.Set;

import com.google.gerrit.server.events.ChangeEvent;

import com.googlesource.gerrit.plugins.hooks.workflow.Property;

/**
 * Extractor to translate an {@link ChangeEvent} to
 * {@link Property Properties}.
 */
public class PropertyExtractor {
  /**
   * A set of property sets extracted from an event.
   *
   * As events may relate to more that a single issue, and properties sets are
   * should be tied to a single issue, returning {@code Collection<Property>}
   * is not sufficient, and we need to return
   * {@code Collection<Collection<Property>>}. Using this approach, a
   * PatchSetCreatedEvent for a patch set with commit message:
   *
   * <pre>
   *   (bug 4711) Fix treatment of special characters in title
   *
   *   This commit mitigates the effects of bug 42, but does not fix them.
   *
   *   Change-Id: I1234567891123456789212345678931234567894
   * </pre>
   *
   * may return both
   *
   * <pre>
   *   issue: 4711
   *   association: subject
   *   event: PatchSetCreatedEvent
   * </pre>
   *
   * and
   *
   * <pre>
   *   issue: 42
   *   association: body
   *   event: PatchSetCreatedEvent
   * </pre>
   *
   * Thereby, sites can choose to to cause different actions for different
   * issues associated to the same event. So in the above example, a comment
   * "mentioned in change 123" may be added for issue 42, and a comment
   * "fixed by change 123” may be added for issue 4711.
   *
   * @param event The event to extract property sets from.
   * @return sets of property sets extracted from the event.
   */
  public Set<Set<Property>> extractFrom(ChangeEvent event) {
    // TODO implement
    throw new RuntimeException("unimplemented");
  }
}
