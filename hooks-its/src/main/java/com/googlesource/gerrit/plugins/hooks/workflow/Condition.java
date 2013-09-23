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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A condition as used in {@link Rule}, as precondition to {@link Action}s.
 * <p>
 * A condition consists of a key and an associated set of values.
 * <p>
 * For positive conditions (see constructor), the condition is said to match a
 * set of properties, if this set contains at least one property that matches
 * the rules key and whose value matches at least one of the rule's value.
 * <p>
 * For negated conditions (see constructor), the condition is said to match a
 * set of properties, if this set does not contain any property that matches
 * the rules key and whose value matches at least one of the rule's value.
 */
public class Condition {
  private final String key;
  private final Set<String> values;
  private final boolean negated;

  public interface Factory {
    Condition create(@Assisted("key") String key,
        @Assisted("values") String values);
  }

  /**
   * Constructs a condition.
   * @param key The key to use for values.
   * @param values A comma separated list of values to associate to the key. If
   *    the first value is not "!", it's a positive condition. If the first
   *    value is "!", the "!" is removed from the values and the condition is
   *    considered a negated condition.
   */
  @Inject
  public Condition(@Assisted("key") String key,
      @Nullable @Assisted("values") String values) {
    this.key = key;
    Set<String> modifyableValues;
    boolean modifyableNegated = false;
    if (values == null) {
      modifyableValues = Collections.emptySet();
    } else {
      List<String> valueList = Lists.newArrayList(values.split(","));
      if (!valueList.isEmpty() && "!".equals(valueList.get(0))) {
        modifyableNegated = true;
        valueList.remove(0);
      }
      modifyableValues = Sets.newHashSet(valueList);
    }
    this.values = Collections.unmodifiableSet(modifyableValues);
    this.negated = modifyableNegated;
  }

  public String getKey() {
    return key;
  }

  /**
   * Checks whether or not the Condition matches the given set of properties
   *
   * @param properties The set of properties to match against.
   * @return For positive conditions, true iff properties contains at least
   *    one property that matches the rules key and whose value matches at
   *    least one of the rule's value. For negated conditions, true iff
   *    properties does not contain any property that matches the rules key
   *    and whose value matches at least one of the rule's value.
   */
  public boolean isMetBy(Iterable<Property> properties) {
    for (Property property : properties) {
      String propertyKey = property.getKey();
      if ((key == null && propertyKey == null)
          || (key != null && key.equals(propertyKey))) {
        if (values.contains(property.getValue())) {
          return !negated;
        }
      }
    }
    return negated;
  }

  @Override
  public String toString() {
    return "[" + key + " = " + values + "]";
  }
}
