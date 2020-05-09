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
import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.jgit.lib.Config;

public class RulesConfigReader {
  /** The section for rules within rulebases */
  static final String RULE_SECTION = "rule";

  /** The key for actions within rulebases */
  static final String ACTION_KEY = "action";

  private final Rule.Factory ruleFactory;
  private final Condition.Factory conditionFactory;
  private final ActionRequest.Factory actionRequestFactory;

  @Inject
  RulesConfigReader(
      Rule.Factory ruleFactory,
      Condition.Factory conditionFactory,
      ActionRequest.Factory actionRequestFactory) {
    this.ruleFactory = ruleFactory;
    this.conditionFactory = conditionFactory;
    this.actionRequestFactory = actionRequestFactory;
  }

  Collection<Rule> getRulesFromConfig(Config cfg) {
    Collection<Rule> rules = new ArrayList<>();
    for (String subsection : cfg.getSubsections(RULE_SECTION)) {
      Rule rule = ruleFactory.create(subsection);
      for (String key : cfg.getNames(RULE_SECTION, subsection)) {
        String[] values = cfg.getStringList(RULE_SECTION, subsection, key);
        if (ACTION_KEY.equals(key)) {
          addActions(rule, values);
        } else {
          addConditions(rule, key, values);
        }
      }
      rules.add(rule);
    }
    return rules;
  }

  private void addActions(Rule rule, String[] values) {
    for (String value : values) {
      rule.addActionRequest(actionRequestFactory.create(value));
    }
  }

  private void addConditions(Rule rule, String key, String[] values) {
    for (String value : values) {
      rule.addCondition(conditionFactory.create(key, value));
    }
  }
}
