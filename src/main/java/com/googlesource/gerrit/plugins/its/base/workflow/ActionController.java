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
import java.util.Set;

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
  private final EventExecutor eventExecutor;

  @Inject
  public ActionController(
      PropertyExtractor propertyExtractor,
      RuleBase ruleBase,
      ActionExecutor actionExecutor,
      ItsConfig itsConfig,
      EventExecutor eventExecutor) {
    this.propertyExtractor = propertyExtractor;
    this.ruleBase = ruleBase;
    this.actionExecutor = actionExecutor;
    this.itsConfig = itsConfig;
    this.eventExecutor = eventExecutor;
  }

  @Override
  public void onEvent(final Event event) {
    if (event instanceof RefEvent refEvent) {
      if (itsConfig.isEnabled(refEvent)) {
        final Project.NameKey projectName = refEvent.getProjectNameKey();
        ItsConfig.setCurrentProjectName(projectName);
        try {
          EventHandler handler = new EventHandler(refEvent, projectName);
          if (handler.hasActions()) {
            eventExecutor.execute(ChangeKey.from(refEvent, projectName), handler);
          }
        } finally {
          ItsConfig.clearCurrentProjectName();
        }
      }
    }
  }

  public record ChangeKey(Project.NameKey projectName, String refName, Change.Key changeKey) {
    @Nullable
    static ChangeKey from(final RefEvent event, final Project.NameKey projectName) {
      if (!(event instanceof ChangeEvent changeEvent)) {
        return null;
      }
      return new ChangeKey(projectName, event.getRefName(), changeEvent.getChangeKey());
    }
  }

  private class EventHandler implements Runnable {
    private final RefEvent refEvent;
    private final Project.NameKey projectName;
    private final List<Runnable> actionRunnables = new ArrayList<>();

    EventHandler(RefEvent refEvent, Project.NameKey projectName) {
      this.refEvent = refEvent;
      this.projectName = projectName;

      RefEventProperties refEventProperties = propertyExtractor.extractFrom(refEvent);
      handleIssuesEvent(refEventProperties.getIssuesProperties());
      handleProjectEvent(refEventProperties.getProjectProperties());
    }

    boolean hasActions() {
      return !actionRunnables.isEmpty();
    }

    @Override
    public void run() {
      ItsConfig.setCurrentProjectName(projectName);
      try {
        for (Runnable action : actionRunnables) {
          action.run();
        }
      } finally {
        ItsConfig.clearCurrentProjectName();
      }
    }

    @Override
    public String toString() {
      return "its: " + refEvent.getType() + " " + refEvent.getBranchNameKey();
    }

    private void handleIssuesEvent(Set<Map<String, String>> issuesProperties) {
      for (Map<String, String> issueProperties : issuesProperties) {
        Collection<ActionRequest> actions = ruleBase.actionRequestsFor(issueProperties);
        if (!actions.isEmpty()) {
          actionRunnables.add(() -> actionExecutor.executeOnIssue(actions, issueProperties));
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
        logger.atFinest().log(
            "Could not process project event. No its-project associated with project %s. "
                + "Did you forget to configure the ITS project association in project.config?",
            projectProperties.get("project"));
        return;
      }

      actionRunnables.add(() -> actionExecutor.executeOnProject(projectActions, projectProperties));
    }
  }
}
