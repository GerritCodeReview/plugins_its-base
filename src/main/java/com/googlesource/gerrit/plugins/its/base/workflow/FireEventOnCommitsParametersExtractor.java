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

import com.google.common.base.Strings;
import com.googlesource.gerrit.plugins.its.base.workflow.commit_collector.CommitCollector;
import com.googlesource.gerrit.plugins.its.base.workflow.commit_collector.SinceLastTagCommitCollector;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FireEventOnCommitsParametersExtractor {

  private static final Logger log =
      LoggerFactory.getLogger(FireEventOnCommitsParametersExtractor.class);

  private final SinceLastTagCommitCollector.Factory sinceLastTagCommitCollectorFactory;

  @Inject
  public FireEventOnCommitsParametersExtractor(
      SinceLastTagCommitCollector.Factory sinceLastTagCommitCollectorFactory) {
    this.sinceLastTagCommitCollectorFactory = sinceLastTagCommitCollectorFactory;
  }

  private CommitCollector getCommitCollector(String name) {
    switch (name) {
      case "since-last-tag":
        return sinceLastTagCommitCollectorFactory.create();
      default:
        return null;
    }
  }

  /**
   * @return The parameters needed by {@link FireEventOnCommits}. Empty if the parameters could not
   *     be extracted.
   */
  public Optional<FireEventOnCommitsParameters> extract(
      ActionRequest actionRequest, Map<String, String> properties) {
    String[] parameters = actionRequest.getParameters();
    if (parameters.length != 1) {
      log.error(
          "Wrong number of received parameters. Received parameters are {}. Only one parameter is expected, the collector name.",
          Arrays.toString(parameters));
      return Optional.empty();
    }

    String collectorName = parameters[0];
    if (Strings.isNullOrEmpty(collectorName)) {
      log.error("Received collector name is blank");
      return Optional.empty();
    }

    CommitCollector commitCollector = getCommitCollector(collectorName);
    if (commitCollector == null) {
      log.error("No commit collector found for name {}", collectorName);
      return Optional.empty();
    }

    String projectName = properties.get("project");
    return Optional.of(new FireEventOnCommitsParameters(commitCollector, projectName));
  }
}
