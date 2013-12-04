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

package com.googlesource.gerrit.plugins.hooks.its;

import com.google.gerrit.common.data.RefConfigSection;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.server.project.RefPatternMatcher;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItsConfig {
  private static final Logger log = LoggerFactory.getLogger(ItsConfig.class);

  private final String pluginName;
  private final ProjectCache projectCache;
  private final PluginConfigFactory pluginCfgFactory;
  private final Provider<CurrentUser> self;

  @Inject
  public ItsConfig(@PluginName String pluginName, ProjectCache projectCache,
      PluginConfigFactory pluginCfgFactory, Provider<CurrentUser> self) {
    this.pluginName = pluginName;
    this.projectCache = projectCache;
    this.pluginCfgFactory = pluginCfgFactory;
    this.self = self;
  }

  public boolean isEnabled(ChangeEvent event) {
    if (event instanceof PatchSetCreatedEvent) {
      PatchSetCreatedEvent e = (PatchSetCreatedEvent) event;
      return isEnabled(e.change.project, e.change.branch);
    } else if (event instanceof CommentAddedEvent) {
      CommentAddedEvent e = (CommentAddedEvent) event;
      return isEnabled(e.change.project, e.change.branch);
    } else if (event instanceof ChangeMergedEvent) {
      ChangeMergedEvent e = (ChangeMergedEvent) event;
      return isEnabled(e.change.project, e.change.branch);
    } else if (event instanceof ChangeAbandonedEvent) {
      ChangeAbandonedEvent e = (ChangeAbandonedEvent) event;
      return isEnabled(e.change.project, e.change.branch);
    } else if (event instanceof ChangeRestoredEvent) {
      ChangeRestoredEvent e = (ChangeRestoredEvent) event;
      return isEnabled(e.change.project, e.change.branch);
    } else if (event instanceof RefUpdatedEvent) {
      RefUpdatedEvent e = (RefUpdatedEvent) event;
      return isEnabled(e.refUpdate.project, e.refUpdate.refName);
    } else {
      log.debug("Event " + event + " not recognised and ignored");
      return false;
    }
  }

  public boolean isEnabled(String project, String branch) {
    ProjectState projectState = projectCache.get(new Project.NameKey(project));
    if (projectState == null) {
      log.error("Failed to check if " + pluginName + " is enabled for project "
          + project + ": Project " + project + " not found");
      return false;
    }

    for (ProjectState parentState : projectState.treeInOrder()) {
      PluginConfig parentCfg =
          pluginCfgFactory.getFromProjectConfig(parentState, pluginName);
      if ("enforced".equals(parentCfg.getString("enabled"))
          && isEnabledForBranch(parentState, branch)) {
        return true;
      }
    }

    return pluginCfgFactory.getFromProjectConfigWithInheritance(
        projectState, pluginName).getBoolean("enabled", false)
        && isEnabledForBranch(projectState, branch);
  }

  private boolean isEnabledForBranch(ProjectState project, String branch) {
    String[] refPatterns =
        pluginCfgFactory.getFromProjectConfigWithInheritance(project,
            pluginName).getStringList("branch");
    if (refPatterns.length == 0) {
      return true;
    }
    for (String refPattern : refPatterns) {
      if (RefConfigSection.isValid(refPattern) && match(branch, refPattern)) {
        return true;
      }
    }
    return false;
  }

  private boolean match(String branch, String refPattern) {
    return RefPatternMatcher.getMatcher(refPattern)
        .match(branch, self.get().getUserName());
  }
}
