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
import com.google.gerrit.server.git.ProjectRunnable;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.ItsExecutor;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import com.googlesource.gerrit.plugins.its.base.util.PropertyExtractor;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Controller that takes actions according to {@code ChangeEvents@}.
 *
 * <p>The taken actions are typically Its related (e.g.: adding an Its comment, or changing an
 * issue's status).
 */
public class ActionController implements EventListener {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final PropertyExtractor propertyExtractor;
  private final RuleBase ruleBase;
  private final ActionExecutor actionExecutor;
  private final ItsConfig itsConfig;
  private final ExecutorService executor;

  @Inject
  public ActionController(
      PropertyExtractor propertyExtractor,
      RuleBase ruleBase,
      ActionExecutor actionExecutor,
      ItsConfig itsConfig,
      @ItsExecutor ExecutorService executor) {
    this.propertyExtractor = propertyExtractor;
    this.ruleBase = ruleBase;
    this.actionExecutor = actionExecutor;
    this.itsConfig = itsConfig;
    this.executor = executor;
  }

  @Override
  public void onEvent(Event event) {
    if (!(event instanceof RefEvent)) {
      return;
    }
    RefEvent refEvent = (RefEvent) event;
    Project.NameKey projectName = refEvent.getProjectNameKey();

    if (!isEnabled(refEvent, projectName)) {
      return;
    }

    executor.execute(new EventTask(refEvent, projectName));
  }

  private boolean isEnabled(RefEvent refEvent, Project.NameKey projectName) {
    ItsConfig.setCurrentProjectName(projectName);
    try {
      return itsConfig.isEnabled(refEvent);
    } finally {
      ItsConfig.setCurrentProjectName(null);
    }
  }

  private void handleEvent(RefEvent refEvent) {
    RefEventProperties refEventProperties = propertyExtractor.extractFrom(refEvent);

    handleIssuesEvent(refEventProperties.getIssuesProperties());
    handleProjectEvent(refEventProperties.getProjectProperties());
  }

  private void handleIssuesEvent(Set<Map<String, String>> issuesProperties) {
    for (Map<String, String> issueProperties : issuesProperties) {
      Collection<ActionRequest> actions = ruleBase.actionRequestsFor(issueProperties);
      if (!actions.isEmpty()) {
        actionExecutor.executeOnIssue(actions, issueProperties);
      }
    }
  }

  private void handleProjectEvent(Map<String, String> projectProperties) {
    if (projectProperties.isEmpty()) {
      return;
    }

    Collection<ActionRequest> projectActions = ruleBase.actionRequestsFor(projectProperties);
    if (projectActions.isEmpty()) {
      return;
    }
    if (!projectProperties.containsKey("its-project")) {
      String project = projectProperties.get("project");
      logger.atFinest().log(
          "Could not process project event. No its-project associated with project %s. "
              + "Did you forget to configure the ITS project association in project.config?",
          project);
      return;
    }

    actionExecutor.executeOnProject(projectActions, projectProperties);
  }

  private class EventTask implements ProjectRunnable {
    private final RefEvent event;
    private final Project.NameKey projectName;

    EventTask(RefEvent event, Project.NameKey projectName) {
      this.event = event;
      this.projectName = projectName;
    }

    @Override
    public void run() {
      ItsConfig.setCurrentProjectName(projectName);
      try {
        handleEvent(event);
      } catch (RuntimeException e) {
        logger.atSevere().withCause(e).log(
            "Error while handling event %s for project %s", event, projectName);
      } finally {
        ItsConfig.setCurrentProjectName(null);
      }
    }

    @Override
    public Project.NameKey getProjectNameKey() {
      return projectName;
    }

    @Override
    public String getRemoteName() {
      return null;
    }

    @Override
    public boolean hasCustomizedPrint() {
      return false;
    }

    @Override
    public String toString() {
      return "its: " + event.getType() + " " + event.getRefName();
    }
  }
}
