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

import com.google.common.collect.ImmutableList;
import com.google.gerrit.reviewdb.client.Project;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.GlobalRulesFileName;
import com.googlesource.gerrit.plugins.its.base.ItsPath;
import com.googlesource.gerrit.plugins.its.base.PluginRulesFileName;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Collection and matcher against {@link Rule}s. */
public class RuleBase {
  private static final Logger log = LoggerFactory.getLogger(RuleBase.class);

  private final File globalRuleFile;
  private final File itsSpecificRuleFile;
  private final RulesConfigReader rulesConfigReader;
  private final ItsRulesProjectCache rulesProjectCache;

  private Collection<Rule> rules;

  public interface Factory {
    RuleBase create();
  }

  @Inject
  public RuleBase(
      @ItsPath Path itsPath,
      @GlobalRulesFileName String globalRulesFileName,
      @PluginRulesFileName String pluginRulesFileName,
      ItsRulesProjectCache rulesProjectCache,
      RulesConfigReader rulesConfigReader) {
    this.globalRuleFile = itsPath.resolve(globalRulesFileName).toFile();
    this.itsSpecificRuleFile = itsPath.resolve(pluginRulesFileName).toFile();
    this.rulesConfigReader = rulesConfigReader;
    this.rulesProjectCache = rulesProjectCache;
    this.rules =
        new ImmutableList.Builder<Rule>()
            .addAll(getRulesFromFile(globalRuleFile))
            .addAll(getRulesFromFile(itsSpecificRuleFile))
            .build();
  }

  /**
   * Gets rules from a file.
   *
   * <p>If the given file does not exist, it is silently ignored
   *
   * @param ruleFile File from which to read the rules
   * @return A collection of rules or an empty collection if the file does not exist or contains an
   *     invalid configuration
   */
  private Collection<Rule> getRulesFromFile(File ruleFile) {
    if (ruleFile.exists()) {
      FileBasedConfig cfg = new FileBasedConfig(ruleFile, FS.DETECTED);
      try {
        cfg.load();
        return rulesConfigReader.getRulesFromConfig(cfg);
      } catch (IOException | ConfigInvalidException e) {
        log.error("Invalid ITS action configuration", e);
      }
    }
    return Collections.emptyList();
  }

  /**
   * Gets the action requests for a set of properties.
   *
   * @param properties The properties to search actions for.
   * @return Requests for the actions that should be fired.
   */
  public Collection<ActionRequest> actionRequestsFor(Map<String, String> properties) {
    String projectName = properties.get("project");
    Collection<Rule> fromProjectConfig = rulesProjectCache.get(new Project.NameKey(projectName));
    Collection<Rule> rulesToAdd = !fromProjectConfig.isEmpty() ? fromProjectConfig : rules;
    if (rulesToAdd.isEmpty() && !globalRuleFile.exists() && !itsSpecificRuleFile.exists()) {
      log.warn(
          "Neither global rule file {} nor Its specific rule file {} exist and no rules are "
              + "configured for project {}. Please configure rules.",
          globalRuleFile,
          itsSpecificRuleFile,
          projectName);
      return Collections.emptyList();
    }
    Collection<ActionRequest> actions = new ArrayList<>();
    for (Rule rule : rulesToAdd) {
      actions.addAll(rule.actionRequestsFor(properties));
    }
    return actions;
  }
}
