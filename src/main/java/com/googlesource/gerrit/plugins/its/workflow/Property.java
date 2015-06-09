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

package com.googlesource.gerrit.plugins.its.workflow;

import com.google.gerrit.common.Nullable;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A property to match against {@code Condition}s.
 *
 * A property is a simple key value pair.
 */
public class Property {
  public interface Factory {
    Property create(@Assisted("key") String key,
        @Assisted("value") String value);
  }

  private final String key;
  private final String value;

  @Inject
  public Property(@Assisted("key") String key,
      @Nullable @Assisted("value") String value) {
    this.key = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  @Override
  public boolean equals(Object other) {
    boolean ret = false;
    if (other != null && other instanceof Property) {
      Property otherProperty = (Property) other;
      ret = true;

      if (key == null) {
        ret &= otherProperty.getKey() == null;
      } else {
        ret &= key.equals(otherProperty.getKey());
      }

      if (value == null) {
        ret &= otherProperty.getValue() == null;
      } else {
        ret &= value.equals(otherProperty.getValue());
      }
    }
    return ret;
  }

  @Override
  public int hashCode() {
    return (key == null ? 0 : key.hashCode()) * 31 +
        (value == null ? 0 : value.hashCode());
  }

  @Override
  public String toString() {
    return "[" + key + " = " + value + "]";
  }
}
