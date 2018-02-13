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

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.SitePath;
import com.google.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Collection and matcher against {@link Rule}s. */
public class RuleBase {
  private static final Logger log = LoggerFactory.getLogger(RuleBase.class);

  /** File beginning (relative to site) to load rules from */
  private static final String ITS_CONFIG_FILE_START =
      "etc" + File.separatorChar + "its" + File.separator + "actions";

  /** File end to load rules from */
  private static final String ITS_CONFIG_FILE_END = ".config";

  /** The section for rules within rulebases */
  private static final String RULE_SECTION = "rule";

  /** The key for actions within rulebases */
  private static final String ACTION_KEY = "action";

  private final Path sitePath;
  private final Rule.Factory ruleFactory;
  private final Condition.Factory conditionFactory;
  private final ActionRequest.Factory actionRequestFactory;
  private final String pluginName;

  private Collection<Rule> rules;

  public interface Factory {
    RuleBase create();
  }

  @Inject
  public RuleBase(
      @SitePath Path sitePath,
      Rule.Factory ruleFactory,
      Condition.Factory conditionFactory,
      ActionRequest.Factory actionRequestFactory,
      @PluginName String pluginName) {
    this.sitePath = sitePath;
    this.ruleFactory = ruleFactory;
    this.conditionFactory = conditionFactory;
    this.actionRequestFactory = actionRequestFactory;
    this.pluginName = pluginName;
    reloadRules();
  }

  /**
   * Adds rules from a file to the RuleBase.
   *
   * <p>If the given file does not exist, it is silently ignored
   *
   * @param ruleFile File from which to read the rules
   */
  private void addRulesFromFile(File ruleFile) {
    if (ruleFile.exists()) {
      FileBasedConfig cfg = new FileBasedConfig(ruleFile, FS.DETECTED);
      try {
        cfg.load();
      } catch (IOException | ConfigInvalidException e) {
        log.error("Invalid ITS action configuration", e);
        return;
      }

      Collection<String> subsections = cfg.getSubsections(RULE_SECTION);
      for (String subsection : subsections) {
        Rule rule = ruleFactory.create(subsection);
        Collection<String> keys = cfg.getNames(RULE_SECTION, subsection);
        for (String key : keys) {
          String[] values = cfg.getStringList(RULE_SECTION, subsection, key);
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
    }
  }

  /** Loads the rules for the RuleBase. */
  private void reloadRules() {
    rules = Lists.newArrayList();

    // Add rules from file with typo in filename
    //
    // While the documentation called for "actions.config" (Trailing "s" in
    // "actions"), the code previously only loaded "action.config" (No
    // trailing "s" in "action"). To give users time to gracefully migrate to
    // "actions.config" (with trailing "s", we (for now) load files from both
    // locations, but consider "actions.config" (with trailing "s" the
    // canonical place.
    File faultyNameRuleFile =
        new File(
            sitePath.toFile(),
            "etc" + File.separatorChar + "its" + File.separator + "action.config");
    if (faultyNameRuleFile.exists()) {
      log.warn(
          "Loading rules from deprecated 'etc/its/action.config' (No "
              + "trailing 's' in 'action'). Please migrate to "
              + "'etc/its/actions.config' (Trailing 's' in 'actions').");
      addRulesFromFile(faultyNameRuleFile);
    }

    // Add global rules
    File globalRuleFile = new File(sitePath.toFile(), ITS_CONFIG_FILE_START + ITS_CONFIG_FILE_END);
    addRulesFromFile(globalRuleFile);

    // Add its-specific rules
    File itsSpecificRuleFile =
        new File(sitePath.toFile(), ITS_CONFIG_FILE_START + "-" + pluginName + ITS_CONFIG_FILE_END);
    addRulesFromFile(itsSpecificRuleFile);

    if (!globalRuleFile.exists() && !itsSpecificRuleFile.exists()) {
      try {
        log.warn(
            "Neither global rule file "
                + globalRuleFile.getCanonicalPath()
                + " nor Its specific rule file"
                + itsSpecificRuleFile.getCanonicalPath()
                + " exist. Please configure "
                + "rules.");
      } catch (IOException e) {
        log.warn(
            "Neither global rule file nor Its specific rule files exist. "
                + "Please configure rules.");
      }
    }
  }

  /**
   * Gets the action requests for a set of properties.
   *
   * @param properties The properties to search actions for.
   * @return Requests for the actions that should be fired.
   */
  public Collection<ActionRequest> actionRequestsFor(Iterable<Property> properties) {
    Collection<ActionRequest> ret = Lists.newLinkedList();
    for (Rule rule : rules) {
      ret.addAll(rule.actionRequestsFor(properties));
    }
    return ret;
  }
}
