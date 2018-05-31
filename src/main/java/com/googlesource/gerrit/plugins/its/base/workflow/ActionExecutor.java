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

import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.workflow.action.*;

import java.io.IOException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Executes an {@link ActionRequest} */
public class ActionExecutor {
  private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);

  private final ItsFacade its;
  private final AddComment.Factory addCommentFactory;
  private final AddStandardComment.Factory addStandardCommentFactory;
  private final AddSoyComment.Factory addSoyCommentFactory;
  private final LogEvent.Factory logEventFactory;
  private final AddPropertyToField.Factory addPropertyToFieldFactory;

  @Inject
  public ActionExecutor(
      ItsFacade its,
      AddComment.Factory addCommentFactory,
      AddStandardComment.Factory addStandardCommentFactory,
      AddSoyComment.Factory addSoyCommentFactory,
      LogEvent.Factory logEventFactory,
      AddPropertyToField.Factory addPropertyToFieldFactory) {
    this.its = its;
    this.addCommentFactory = addCommentFactory;
    this.addStandardCommentFactory = addStandardCommentFactory;
    this.addSoyCommentFactory = addSoyCommentFactory;
    this.logEventFactory = logEventFactory;
    this.addPropertyToFieldFactory = addPropertyToFieldFactory;
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
      default:
        return null;
    }
  }

  public void execute(String issue, ActionRequest actionRequest, Set<Property> properties) {
    try {
      Action action = getAction(actionRequest.getName());
      if (action == null) {
        its.performAction(issue, actionRequest.getUnparsed());
      } else {
        action.execute(issue, actionRequest, properties);
      }
    } catch (IOException e) {
      log.error("Error while executing action " + actionRequest, e);
    }
  }

  public void execute(String issue, Iterable<ActionRequest> actions, Set<Property> properties) {
    for (ActionRequest actionRequest : actions) {
      execute(issue, actionRequest, properties);
    }
  }
}
