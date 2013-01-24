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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.hooks.its.ItsName;

public class GerritHookFilter implements ChangeListener {
  private static final Logger log = LoggerFactory.getLogger(GerritHookFilter.class);

  @Inject @GerritServerConfig
  private Config gerritConfig;

  @Inject @ItsName
  private String itsName;

  @Inject
  private GitRepositoryManager repoManager;

  public String getComment(String projectName, String commitId)
      throws IOException {

    final Repository repo =
        repoManager.openRepository(new NameKey(projectName));
    try {
      RevWalk revWalk = new RevWalk(repo);
      RevCommit commit = revWalk.parseCommit(ObjectId.fromString(commitId));

      return commit.getFullMessage();
    } finally {
      repo.close();
    }
  }

  protected String[] getIssueIds(String gitComment) {
    List<Pattern> commentRegexList = getCommentRegexList();
    if (commentRegexList == null) return new String[] {};

    log.debug("Matching '" + gitComment + "' against " + commentRegexList);

    ArrayList<String> issues = new ArrayList<String>();
    for (Pattern pattern : commentRegexList) {
      Matcher matcher = pattern.matcher(gitComment);

      while (matcher.find()) {
        int groupCount = matcher.groupCount();
        for (int i = 1; i <= groupCount; i++) {
          String group = matcher.group(i);
          issues.add(group);
        }
      }
    }

    return issues.toArray(new String[issues.size()]);
  }

  protected Long[] getWorkItems(String gitComment) {
    List<Pattern> commentRegexList = getCommentRegexList();
    if (commentRegexList == null) return new Long[] {};

    log.debug("Matching '" + gitComment + "' against " + commentRegexList);

    ArrayList<Long> workItems = new ArrayList<Long>();

    for (Pattern pattern : commentRegexList) {
      Matcher matcher = pattern.matcher(gitComment);

      while (matcher.find()) {
        addMatchedWorkItems(workItems, matcher);
      }
    }

    return workItems.toArray(new Long[workItems.size()]);
  }

  private void addMatchedWorkItems(ArrayList<Long> workItems, Matcher matcher) {
    int groupCount = matcher.groupCount();
    for (int i = 1; i <= groupCount; i++) {

      String group = matcher.group(i);
      try {
        Long workItem = new Long(group);
        workItems.add(workItem);
      } catch (NumberFormatException e) {
        log.debug("matched string '" + group
            + "' is not a work item > skipping");
      }
    }
  }

  private List<Pattern> getCommentRegexList() {
    ArrayList<Pattern> regexList = new ArrayList<Pattern>();

    String match = gerritConfig.getString("commentLink", itsName, "match");
    if (match != null) {
      regexList.add(Pattern.compile(match));
    }

    return regexList;
  }

  public void doFilter(PatchSetCreatedEvent hook) throws IOException {
  }

  public void doFilter(CommentAddedEvent hook) throws IOException {
  }

  public void doFilter(ChangeMergedEvent hook) throws IOException {
  }

  public void doFilter(ChangeAbandonedEvent changeAbandonedHook)
      throws IOException {
  }

  public void doFilter(ChangeRestoredEvent changeRestoredHook)
      throws IOException {
  }

  public void doFilter(RefUpdatedEvent refUpdatedHook) throws IOException {
  }

  @Override
  public void onChangeEvent(ChangeEvent event) {
    try {
      if (event instanceof PatchSetCreatedEvent) {
        doFilter((PatchSetCreatedEvent) event);
      } else if (event instanceof CommentAddedEvent) {
        doFilter((CommentAddedEvent) event);
      } else if (event instanceof ChangeMergedEvent) {
        doFilter((ChangeMergedEvent) event);
      } else if (event instanceof ChangeAbandonedEvent) {
        doFilter((ChangeAbandonedEvent) event);
      } else if (event instanceof ChangeRestoredEvent) {
        doFilter((ChangeRestoredEvent) event);
      } else if (event instanceof RefUpdatedEvent) {
        doFilter((RefUpdatedEvent) event);
      } else {
        log.info("Event " + event + " not recognised and ignored");
      }
    } catch (Throwable e) {
      log.error("Event " + e + " processing failed", e);
    }
  }

  public String getUrl(PatchSetCreatedEvent hook) {
    return null;
  }
}
