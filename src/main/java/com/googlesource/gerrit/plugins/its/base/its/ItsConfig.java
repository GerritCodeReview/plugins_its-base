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

package com.googlesource.gerrit.plugins.its.base.its;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.gerrit.common.data.RefConfigSection;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.projects.CommentLinkInfo;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.DraftPublishedEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.server.project.RefPatternMatcher;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.its.base.validation.ItsAssociationPolicy;

import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


public class ItsConfig {
  private static final String PLUGIN = "plugin";

  private static final Logger log = LoggerFactory.getLogger(ItsConfig.class);

  private final String pluginName;
  private final ProjectCache projectCache;
  private final PluginConfigFactory pluginCfgFactory;
  private final Config gerritConfig;

  private static final ThreadLocal<Project.NameKey> currentProjectName =
      new ThreadLocal<Project.NameKey>() {
        @Override
        protected Project.NameKey initialValue() {
          return null;
        }
      };

  public static void setCurrentProjectName(Project.NameKey projectName) {
    currentProjectName.set(projectName);
  }

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
      return isEnabled(e.getProjectNameKey(), e.getRefName());
    } else if (event instanceof CommentAddedEvent) {
      CommentAddedEvent e = (CommentAddedEvent) event;
      return isEnabled(e.getProjectNameKey(), e.getRefName());
    } else if (event instanceof ChangeMergedEvent) {
      ChangeMergedEvent e = (ChangeMergedEvent) event;
      return isEnabled(e.getProjectNameKey(), e.getRefName());
    } else if (event instanceof ChangeAbandonedEvent) {
      ChangeAbandonedEvent e = (ChangeAbandonedEvent) event;
      return isEnabled(e.getProjectNameKey(), e.getRefName());
    } else if (event instanceof ChangeRestoredEvent) {
      ChangeRestoredEvent e = (ChangeRestoredEvent) event;
      return isEnabled(e.getProjectNameKey(), e.getRefName());
    } else if (event instanceof DraftPublishedEvent) {
      DraftPublishedEvent e = (DraftPublishedEvent) event;
      return isEnabled(e.getProjectNameKey(), e.getRefName());
    } else if (event instanceof RefUpdatedEvent) {
      RefUpdatedEvent e = (RefUpdatedEvent) event;
      return isEnabled(e.getProjectNameKey(), e.getRefName());
    } else {
      log.debug("Event " + event + " not recognised and ignored");
      return false;
    }
  }

  public boolean isEnabled(Project.NameKey projectNK, String refName) {
    ProjectState projectState = projectCache.get(projectNK);
    if (projectState == null) {
      log.error("Failed to check if " + pluginName + " is enabled for project "
          + projectNK.get() + ": Project " + projectNK.get() + " not found");
      return false;
    }

    for (ProjectState parentState : projectState.treeInOrder()) {
      PluginConfig parentCfg =
          pluginCfgFactory.getFromProjectConfig(parentState, pluginName);
      if (!"false".equals(parentCfg.getString("enabled"))
          && isEnabledForBranch(parentState, refName)) {
        return true;
      }
    }

    return !"false".equals(pluginCfgFactory.getFromProjectConfigWithInheritance(
        projectState, pluginName).getString("enabled", "false"))
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

    ret = getPluginConfigString("commentlink");
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
    String match =
        FluentIterable
            .from(getCommitLinkInfo(getCommentLinkName()))
            .filter(new Predicate<CommentLinkInfo>() {
              @Override
              public boolean apply(CommentLinkInfo input) {
                return input.match != null && !input.match.trim().isEmpty();
              }
            })
            .transform(new Function<CommentLinkInfo, String>() {
              @Override
              public String apply(CommentLinkInfo input) {
                return input.match;
              }
            })
            .last()
            .or(gerritConfig.getString("commentlink", getCommentLinkName(),
                "match"));
    Pattern ret = null;

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
    int index = getPluginConfigInt("commentlinkGroupIndex", 1);
    if (index < 0 || index > groupCount) {
      index = (groupCount == 0 ? 0 : 1);
    }
    return index;
  }

  /**
   * Gets how necessary it is to associate commits with issues
   *
   * @return policy on how necessary association with issues is
   */
  public ItsAssociationPolicy getItsAssociationPolicy() {
    ItsAssociationPolicy legacyItsAssociationPolicy =
        gerritConfig.getEnum("commentLink", getCommentLinkName(),
            "association", ItsAssociationPolicy.OPTIONAL);
    return getPluginConfigEnum("association", legacyItsAssociationPolicy);
  }

  private String getPluginConfigString(String key) {
    return getCurrentPluginConfig().getString(key,
        gerritConfig.getString(PLUGIN, pluginName, key));
  }

  private int getPluginConfigInt(String key, int defaultValue) {
    return getCurrentPluginConfig().getInt(key,
        gerritConfig.getInt(PLUGIN, pluginName, key, defaultValue));
  }

  private <T extends Enum<?>> T getPluginConfigEnum(String key, T defaultValue) {
    return getCurrentPluginConfig().getEnum(key,
        gerritConfig.getEnum(PLUGIN, pluginName, key, defaultValue));
  }

  private PluginConfig getCurrentPluginConfig() {
    NameKey projectName = currentProjectName.get();
    if (projectName != null) {
      try {
        return pluginCfgFactory.getFromProjectConfigWithInheritance(
            projectName, pluginName);
      } catch (NoSuchProjectException e) {
        log.error("Cannot access " + projectName + " configuration for plugin "
            + pluginName, e);
      }
    }
    return new PluginConfig(pluginName, new Config());
  }

  private List<CommentLinkInfo> getCommitLinkInfo(final String commentLinkName) {
    NameKey projectName = currentProjectName.get();
    if (projectName != null) {
      List<CommentLinkInfo> commentLinks =
          projectCache.get(projectName).getCommentLinks();
      return FluentIterable.from(commentLinks)
          .filter(new Predicate<CommentLinkInfo>() {
            @Override
            public boolean apply(CommentLinkInfo input) {
              return input.name.equals(commentLinkName);
            }
          }).toList();
    }
    return Collections.emptyList();
  }
}
