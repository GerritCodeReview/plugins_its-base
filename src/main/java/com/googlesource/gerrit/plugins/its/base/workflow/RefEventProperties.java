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

import java.util.Map;
import java.util.Set;

/** The properties extracted from a {@link com.google.gerrit.server.events.RefEvent} */
public class RefEventProperties {
  private final Map<String, String> projectProperties;
  private final Set<Map<String, String>> issuesProperties;

  /**
   * @param projectProperties Properties of the ref event
   * @param issuesProperties Properties of the ref event added of the properties specific to the
   *     issues. There will be as many set of properties as number of issues
   */
  public RefEventProperties(
      Map<String, String> projectProperties, Set<Map<String, String>> issuesProperties) {
    this.projectProperties = projectProperties;
    this.issuesProperties = issuesProperties;
  }

  /** @return Properties of the ref event */
  public Map<String, String> getProjectProperties() {
    return projectProperties;
  }

  /**
   * @return Properties of the ref event added of the properties specific to the issues. There will
   *     be as many set of properties as number of issues
   */
  public Set<Map<String, String>> getIssuesProperties() {
    return issuesProperties;
  }
}
