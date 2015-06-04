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
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.server.project.RefPatternMatcher;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.hooks.validation.ItsAssociationPolicy;

import java.util.regex.Pattern;

import org.eclipse.jgit.lib.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ItsConfig {
  private static final Logger log = LoggerFactory.getLogger(ItsConfig.class);

  private final String pluginName;
  private final ProjectCache projectCache;
  private final PluginConfigFactory pluginCfgFactory;
  private final Config gerritConfig;

  @Inject
  public ItsConfig(@PluginName String pluginName, ProjectCache projectCache,
      PluginConfigFactory pluginCfgFactory, @GerritServerConfig Config gerritConfig) {
    this.pluginName = pluginName;
    this.projectCache = projectCache;
    this.pluginCfgFactory = pluginCfgFactory;
    this.gerritConfig = gerritConfig;
  }

  // Plugin enablement --------------------------------------------------------

  public boolean isEnabled(Event event) {
    if (event instanceof PatchSetCreatedEvent) {
      PatchSetCreatedEvent e = (PatchSetCreatedEvent) event;
      return isEnabled(e.change.project, e.getRefName());
    } else if (event instanceof CommentAddedEvent) {
      CommentAddedEvent e = (CommentAddedEvent) event;
      return isEnabled(e.change.project, e.getRefName());
    } else if (event instanceof ChangeMergedEvent) {
      ChangeMergedEvent e = (ChangeMergedEvent) event;
      return isEnabled(e.change.project, e.getRefName());
    } else if (event instanceof ChangeAbandonedEvent) {
      ChangeAbandonedEvent e = (ChangeAbandonedEvent) event;
      return isEnabled(e.change.project, e.getRefName());
    } else if (event instanceof ChangeRestoredEvent) {
      ChangeRestoredEvent e = (ChangeRestoredEvent) event;
      return isEnabled(e.change.project, e.getRefName());
    } else if (event instanceof RefUpdatedEvent) {
      RefUpdatedEvent e = (RefUpdatedEvent) event;
      return isEnabled(e.refUpdate.project, e.refUpdate.refName);
    } else {
      log.debug("Event " + event + " not recognised and ignored");
      return false;
    }
  }

  public boolean isEnabled(String project, String refName) {
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
          && isEnabledForBranch(parentState, refName)) {
        return true;
      }
    }

    return pluginCfgFactory.getFromProjectConfigWithInheritance(
        projectState, pluginName).getBoolean("enabled", false)
        && isEnabledForBranch(projectState, refName);
  }

  private boolean isEnabledForBranch(ProjectState project, String refName) {
    String[] refPatterns =
        pluginCfgFactory.getFromProjectConfigWithInheritance(project,
            pluginName).getStringList("branch");
    if (refPatterns.length == 0) {
      return true;
    }
    for (String refPattern : refPatterns) {
      if (RefConfigSection.isValid(refPattern) && match(refName, refPattern)) {
        return true;
      }
    }
    return false;
  }

  private boolean match(String refName, String refPattern) {
    return RefPatternMatcher.getMatcher(refPattern).match(refName, null);
  }

  // Issue association --------------------------------------------------------

  /**
   * Gets the name of the comment link that should be used
   *
   * @return name of the comment link that should be used
   */
  public String getCommentLinkName() {
    String ret;

    ret = gerritConfig.getString(pluginName, null, "commentlink");
    if (ret == null) {
      ret = pluginName;
    }

    return ret;
  }

  /**
   * Gets the regular expression used to identify issue ids.
   * <p>
   * The index of the group that holds the issue id is
   * {@link #getIssuePatternGroupIndex()}.
   *
   * @return the regular expression, or {@code null}, if there is no pattern
   *    to match issue ids.
   */
  public Pattern getIssuePattern() {
    Pattern ret = null;
    String match = gerritConfig.getString("commentlink",
        getCommentLinkName(), "match");
    if (match != null) {
      ret = Pattern.compile(match);
    }
    return ret;
  }

  /**
   * Gets the index of the group in the issue pattern that holds the issue id.
   * <p>
   * The corresponding issue pattern is {@link #getIssuePattern()}
   *
   * @return the group index for {@link #getIssuePattern()} that holds the
   *     issue id. The group index is guaranteed to be a valid group index.
   */
  public int getIssuePatternGroupIndex() {
    Pattern pattern = getIssuePattern();
    int groupCount = pattern.matcher("").groupCount();
    int index = gerritConfig.getInt(pluginName, "commentlinkGroupIndex", 1);
    if (index < 0 || index > groupCount) {
      index = (groupCount == 0 ? 0 : 1);
    }
    return index;
  }

  /**
   * Gets how necessary it is to associate commits with issues
   * @return policy on how necessary assiaction with issues is
   */
  public ItsAssociationPolicy getItsAssociationPolicy() {
    return gerritConfig.getEnum("commentlink", getCommentLinkName(),
        "association", ItsAssociationPolicy.OPTIONAL);
  }
}
