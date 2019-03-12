// Copyright (C) 2017 The Android Open Source Project
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

import com.google.gerrit.extensions.registration.DynamicMap;
import com.google.gerrit.extensions.registration.PluginName;
import com.google.gerrit.reviewdb.client.Project;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacadeFactory;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Executes an {@link ActionRequest} */
public class ActionExecutor {
  private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);

  private final ItsFacadeFactory itsFactory;
  private final AddComment.Factory addCommentFactory;
  private final AddStandardComment.Factory addStandardCommentFactory;
  private final AddSoyComment.Factory addSoyCommentFactory;
  private final LogEvent.Factory logEventFactory;
  private final AddPropertyToField.Factory addPropertyToFieldFactory;
  private final CreateVersionFromProperty.Factory createVersionFromPropertyFactory;
  private final FireEventOnCommits.Factory fireEventOnCommitsFactory;
  private final DynamicMap<CustomAction> customActions;

  @Inject
  public ActionExecutor(
      ItsFacadeFactory itsFactory,
      AddComment.Factory addCommentFactory,
      AddStandardComment.Factory addStandardCommentFactory,
      AddSoyComment.Factory addSoyCommentFactory,
      LogEvent.Factory logEventFactory,
      AddPropertyToField.Factory addPropertyToFieldFactory,
      CreateVersionFromProperty.Factory createVersionFromPropertyFactory,
      FireEventOnCommits.Factory fireEventOnCommitsFactory,
      DynamicMap<CustomAction> customActions) {
    this.itsFactory = itsFactory;
    this.addCommentFactory = addCommentFactory;
    this.addStandardCommentFactory = addStandardCommentFactory;
    this.addSoyCommentFactory = addSoyCommentFactory;
    this.logEventFactory = logEventFactory;
    this.addPropertyToFieldFactory = addPropertyToFieldFactory;
    this.createVersionFromPropertyFactory = createVersionFromPropertyFactory;
    this.fireEventOnCommitsFactory = fireEventOnCommitsFactory;
    this.customActions = customActions;
  }

  private Action getAction(String actionName) {
    switch (actionName) {
      case "add-comment":
        return addCommentFactory.create();
      case "add-standard-comment":
        return addStandardCommentFactory.create();
      case "add-soy-comment":
        return addSoyCommentFactory.create();
      case "log-event":
        return logEventFactory.create();
      case "add-property-to-field":
        return addPropertyToFieldFactory.create();
      case "create-version-from-property":
        return createVersionFromPropertyFactory.create();
      case "fire-event-on-commits":
        return fireEventOnCommitsFactory.create();
      default:
        return customActions.get(PluginName.GERRIT, actionName);
    }
  }

  private void execute(
      Action action, String target, ActionRequest actionRequest, Map<String, String> properties)
      throws IOException {
    ItsFacade its = itsFactory.getFacade(new Project.NameKey(properties.get("project")));
    action.execute(its, target, actionRequest, properties);
  }

  private void executeOnIssue(
      String issue, ActionRequest actionRequest, Map<String, String> properties) {
    try {
      Action action = getAction(actionRequest.getName());
      if (action == null) {
        ItsFacade its = itsFactory.getFacade(new Project.NameKey(properties.get("project")));
        its.performAction(issue, actionRequest.getUnparsed());
      } else if (action.getType() == ActionType.ISSUE) {
        execute(action, issue, actionRequest, properties);
      }
    } catch (IOException e) {
      log.error("Error while executing action " + actionRequest, e);
    }
  }

  public void executeOnIssue(Iterable<ActionRequest> actions, Map<String, String> properties) {
    for (ActionRequest actionRequest : actions) {
      executeOnIssue(properties.get("issue"), actionRequest, properties);
    }
  }

  private void executeOnProject(
      String itsProject, ActionRequest actionRequest, Map<String, String> properties) {
    try {
      String actionName = actionRequest.getName();
      Action action = getAction(actionName);
      if (action == null) {
        log.debug("No action found for name {}", actionName);
        return;
      }
      if (action.getType() != ActionType.PROJECT) {
        return;
      }
      execute(action, itsProject, actionRequest, properties);
    } catch (IOException e) {
      log.error("Error while executing action " + actionRequest, e);
    }
  }

  public void executeOnProject(Iterable<ActionRequest> actions, Map<String, String> properties) {
    for (ActionRequest actionRequest : actions) {
      executeOnProject(properties.get("its-project"), actionRequest, properties);
    }
  }
}
