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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A condition as used in {@link Rule}, as precondition to {@link Action}s.
 *
 * A condition consists of a key and an associated set of values. A condition
 * is said to match a set of properties, is this set contains at least one
 * property that matches the rules key and whose value matches at least one
 * of the rule's value.
 */
public class Condition {
  private final String key;
  private final Set<String> values;

  public interface Factory {
    Condition create(@Assisted("key") String key,
        @Assisted("values") String values);
  }

  /**
   * Constructs a condition.
   * @param key The key to use for values.
   * @param values A comma separated list of values to associate to the key.
   */
  @Inject
  public Condition(@Assisted("key") String key,
      @Nullable @Assisted("values") String values) {
    this.key = key;
    Set<String> modifyableValues;
    if (values == null) {
      modifyableValues = Collections.emptySet();
    } else {
      modifyableValues = Sets.newHashSet(Arrays.asList(values.split(",")));
    }
    this.values = Collections.unmodifiableSet(modifyableValues);
  }

  public String getKey() {
    return key;
  }

  /**
   * Checks whether or not the Condition matches the given set of properties
   *
   * @param properties The set of properties to match against.
   * @return True iff properties contains at least one property that matches
   *    the rules key and whose value matches at least one of the rule's value.
   */
  public boolean isMetBy(Iterable<Property> properties) {
    for (Property property : properties) {
      String propertyKey = property.getKey();
      if ((key == null && propertyKey == null)
          || (key != null && key.equals(propertyKey))) {
        if (values.contains(property.getValue())) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "[" + key + " = " + values + "]";
  }
}
