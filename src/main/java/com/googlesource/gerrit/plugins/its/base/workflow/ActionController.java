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

import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.EventListener;
import com.google.gerrit.server.events.RefEvent;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import com.googlesource.gerrit.plugins.its.base.its.ItsServer;
import com.googlesource.gerrit.plugins.its.base.its.ItsServerInfo;
import com.googlesource.gerrit.plugins.its.base.util.PropertyExtractor;
import java.util.Collection;
import java.util.Set;

/**
 * Controller that takes actions according to {@code ChangeEvents@}.
 *
 * <p>The taken actions are typically Its related (e.g.: adding an Its comment, or changing an
 * issue's status).
 */
public class ActionController implements EventListener {
  private final PropertyExtractor propertyExtractor;
  private final RuleBase ruleBase;
  private final ActionExecutor actionExecutor;
  private final ItsConfig itsConfig;
  private final ItsServer itsServer;

  @Inject
  public ActionController(
      PropertyExtractor propertyExtractor,
      RuleBase ruleBase,
      ActionExecutor actionExecutor,
      ItsConfig itsConfig,
      ItsServer itsServer) {
    this.propertyExtractor = propertyExtractor;
    this.ruleBase = ruleBase;
    this.actionExecutor = actionExecutor;
    this.itsConfig = itsConfig;
    this.itsServer = itsServer;
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
    ItsServerInfo server = itsServer.getServer(refEvent.getProjectNameKey());
    ItsConfig.setCurrentProjectName(refEvent.getProjectNameKey());
    for (Set<Property> properties : propertyExtractor.extractFrom(refEvent)) {
      Collection<ActionRequest> actions = ruleBase.actionRequestsFor(properties);
      if (!actions.isEmpty()) {
        for (Property property : properties) {
          if ("issue".equals(property.getKey())) {
            String issue = property.getValue();
            actionExecutor.execute(server, issue, actions, properties);
          }
        }
      }
    }
  }
}
