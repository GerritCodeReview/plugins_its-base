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
import com.google.gerrit.common.Nullable;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.events.ChangeEvent;
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
 * <p>Issue detection and rule matching run synchronously on the calling (event-dispatch) thread, so
 * that no work is queued unless at least one action needs to be fired. Only the matched actions,
 * which update the issue tracker, are executed asynchronously through the {@link EventExecutor}.
 */
public class ActionController implements EventListener {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final PropertyExtractor propertyExtractor;
  private final RuleBase ruleBase;
  private final ActionExecutor actionExecutor;
  private final ItsConfig itsConfig;
  private final EventExecutor executor;

  @Inject
  public ActionController(
      PropertyExtractor propertyExtractor,
      RuleBase ruleBase,
      ActionExecutor actionExecutor,
      ItsConfig itsConfig,
      EventExecutor executor) {
    this.propertyExtractor = propertyExtractor;
    this.ruleBase = ruleBase;
    this.actionExecutor = actionExecutor;
    this.itsConfig = itsConfig;
    this.executor = executor;
  }

  @Override
  public void onEvent(final Event event) {
    if (!(event instanceof RefEvent refEvent)) {
      return;
    }
    if (!itsConfig.isEnabled(refEvent)) {
      return;
    }
    final Project.NameKey projectName = refEvent.getProjectNameKey();

    Optional<EventActions> eventActions = gatherActions(refEvent, projectName);
    if (eventActions.isEmpty()) {
      return;
    }

    String changeKey = changeKey(refEvent, projectName);
    EventTask task = new EventTask(refEvent, projectName, changeKey, eventActions.get());
    executor.execute(task.orderingKey(), task);
  }

  @Nullable
  private static String changeKey(final RefEvent event, final Project.NameKey projectName) {
    if (!(event instanceof ChangeEvent changeEvent)) {
      return null;
    }
    final Change.Key changeKey = changeEvent.getChangeKey();
    if (changeKey == null) {
      return null;
    }
    return projectName.get() + "\n" + event.getRefName() + "\n" + changeKey.get();
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
    } finally {
      ItsConfig.clearCurrentProjectName();
    }

    if (issueActions.isEmpty() && projectActions.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new EventActions(issueActions, projectActions));
  }

  private List<ScopedActions> gatherIssueActions(Set<Map<String, String>> issuesProperties) {
    List<ScopedActions> issueActions = new ArrayList<>();
    for (Map<String, String> issueProperties : issuesProperties) {
      Collection<ActionRequest> actions = ruleBase.actionRequestsFor(issueProperties);
      if (!actions.isEmpty()) {
        issueActions.add(new ScopedActions(actions, issueProperties));
      }
    }
    return issueActions;
  }

  private Optional<ScopedActions> gatherProjectActions(Map<String, String> projectProperties) {
    if (projectProperties.isEmpty()) {
      return Optional.empty();
    }
    Collection<ActionRequest> projectActions = ruleBase.actionRequestsFor(projectProperties);
    if (projectActions.isEmpty()) {
      return Optional.empty();
    }
    if (!projectProperties.containsKey("its-project")) {
      logger.atFinest().log(
          "Could not process project event. No its-project associated with project %s. "
              + "Did you forget to configure the ITS project association in project.config?",
          projectProperties.get("project"));
      return Optional.empty();
    }
    return Optional.of(new ScopedActions(projectActions, projectProperties));
  }

  private record ScopedActions(Collection<ActionRequest> actions, Map<String, String> properties) {}

  private record EventActions(
      List<ScopedActions> issueActions, Optional<ScopedActions> projectActions) {}

  private class EventTask implements Runnable {
    private final RefEvent event;
    private final Project.NameKey projectName;
    @Nullable private final String orderingKey;
    private final EventActions eventActions;

    EventTask(
        final RefEvent event,
        Project.NameKey projectName,
        @Nullable String orderingKey,
        EventActions eventActions) {
      this.event = event;
      this.projectName = projectName;
      this.orderingKey = orderingKey;
      this.eventActions = eventActions;
    }

    @Nullable
    String orderingKey() {
      return orderingKey;
    }

    @Override
    public void run() {
      ItsConfig.setCurrentProjectName(projectName);
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
      } catch (RuntimeException failure) {
        logger.atSevere().withCause(failure).log(
            "Error while handling event %s for project %s", event, projectName);
      } finally {
        ItsConfig.clearCurrentProjectName();
      }
    }

    @Override
    public String toString() {
      return "its: " + event.getType() + " " + event.getBranchNameKey();
    }
  }
}
