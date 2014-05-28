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

package com.googlesource.gerrit.plugins.hooks.workflow;

import java.util.Collection;
import java.util.Set;

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.hooks.its.ItsConfig;
import com.googlesource.gerrit.plugins.hooks.util.PropertyExtractor;

/**
 * Controller that takes actions according to {@code ChangeEvents@}.
 *
 * The taken actions are typically Its related (e.g.: adding an Its comment, or
 * changing an issue's status).
 */
public class ActionController implements ChangeListener {
  private final PropertyExtractor propertyExtractor;
  private final RuleBase ruleBase;
  private final ActionExecutor actionExecutor;
  private final ItsConfig itsConfig;

  @Inject
  public ActionController(PropertyExtractor propertyExtractor,
      RuleBase ruleBase, ActionExecutor actionExecutor, ItsConfig itsConfig) {
    this.propertyExtractor = propertyExtractor;
    this.ruleBase = ruleBase;
    this.actionExecutor = actionExecutor;
    this.itsConfig = itsConfig;
  }

  @Override
  public void onChangeEvent(ChangeEvent event) {
    if (!itsConfig.isEnabled(event)) {
      return;
    }

    Set<Set<Property>> propertiesCollections =
        propertyExtractor.extractFrom(event);
    for (Set<Property> properties : propertiesCollections) {
      Collection<ActionRequest> actions =
          ruleBase.actionRequestsFor(properties);
      if (!actions.isEmpty()) {
        for (Property property : properties) {
          if ("issue".equals(property.getKey())) {
            String issue = property.getValue();
            actionExecutor.execute(issue, actions, properties);
          }
        }
      }
    }
  }
}
