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

package com.googlesource.gerrit.plugins.its.base.workflow.action;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.util.IssueExtractor;
import com.googlesource.gerrit.plugins.its.base.util.PropertyExtractor;
import com.googlesource.gerrit.plugins.its.base.util.PropertyUtils;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionController;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;
import com.googlesource.gerrit.plugins.its.base.workflow.action.commit_collector.CommitCollector;
import com.googlesource.gerrit.plugins.its.base.workflow.action.commit_collector.SinceLastTagCommitCollector;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FireEventOnCommits implements ProjectAction {

  private static final Logger log = LoggerFactory.getLogger(FireEventOnCommits.class);

  public interface Factory {
    FireEventOnCommits create();
  }

  private final PropertyExtractor propertyExtractor;
  private final IssueExtractor issueExtractor;
  private final ActionController actionController;
  private final SinceLastTagCommitCollector.Factory sinceLastTagCommitCollectorFactory;

  @Inject
  public FireEventOnCommits(
      PropertyExtractor propertyExtractor,
      IssueExtractor issueExtractor,
      ActionController actionController,
      SinceLastTagCommitCollector.Factory sinceLastTagCommitCollectorFactory) {
    this.propertyExtractor = propertyExtractor;
    this.issueExtractor = issueExtractor;
    this.actionController = actionController;
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

  @Override
  public void execute(String itsProject, ActionRequest actionRequest, Set<Property> properties)
      throws IOException {
    String[] parameters = actionRequest.getParameters();
    if (parameters.length != 1) {
      log.error(
          "Wrong number of received parameters. Received parameters are {}. Only one parameter is expected, the collector name.",
          Arrays.toString(parameters));
      return;
    }

    String collectorName = parameters[0];
    if (Strings.isNullOrEmpty(collectorName)) {
      log.error("Received collector name is blank");
      return;
    }

    CommitCollector commitCollector = getCommitCollector(collectorName);
    if (commitCollector == null) {
      log.error("No commit collector found for name {}", collectorName);
      return;
    }

    String projectName = PropertyUtils.getValue(properties, "project");

    Set<Set<Property>> issuesProperties =
        commitCollector
            .collect(properties)
            .stream()
            .map(commitId -> issueExtractor.getIssueIds(projectName, commitId))
            .flatMap(Stream::of)
            .map(
                associations -> propertyExtractor.extractIssuesProperties(properties, associations))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    actionController.handleIssuesEvent(issuesProperties);
  }
}
