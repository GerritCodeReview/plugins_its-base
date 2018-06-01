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
import com.googlesource.gerrit.plugins.its.base.workflow.action.CreateVersionFromProperty;
import com.googlesource.gerrit.plugins.its.base.workflow.action.ProjectAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class ProjectActionExecutor {

  private static final Logger log = LoggerFactory.getLogger(ProjectActionExecutor.class);

  private final CreateVersionFromProperty.Factory createVersionFromPropertyFactory;

  @Inject
  public ProjectActionExecutor(CreateVersionFromProperty.Factory createVersionFromPropertyFactory) {
    this.createVersionFromPropertyFactory = createVersionFromPropertyFactory;
  }

  private ProjectAction getAction(String actionName) {
    switch (actionName) {
      case "create-version-from-property":
        return createVersionFromPropertyFactory.create();
      default:
        return null;
    }
  }

  public void execute(String itsProject, ActionRequest actionRequest, Set<Property> properties) {
    try {
      String actionName = actionRequest.getName();
      ProjectAction action = getAction(actionName);
      if (action == null) {
        log.debug("No action found for name {}", actionName);
        return;
      }
      action.execute(itsProject, actionRequest, properties);
    } catch (IOException e) {
      log.error("Error while executing action " + actionRequest, e);
    }
  }

  public void execute(
      String itsProject, Iterable<ActionRequest> actions, Set<Property> properties) {
    for (ActionRequest actionRequest : actions) {
      execute(itsProject, actionRequest, properties);
    }
  }
}
