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

import static java.util.stream.Collectors.toList;

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.common.data.AccessSection;
import com.google.gerrit.entities.Project;
import com.google.gerrit.entities.Project.NameKey;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.api.projects.CommentLinkInfo;
import com.google.gerrit.server.config.GerritInstanceId;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.PrivateStateChangedEvent;
import com.google.gerrit.server.events.RefEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.events.WorkInProgressStateChangedEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectCache;
import com.google.gerrit.server.project.ProjectState;
import com.google.gerrit.server.project.RefPatternMatcher;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.validation.ItsAssociationPolicy;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.eclipse.jgit.lib.Config;

public class ItsConfig {
  private static final String PLUGIN = "plugin";

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final String pluginName;
  private final ProjectCache projectCache;
  private final PluginConfigFactory pluginCfgFactory;
  private final Config gerritConfig;
  private String instanceId;

  private static final ThreadLocal<Project.NameKey> currentProjectName =
      ThreadLocal.withInitial(() -> null);

  public static void setCurrentProjectName(Project.NameKey projectName) {
    currentProjectName.set(projectName);
  }

  @Inject
  public ItsConfig(
      @PluginName String pluginName,
      ProjectCache projectCache,
      PluginConfigFactory pluginCfgFactory,
      @GerritServerConfig Config gerritConfig,
      @GerritInstanceId String instanceId) {
    this.pluginName = pluginName;
    this.projectCache = projectCache;
    this.pluginCfgFactory = pluginCfgFactory;
    this.gerritConfig = gerritConfig;
    this.instanceId = instanceId;
  }

  // Plugin enablement --------------------------------------------------------

  public boolean isEnabled(RefEvent event) {
    if (instanceId != null && !instanceId.equals(event.instanceId)) {
      logger.atFine().log(
          "Event %s is coming from a remote Gerrit instance-id (%s)", event, event.instanceId);
      return false;
    }

    if (event instanceof PatchSetCreatedEvent
        || event instanceof CommentAddedEvent
        || event instanceof ChangeMergedEvent
        || event instanceof ChangeAbandonedEvent
        || event instanceof ChangeRestoredEvent
        || event instanceof PrivateStateChangedEvent
        || event instanceof WorkInProgressStateChangedEvent
        || event instanceof RefUpdatedEvent) {
      return isEnabled(event.getProjectNameKey(), event.getRefName());
    }
    logger.atFine().log("Event %s not recognised and ignored", event);
    return false;
  }

  public boolean isEnabled(Project.NameKey projectNK, String refName) {
    Optional<ProjectState> projectState = projectCache.get(projectNK);
    if (!projectState.isPresent()) {
      logger.atSevere().log(
          "Failed to check if %s is enabled for project %s: Project not found",
          pluginName, projectNK.get());
      return false;
    }
    return isEnforcedByAnyParentProject(refName, projectState.get())
        || (isEnabledForProject(projectState.get())
            && isEnabledForBranch(projectState.get(), refName));
  }

  private boolean isEnforcedByAnyParentProject(String refName, ProjectState projectState) {
    for (ProjectState parentState : projectState.treeInOrder()) {
      PluginConfig parentCfg = pluginCfgFactory.getFromProjectConfig(parentState, pluginName);
      if ("enforced".equals(parentCfg.getString("enabled", "false"))
          && isEnabledForBranch(parentState, refName)) {
        return true;
      }
    }
    return false;
  }

  private boolean isEnabledForProject(ProjectState projectState) {
    return !"false"
        .equals(
            pluginCfgFactory
                .getFromProjectConfigWithInheritance(projectState, pluginName)
                .getString("enabled", "false"));
  }

  private boolean isEnabledForBranch(ProjectState project, String refName) {
    String[] refPatterns =
        pluginCfgFactory
            .getFromProjectConfigWithInheritance(project, pluginName)
            .getStringList("branch");
    if (refPatterns.length == 0) {
      return true;
    }
    for (String refPattern : refPatterns) {
      if (AccessSection.isValidRefSectionName(refPattern) && match(refName, refPattern)) {
        return true;
      }
    }
    return false;
  }

