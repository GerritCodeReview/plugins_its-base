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

import com.googlesource.gerrit.plugins.its.base.workflow.commit_collector.CommitCollector;

/** Parameters needed by {@link FireEventOnCommits} action */
public class FireEventOnCommitsParameters {

  private final CommitCollector commitCollector;
  private final String projectName;

  public FireEventOnCommitsParameters(CommitCollector commitCollector, String projectName) {
    this.commitCollector = commitCollector;
    this.projectName = projectName;
  }

  /**
   * @return The collector that will be used to collect the commits on which the event will be fired
   */
  public CommitCollector getCommitCollector() {
    return commitCollector;
  }

  /** @return The name of the project from which the commits will be collected */
  public String getProjectName() {
    return projectName;
  }
}
