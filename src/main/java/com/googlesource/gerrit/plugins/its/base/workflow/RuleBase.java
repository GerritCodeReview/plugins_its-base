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
import com.google.gerrit.server.config.SitePath;
import com.google.gerrit.server.git.ProjectLevelConfig;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.GlobalRulesFileName;
import com.googlesource.gerrit.plugins.its.base.PluginRulesFileName;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Collection and matcher against {@link Rule}s. */
public class RuleBase {
  private static final Logger log = LoggerFactory.getLogger(RuleBase.class);

  private final Path itsPath;
  private final RulesConfigReader rulesConfigReader;
  private final String globalRulesFileName;
  private final String pluginRulesFileName;
  private final ProjectCache projectCache;

  private Collection<Rule> rules;

  public interface Factory {
    RuleBase create();
  }

  @Inject
  public RuleBase(
      @SitePath Path sitePath,
      RulesConfigReader rulesConfigReader,
      ProjectCache projectCache,
      @GlobalRulesFileName String globalRulesFileName,
      @PluginRulesFileName String pluginRulesFileName) {
    this.itsPath = sitePath.normalize().resolve("etc").resolve("its");
    this.rulesConfigReader = rulesConfigReader;
    this.projectCache = projectCache;
    this.globalRulesFileName = globalRulesFileName;
    this.pluginRulesFileName = pluginRulesFileName;
    reloadRules();
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

  /** Loads the rules for the RuleBase. */
  private void reloadRules() {
    rules = new ArrayList<>();

    // Add global rules
    File globalRuleFile = itsPath.resolve(globalRulesFileName).toFile();
    rules.addAll(getRulesFromFile(globalRuleFile));

    // Add its-specific rules
    File itsSpecificRuleFile = itsPath.resolve(pluginRulesFileName).toFile();
    rules.addAll(getRulesFromFile(itsSpecificRuleFile));

    if (!globalRuleFile.exists() && !itsSpecificRuleFile.exists()) {
      log.warn(
          "Neither global rule file {} nor Its specific rule file {} exist. Please configure rules.",
          globalRuleFile,
          itsSpecificRuleFile);
    }
  }

  /**
   * Gets the action requests for a set of properties.
   *
   * @param properties The properties to search actions for.
   * @return Requests for the actions that should be fired.
   */
  public Collection<ActionRequest> actionRequestsFor(Collection<Property> properties) {
    String projectName = getProjectFromProperties(properties);
    Collection<Rule> fromProjectConfig = getRulesForProject(projectName);
    Collection<Rule> rulesToAdd = !fromProjectConfig.isEmpty() ? fromProjectConfig : rules;

    Collection<ActionRequest> actions = new ArrayList<>();
    for (Rule rule : rulesToAdd) {
      actions.addAll(rule.actionRequestsFor(properties));
    }
    return actions;
  }

  private String getProjectFromProperties(Collection<Property> properties) {
    for (Property property : properties) {
      if ("project".equals(property.getKey())) {
        return property.getValue();
      }
    }
    return "";
  }

  private Collection<Rule> getRulesForProject(String projectName) {
    if (projectName.isEmpty()) {
      return Collections.emptyList();
    }
    Collection<Rule> projectRules = new ArrayList<>();
    try {
      ProjectState project = projectCache.checkedGet(new Project.NameKey(projectName));
      projectRules = readRulesFrom(project);
      if (projectRules.isEmpty()) {
        for (ProjectState parent : project.parents()) {
          projectRules = readRulesFrom(parent);
          if (!projectRules.isEmpty()) {
            break;
          }
        }
      }
    } catch (IOException e) {
      log.debug("Ignoring project specific rules: unable to open project {}.", projectName, e);
    }
    return projectRules;
  }

  private Collection<Rule> readRulesFrom(ProjectState project) {
    ProjectLevelConfig global = project.getConfig(globalRulesFileName);
    Config general = global.get();
    ProjectLevelConfig plugin = project.getConfig(pluginRulesFileName);
    Config pluginSpecific = plugin.get();
    return new ImmutableList.Builder<Rule>()
        .addAll(rulesConfigReader.getRulesFromConfig(general))
        .addAll(rulesConfigReader.getRulesFromConfig(pluginSpecific))
        .build();
  }
}