  private boolean match(String refName, String refPattern) {
    return RefPatternMatcher.getMatcher(refPattern).match(refName, null);
  }

  // Project association
  public Optional<String> getItsProjectName(Project.NameKey projectNK) {
    Optional<ProjectState> projectState = projectCache.get(projectNK);
    if (!projectState.isPresent()) {
      return Optional.empty();
    }
    return Optional.ofNullable(
        pluginCfgFactory
            .getFromProjectConfig(projectState.get(), pluginName)
            .getString("its-project"));
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
   *
   * <p>The index of the group that holds the issue id is {@link #getIssuePatternGroupIndex()}.
   *
   * @return the regular expression, or {@code null}, if there is no pattern to match issue ids.
   */
  public Pattern getIssuePattern() {
    Optional<String> match =
        getCommentLinkInfo(getCommentLinkName()).stream()
            .filter(input -> input.match != null && !input.match.trim().isEmpty())
            .map(input -> input.match)
            .reduce((a, b) -> b);

    String defPattern = gerritConfig.getString("commentlink", getCommentLinkName(), "match");

    if (!match.isPresent() && defPattern == null) {
      return null;
    }

    return Pattern.compile(match.orElse(defPattern));
  }

  /**
   * Gets the index of the group in the issue pattern that holds the issue id.
   *
   * <p>The corresponding issue pattern is {@link #getIssuePattern()}
   *
   * @return the group index for {@link #getIssuePattern()} that holds the issue id. The group index
   *     is guaranteed to be a valid group index.
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
   * Pattern to skip the mandatory check for an issue. Can be used to explicitly bypass the
   * mandatory issue pattern check for some commits.
   *
   * <p>When no pattern is specified, it will return a pattern which never matches.
   */
  public Optional<Pattern> getDummyIssuePattern() {
    return Optional.ofNullable(getPluginConfigString("dummyIssuePattern")).map(Pattern::compile);
  }

  /**
   * Gets how necessary it is to associate commits with issues
   *
   * @return policy on how necessary association with issues is
   */
  public ItsAssociationPolicy getItsAssociationPolicy() {
    ItsAssociationPolicy legacyItsAssociationPolicy =
        gerritConfig.getEnum(
            "commentlink", getCommentLinkName(), "association", ItsAssociationPolicy.OPTIONAL);

    return getPluginConfigEnum("association", legacyItsAssociationPolicy);
  }

  private String getPluginConfigString(String key) {
    return getCurrentPluginConfig().getString(key, gerritConfig.getString(PLUGIN, pluginName, key));
  }

  private int getPluginConfigInt(String key, int defaultValue) {
    return getCurrentPluginConfig()
        .getInt(key, gerritConfig.getInt(PLUGIN, pluginName, key, defaultValue));
  }

  private <T extends Enum<?>> T getPluginConfigEnum(String key, T defaultValue) {
    return getCurrentPluginConfig()
        .getEnum(key, gerritConfig.getEnum(PLUGIN, pluginName, key, defaultValue));
  }

  private PluginConfig getCurrentPluginConfig() {
    NameKey projectName = currentProjectName.get();
    if (projectName != null) {
      try {
        return pluginCfgFactory.getFromProjectConfigWithInheritance(projectName, pluginName);
      } catch (NoSuchProjectException e) {
        logger.atSevere().withCause(e).log(
            "Cannot access %s configuration for plugin %s", projectName, pluginName);
      }
    }
    return new PluginConfig(pluginName, new Config());
  }

  private List<CommentLinkInfo> getCommentLinkInfo(final String commentlinkName) {
    NameKey projectName = currentProjectName.get();
    if (projectName != null) {
      List<CommentLinkInfo> commentlinks = projectCache.get(projectName).get().getCommentLinks();
      return commentlinks.stream()
          .filter(input -> input.name.equals(commentlinkName))
          .collect(toList());
    }
    return Collections.emptyList();
  }
}
