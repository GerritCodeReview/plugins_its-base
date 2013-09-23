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

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.Lists;
import com.google.gerrit.server.config.SitePath;
import com.google.inject.Inject;

import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection and matcher agains {@link Rule}s.
 */
public class RuleBase {
  private static final Logger log = LoggerFactory.getLogger(RuleBase.class);

  /**
   * File (relative to site) to load rules from
   */
  private static final String ITS_CONFIG_FILE = "etc" + File.separatorChar +
      "its" + File.separator + "action.config";

  /**
   * The section for rules within {@link #ITS_CONFIG_FILE}
   */
  private static final String RULE_SECTION = "rule";

  /**
   * The key for actions within {@link #ITS_CONFIG_FILE}
   */
  private static final String ACTION_KEY = "action";

  private final File sitePath;
  private final Rule.Factory ruleFactory;
  private final Condition.Factory conditionFactory;
  private final ActionRequest.Factory actionRequestFactory;

  private Collection<Rule> rules;

  public interface Factory {
    RuleBase create();
  }

  @Inject
  public RuleBase(@SitePath File sitePath, Rule.Factory ruleFactory,
      Condition.Factory conditionFactory,
      ActionRequest.Factory actionRequestFactory) {
    this.sitePath = sitePath;
    this.ruleFactory = ruleFactory;
    this.conditionFactory = conditionFactory;
    this.actionRequestFactory = actionRequestFactory;
    loadRules();
  }

  /**
   * Loads the rules for the RuleBase.
   *
   * Consider using {@link #loadRules()@}, as that method only loads the rules,
   * if they have not yet been loaded.
   */
  private void forceLoadRules() throws Exception {
    File configFile = new File(sitePath, ITS_CONFIG_FILE);
    if (configFile.exists()) {
      FileBasedConfig cfg = new FileBasedConfig(configFile, FS.DETECTED);
      cfg.load();

      rules = Lists.newArrayList();
      Collection<String> subsections = cfg.getSubsections(RULE_SECTION);
      for (String subsection : subsections) {
        Rule rule = ruleFactory.create(subsection);
        Collection<String> keys = cfg.getNames(RULE_SECTION, subsection);
        for (String key : keys) {
          String values[] = cfg.getStringList(RULE_SECTION, subsection, key);
          if (ACTION_KEY.equals(key)) {
            for (String value : values) {
              ActionRequest actionRequest = actionRequestFactory.create(value);
              rule.addActionRequest(actionRequest);
            }
          } else {
            for (String value : values) {
              Condition condition = conditionFactory.create(key, value);
              rule.addCondition(condition);
            }
          }
        }
        rules.add(rule);
      }
    } else {
      // configFile does not exist.
      log.warn("ITS actions configuration file (" + configFile + ") does not exist.");
      rules = Collections.emptySet();
    }
  }

  /**
   * Loads the rules for the RuleBase, if they have not yet been loaded.
   */
  private void loadRules() {
    if (rules == null) {
      try {
        forceLoadRules();
      } catch (Exception e) {
        log.error("Invalid ITS action configuration", e);
        rules = Collections.emptySet();
      }
    }
  }

  /**
   * Gets the action requests for a set of properties.
   *
   * @param properties The properties to search actions for.
   * @return Requests for the actions that should be fired.
   */
  public Collection<ActionRequest> actionRequestsFor(
      Iterable<Property> properties) {
    Collection<ActionRequest> ret = Lists.newLinkedList();
    for (Rule rule : rules) {
      ret.addAll(rule.actionRequestsFor(properties));
    }
    return ret;
  }
}
