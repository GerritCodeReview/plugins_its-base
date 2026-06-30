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

package com.googlesource.gerrit.plugins.its.base.workflow;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.RefEvent;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import com.googlesource.gerrit.plugins.its.base.util.PropertyExtractor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Controller that takes actions according to {@code ChangeEvents@}.
 *
 * <p>The taken actions are typically Its related (e.g.: adding an Its comment, or changing an
 * issue's status).
 *
 * <p>Event handling is split into two phases: extract properties and match rules to produce
 * actions, then run those actions against the issue tracker.
 */
public class ActionController implements EventListener {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private record ScopedActions(Collection<ActionRequest> actions, Map<String, String> properties) {}

  private record EventActions(
      List<ScopedActions> issueActions, Optional<ScopedActions> projectActions) {}

  private final PropertyExtractor propertyExtractor;
  private final RuleBase ruleBase;
  private final ActionExecutor actionExecutor;
  private final ItsConfig itsConfig;

  @Inject
  public ActionController(
      PropertyExtractor propertyExtractor,
      RuleBase ruleBase,
      ActionExecutor actionExecutor,
      ItsConfig itsConfig) {
    this.propertyExtractor = propertyExtractor;
    this.ruleBase = ruleBase;
    this.actionExecutor = actionExecutor;
    this.itsConfig = itsConfig;
  }

  @Override
  public void onEvent(Event event) {
    if (!(event instanceof RefEvent refEvent)) {
      return;
    }

    final Project.NameKey projectName = refEvent.getProjectNameKey();
    if (!itsConfig.isEnabled(refEvent)) {
      return;
    }

    Optional<EventActions> eventActions = gatherActions(refEvent, projectName);
    if (eventActions.isEmpty()) {
      return;
    }
    executeActions(refEvent, projectName, eventActions.get());
  }

  private Optional<EventActions> gatherActions(RefEvent refEvent, Project.NameKey projectName) {
    List<ScopedActions> issueActions;
    Optional<ScopedActions> projectActions;
    ItsConfig.setCurrentProjectName(projectName);
    try {
      RefEventProperties refEventProperties = propertyExtractor.extractFrom(refEvent);
      issueActions = gatherIssueActions(refEventProperties.getIssuesProperties());
      projectActions = gatherProjectActions(refEventProperties.getProjectProperties());
    } catch (RuntimeException e) {
      logger.atSevere().withCause(e).log(
          "Error while extracting ITS actions from event %s for project %s", refEvent, projectName);
      return Optional.empty();
    }
    if (issueActions.isEmpty() && projectActions.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new EventActions(issueActions, projectActions));
  }

  private List<ScopedActions> gatherIssueActions(Set<Map<String, String>> issuesProperties) {
    List<ScopedActions> issueActions = new ArrayList<>();
    for (Map<String, String> issueProperties : issuesProperties) {
      gatherIssueAction(issueProperties)
          .ifPresent(actions -> issueActions.add(new ScopedActions(actions, issueProperties)));
    }
    return issueActions;
  }

  private Optional<Collection<ActionRequest>> gatherIssueAction(
      Map<String, String> issueProperties) {
    Collection<ActionRequest> actions = ruleBase.actionRequestsFor(issueProperties);
    return actions.isEmpty() ? Optional.empty() : Optional.of(actions);
  }

  private Optional<ScopedActions> gatherProjectActions(Map<String, String> projectProperties) {
    return gatherProjectAction(projectProperties)
        .map(actions -> new ScopedActions(actions, projectProperties));
  }

  private Optional<Collection<ActionRequest>> gatherProjectAction(
      Map<String, String> projectProperties) {
    if (projectProperties.isEmpty()) {
      return Optional.empty();
    }

    Collection<ActionRequest> projectActions = ruleBase.actionRequestsFor(projectProperties);
    if (projectActions.isEmpty()) {
      return Optional.empty();
    }
    if (!projectProperties.containsKey("its-project")) {
      String project = projectProperties.get("project");
      logger.atFinest().log(
          "Could not process project event. No its-project associated with project %s. "
              + "Did you forget to configure the ITS project association in project.config?",
          project);
      return Optional.empty();
    }

    return Optional.of(projectActions);
  }

  private void executeActions(
      RefEvent event, Project.NameKey projectName, EventActions eventActions) {
    try {
      for (ScopedActions issueAction : eventActions.issueActions()) {
        actionExecutor.executeOnIssue(issueAction.actions(), issueAction.properties());
      }
      eventActions
          .projectActions()
          .ifPresent(
              projectAction ->
                  actionExecutor.executeOnProject(
                      projectAction.actions(), projectAction.properties()));
    } catch (RuntimeException e) {
      logger.atSevere().withCause(e).log(
          "Error while handling event %s for project %s", event, projectName);
    }
  }
}
