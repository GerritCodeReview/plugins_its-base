// Copyright (C) 2018 The Android Open Source Project
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

import java.util.Collections;
import java.util.Set;

public class RefEventProperties {

  private final Set<Property> projectProperties;
  private final Set<Set<Property>> issuesProperties;

  public RefEventProperties(Set<Property> projectProperties, Set<Set<Property>> issuesProperties) {
    this.projectProperties = Collections.unmodifiableSet(projectProperties);
    this.issuesProperties = Collections.unmodifiableSet(issuesProperties);
  }

  /** @return Properties of the ref event */
  public Set<Property> getProjectProperties() {
    return projectProperties;
  }

  /**
   * @return Properties of the ref event added of the properties specific to the issues. There will
   *     be as many set of properties as number of issues
   */
  public Set<Set<Property>> getIssuesProperties() {
    return issuesProperties;
  }
}
