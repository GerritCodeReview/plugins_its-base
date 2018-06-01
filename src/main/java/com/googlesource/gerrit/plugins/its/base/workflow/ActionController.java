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

import com.google.common.base.Strings;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.RefEvent;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import com.googlesource.gerrit.plugins.its.base.util.PropertyExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * Controller that takes actions according to {@code ChangeEvents@}.
 *
 * <p>The taken actions are typically Its related (e.g.: adding an Its comment, or changing an
 * issue's status).
 */
public class ActionController implements EventListener {

  private static final Logger log = LoggerFactory.getLogger(ActionController.class);

  private final PropertyExtractor propertyExtractor;
  private final RuleBase ruleBase;
  private final ActionExecutor actionExecutor;
  private final ProjectActionExecutor projectActionExecutor;
  private final ItsConfig itsConfig;

  @Inject
  public ActionController(
      PropertyExtractor propertyExtractor,
      RuleBase ruleBase,
      ActionExecutor actionExecutor,
      ProjectActionExecutor projectActionExecutor,
      ItsConfig itsConfig) {
    this.propertyExtractor = propertyExtractor;
    this.ruleBase = ruleBase;
    this.actionExecutor = actionExecutor;
    this.projectActionExecutor = projectActionExecutor;
    this.itsConfig = itsConfig;
  }

  @Override
  public void onEvent(Event event) {
    if (event instanceof RefEvent) {
      RefEvent refEvent = (RefEvent) event;
      if (itsConfig.isEnabled(refEvent)) {
        handleEvent(refEvent);
      }
    }
  }

  private void handleEvent(RefEvent refEvent) {
    RefEventProperties refEventProperties = propertyExtractor.extractFrom(refEvent);

    handleIssuesEvent(refEventProperties.getIssuesProperties());
    handleProjectEvent(refEventProperties.getProjectProperties());
  }

  private void handleIssuesEvent(Set<Set<Property>> issuesProperties) {
    for (Set<Property> issueProperties : issuesProperties) {
      Collection<ActionRequest> actions = ruleBase.actionRequestsFor(issueProperties);
      if (actions.isEmpty()) {
        return;
      }

      for (Property property : issueProperties) {
        if ("issue".equals(property.getKey())) {
          String issue = property.getValue();
          actionExecutor.execute(issue, actions, issueProperties);
        }
      }
    }
  }

  private void handleProjectEvent(Set<Property> projectProperties) {
    Collection<ActionRequest> projectActions = ruleBase.actionRequestsFor(projectProperties);
    if (projectActions.isEmpty()) {
      return;
    }
    String itsProject = getValue(projectProperties, "its-project");
    if (Strings.isNullOrEmpty(itsProject)) {
      String project = getValue(projectProperties, "project");
      log.error(
          "Could not process project event. No its-project associated with project {}. "
              + "Did you forget to configure the ITS project association in project.config?",
          project);
      return;
    }

    projectActionExecutor.execute(itsProject, projectActions, projectProperties);
  }

  private String getValue(Set<Property> properties, String key) {
    return properties
        .stream()
        .filter(property -> key.equals(property.getKey()))
        .map(Property::getValue)
        .findFirst()
        .orElse(null);
  }
}
