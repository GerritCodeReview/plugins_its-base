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

import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.util.IssueExtractor;
import com.googlesource.gerrit.plugins.its.base.util.PropertyExtractor;
import com.googlesource.gerrit.plugins.its.base.workflow.commit_collector.CommitCollector;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Fires the triggering event on collected past commits */
public class FireEventOnCommits extends ProjectAction {

  public interface Factory {
    FireEventOnCommits create();
  }

  private final PropertyExtractor propertyExtractor;
  private final IssueExtractor issueExtractor;
  private final RuleBase ruleBase;
  private final ActionExecutor actionExecutor;
  private final FireEventOnCommitsParametersExtractor parametersExtractor;

  @Inject
  public FireEventOnCommits(
      PropertyExtractor propertyExtractor,
      IssueExtractor issueExtractor,
      RuleBase ruleBase,
      ActionExecutor actionExecutor,
      FireEventOnCommitsParametersExtractor parametersExtractor) {
    this.propertyExtractor = propertyExtractor;
    this.issueExtractor = issueExtractor;
    this.ruleBase = ruleBase;
    this.actionExecutor = actionExecutor;
    this.parametersExtractor = parametersExtractor;
  }

  @Override
  public void execute(
      ItsFacade its, String itsProject, ActionRequest actionRequest, Map<String, String> properties)
      throws IOException {
    Optional<FireEventOnCommitsParameters> extractedParameters =
        parametersExtractor.extract(actionRequest, properties);
    if (!extractedParameters.isPresent()) {
      return;
    }

    CommitCollector commitCollector = extractedParameters.get().getCommitCollector();
    String projectName = extractedParameters.get().getProjectName();

    Set<Map<String, String>> issuesProperties =
        commitCollector.collect(properties).stream()
            .map(commitId -> issueExtractor.getIssueIds(projectName, commitId))
            .flatMap(Stream::of)
            .map(
                associations -> propertyExtractor.extractIssuesProperties(properties, associations))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    issuesProperties.forEach(this::doExecute);
  }

  private void doExecute(Map<String, String> issueProperties) {
    Collection<ActionRequest> actions = ruleBase.actionRequestsFor(issueProperties);
    if (actions.isEmpty()) {
      return;
    }
    actionExecutor.executeOnIssue(actions, issueProperties);
  }
}
