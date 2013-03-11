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

import org.eclipse.jgit.lib.Config;

import com.google.common.base.Strings;
import com.google.gerrit.server.config.AnonymousCowardName;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.events.AccountAttribute;
import com.google.gerrit.server.events.ApprovalAttribute;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeAttribute;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;

public class GerritHookFilterAddComment extends GerritHookFilter  {

  @Inject
  private ItsFacade its;

  @Inject @AnonymousCowardName
  private String anonymousCowardName;

  @Inject
  @GerritServerConfig
  private Config gerritConfig;

  @Override
  public void doFilter(CommentAddedEvent hook) throws IOException {
    if (!(gerritConfig.getBoolean(its.name(), null, "commentOnCommentAdded",
        true))) {
      return;
    }

    String comment = getComment(hook);
    addComment(hook.change, comment);
  }

  @Override
  public void doFilter(ChangeMergedEvent hook) throws IOException {
    if (!(gerritConfig.getBoolean(its.name(), null, "commentOnChangeMerged",
        true))) {
      return;
    }

    String comment = getComment(hook);
    addComment(hook.change, comment);
  }

  @Override
  public void doFilter(ChangeAbandonedEvent hook) throws IOException {
    if (!(gerritConfig.getBoolean(its.name(), null, "commentOnChangeAbandoned",
        true))) {
      return;
    }
    String comment = getComment(hook);
    addComment(hook.change, comment);
  }

  @Override
  public void doFilter(ChangeRestoredEvent hook) throws IOException {
    if (!(gerritConfig.getBoolean(its.name(), null, "commentOnChangeRestored",
        true))) {
      return;
    }
    String comment = getComment(hook);
    addComment(hook.change, comment);
  }

  private String getCommentPrefix(ChangeAttribute change) {
    return getChangeIdUrl(change) + " | ";
  }

  private String formatAccountAttribute(AccountAttribute who) {
    if (who != null && !Strings.isNullOrEmpty(who.name)) {
      return who.name;
    }
    return anonymousCowardName;
  }

  private String getComment(ChangeAttribute change, ChangeEvent hook, AccountAttribute who, String what) {
    return getCommentPrefix(change) + "change " + what + " [by "
        + formatAccountAttribute(who) + "]";
  }

  private String getComment(ChangeRestoredEvent hook) {
    return getComment(hook.change, hook, hook.restorer, "RESTORED");
  }

  private String getComment(ChangeAbandonedEvent hook) {
    return getComment(hook.change, hook, hook.abandoner, "ABANDONED");
  }

  private String getComment(ChangeMergedEvent hook) {
    return getComment(hook.change, hook, hook.submitter, "APPROVED and MERGED");
  }

  private String getChangeIdUrl(ChangeAttribute change) {
    final String url = change.url;
    String changeId = change.id;
    return its.createLinkForWebui(url, "Gerrit Change " + changeId);
  }

  private String getComment(CommentAddedEvent commentAdded) {
    StringBuilder comment = new StringBuilder(getCommentPrefix(commentAdded.change));

    if (commentAdded.approvals != null && commentAdded.approvals.length > 0) {
      comment.append("Code-Review: ");
      for (ApprovalAttribute approval : commentAdded.approvals) {
        String value = getApprovalValue(approval);
        if (value != null) {
          comment.append(getApprovalType(approval) + ":" + value + " ");
        }
      }
    }

    comment.append(commentAdded.comment + " ");
    comment.append("[by " + formatAccountAttribute(commentAdded.author) + "]");
    return comment.toString();
  }

  private String getApprovalValue(ApprovalAttribute approval) {
    if (approval.value.equals("0")) {
      return null;
    }

    if (approval.value.charAt(0) != '-') {
      return "+" + approval.value;
    } else {
      return approval.value;
    }
  }

  private String getApprovalType(ApprovalAttribute approval) {
    if (approval.type.equalsIgnoreCase("CRVW")) {
      return "Reviewed";
    } else if (approval.type.equalsIgnoreCase("VRIF")) {
      return "Verified";
    } else
      return approval.type;
  }

  private void addComment(ChangeAttribute change, String comment)
      throws IOException {
    String gitComment = change.subject;;
    String[] issues = getIssueIds(gitComment);

    for (String issue : issues) {
      its.addComment(issue, comment);
    }
  }
}
