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

import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.workflow.action.Action;
import com.googlesource.gerrit.plugins.its.base.workflow.action.AddComment;
import com.googlesource.gerrit.plugins.its.base.workflow.action.AddSoyComment;
import com.googlesource.gerrit.plugins.its.base.workflow.action.AddStandardComment;
import com.googlesource.gerrit.plugins.its.base.workflow.action.AddVelocityComment;
import com.googlesource.gerrit.plugins.its.base.workflow.action.LogEvent;
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
  private final AddVelocityComment.Factory addVelocityCommentFactory;
  private final AddSoyComment.Factory addSoyCommentFactory;
  private final LogEvent.Factory logEventFactory;

  @Inject
  public ActionExecutor(
      ItsFacade its,
      AddComment.Factory addCommentFactory,
      AddStandardComment.Factory addStandardCommentFactory,
      AddVelocityComment.Factory addVelocityCommentFactory,
      AddSoyComment.Factory addSoyCommentFactory,
      LogEvent.Factory logEventFactory) {
    this.its = its;
    this.addCommentFactory = addCommentFactory;
    this.addStandardCommentFactory = addStandardCommentFactory;
    this.addVelocityCommentFactory = addVelocityCommentFactory;
    this.addSoyCommentFactory = addSoyCommentFactory;
    this.logEventFactory = logEventFactory;
  }

  public void execute(String issue, ActionRequest actionRequest, Set<Property> properties) {
    try {
      String name = actionRequest.getName();
      Action action = null;
      if ("add-comment".equals(name)) {
        action = addCommentFactory.create();
      } else if ("add-standard-comment".equals(name)) {
        action = addStandardCommentFactory.create();
      } else if ("add-velocity-comment".equals(name)) {
        action = addVelocityCommentFactory.create();
      } else if ("add-soy-comment".equals(name)) {
        action = addSoyCommentFactory.create();
      } else if ("log-event".equals(name)) {
        action = logEventFactory.create();
      }

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
