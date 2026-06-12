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
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Project;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.PatchSetEvent;
import com.google.gerrit.server.events.RefEvent;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.Actions;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import com.googlesource.gerrit.plugins.its.base.util.PropertyExtractor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;

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
  private final Executor actionsExecutor;

  @Inject
  public ActionController(
      PropertyExtractor propertyExtractor,
      RuleBase ruleBase,
      ActionExecutor actionExecutor,
      ItsConfig itsConfig,
      @Actions Executor actionsExecutor) {
    this.propertyExtractor = propertyExtractor;
    this.ruleBase = ruleBase;
    this.actionExecutor = actionExecutor;
    this.itsConfig = itsConfig;
    this.actionsExecutor = actionsExecutor;
  }

  @Override
  public void onEvent(Event event) {
    if (event instanceof RefEvent) {
      RefEvent refEvent = (RefEvent) event;
      if (itsConfig.isEnabled(refEvent)) {
        new EventHandler(refEvent);
      }
    }
  }

  private class EventHandler implements BoundedOrderedDispatcher.OrderedTask {
    private final RefEvent refEvent;
    private final List<Runnable> actionRunnables = new ArrayList<>();

    EventHandler(RefEvent refEvent) {
      this.refEvent = refEvent;

      ItsConfig.setCurrentProjectName(refEvent.getProjectNameKey());
      try {
        RefEventProperties refEventProperties = propertyExtractor.extractFrom(refEvent);
        handleIssuesEvent(refEventProperties.getIssuesProperties());
        handleProjectEvent(refEventProperties.getProjectProperties());
      } finally {
        ItsConfig.clearCurrentProjectName();
      }
      if (!actionRunnables.isEmpty()) {
        actionsExecutor.execute(this);
      }
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
        String project = projectProperties.get("project");
        logger.atFinest().log(
            "Could not process project event. No its-project associated with project %s. "
                + "Did you forget to configure the ITS project association in project.config?",
            project);
        return;
      }

      actionRunnables.add(() -> actionExecutor.executeOnProject(projectActions, projectProperties));
    }

    @Override
    public void run() {
      ItsConfig.setCurrentProjectName(refEvent.getProjectNameKey());
      try {
        actionRunnables.forEach(Runnable::run);
      } finally {
        ItsConfig.clearCurrentProjectName();
      }
    }

    @Override
    public Optional<ChangeKey> key() {
      return ChangeKey.optionallyFrom(refEvent);
    }

    @Override
    public String toString() {
      String target = refEvent.getBranchNameKey().toString();
      if (refEvent instanceof PatchSetEvent patchSetEvent) {
        PatchSetAttribute patchSet = patchSetEvent.patchSet.get();
        if (patchSet != null) {
          target = refEvent.getProjectNameKey().get() + " " + patchSet.ref;
        }
      }
      return "its-actions: " + refEvent.getType() + " " + target;
    }
  }

  public record ChangeKey(Project.NameKey projectName, String destRefName, Change.Key changeKey) {
    public static Optional<ChangeKey> optionallyFrom(RefEvent event) {
      if (!(event instanceof ChangeEvent changeEvent)) {
        return Optional.empty();
      }
      return Optional.of(
          new ChangeKey(event.getProjectNameKey(), event.getRefName(), changeEvent.getChangeKey()));
    }
  }
}
