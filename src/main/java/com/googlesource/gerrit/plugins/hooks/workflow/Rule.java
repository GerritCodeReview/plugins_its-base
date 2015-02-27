// Copyright (C) 2013 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.hooks.workflow;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A single rule that associates {@code Action}s to {@code Condition}s.
 */
public class Rule {
  private final String name;
  private List<ActionRequest> actionRequests;
  private Set<Condition> conditions;

  public interface Factory {
    Rule create(String name);
  }

  @Inject
  public Rule(@Assisted String name) {
    this.name = name;
    this.actionRequests = Lists.newLinkedList();
    this.conditions = Sets.newHashSet();
  }

  public String getName() {
    return name;
  }

  /**
   * Adds a condition to the rule.
   *
   * @param condition The condition to add.
   */
  public void addCondition(Condition condition) {
    conditions.add(condition);
  }

  /**
   * Adds an action to the rule.
   *
   * @param actionRequest The action to add.
   */
  public void addActionRequest(ActionRequest actionRequest) {
    actionRequests.add(actionRequest);
  }

  /**
   * Gets this rule's the action requests for a given set of properties.
   *
   * If the given set of properties meets all of the rule's conditions, the
   * rule's actions are returned. Otherwise the empty collection is returned.
   *
   * @param properties The properties to check against the rule's conditions.
   * @return The actions that should get fired.
   */
  public Collection<ActionRequest> actionRequestsFor(
      Iterable<Property> properties) {
    for (Condition condition : conditions) {
      if (!condition.isMetBy(properties)) {
        return Collections.emptyList();
      }
    }
    return Collections.unmodifiableList(actionRequests);
  }

  @Override
  public String toString() {
    return "[" + name + ", " + conditions + " -> " + actionRequests + "]";
  }
}
