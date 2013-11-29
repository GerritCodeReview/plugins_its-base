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

package com.googlesource.gerrit.plugins.hooks.validation;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;
import com.googlesource.gerrit.plugins.hooks.util.IssueExtractor;

public class ItsValidateComment implements CommitValidationListener {

  private static final Logger log = LoggerFactory
      .getLogger(ItsValidateComment.class);

  @Inject
  private ItsFacade client;

  @Inject
  @GerritServerConfig
  private Config gerritConfig;

  @Inject @PluginName
  private String pluginName;

  @Inject
  private IssueExtractor issueExtractor;

  private List<CommitValidationMessage> validCommit(ReceiveCommand cmd, RevCommit commit) throws CommitValidationException {
    List<CommitValidationMessage> ret = Lists.newArrayList();
    ItsAssociationPolicy associationPolicy = getItsAssociationPolicy();

    switch (associationPolicy) {
      case MANDATORY:
      case SUGGESTED:
        String commitMessage = commit.getFullMessage();
        String[] issueIds = issueExtractor.getIssueIds(commitMessage);
        String synopsis = null;
        String details = null;
        if (issueIds.length > 0) {
          List<String> nonExistingIssueIds = Lists.newArrayList();
          for (String issueId : issueIds) {
            boolean exists = false;
            try {
              exists = client.exists(issueId);
            } catch (IOException e) {
              synopsis = "Failed to check whether or not issue " + issueId
                  + " exists";
              log.warn(synopsis, e);
              details = e.toString();
              ret.add(commitValidationFailure(synopsis, details));
            }
            if (!exists) {
              nonExistingIssueIds.add(issueId);
            }
          }

          if (!nonExistingIssueIds.isEmpty()) {
            synopsis = "Non-existing issue ids referenced in commit message";

            StringBuilder sb = new StringBuilder();
            sb.append("The issue-ids\n");
            for (String issueId : nonExistingIssueIds) {
              sb.append("    * ");
              sb.append(issueId);
              sb.append("\n");
            }
            sb.append("are referenced in the commit message of\n");
            sb.append(commit.getId().getName());
            sb.append(",\n");
            sb.append("but do not exist in ");
            sb.append(client.name());
            sb.append(" Issue-Tracker");
            details = sb.toString();

            ret.add(commitValidationFailure(synopsis, details));
          }
        } else {
          synopsis = "Missing issue-id in commit message";

          StringBuilder sb = new StringBuilder();
          sb.append("Commit ");
          sb.append(commit.getId().getName());
          sb.append(" not associated to any issue\n");
          sb.append("\n");
          sb.append("Hint: insert one or more issue-id anywhere in the ");
          sb.append("commit message.\n");
          sb.append("      Issue-ids are strings matching ");
          sb.append(issueExtractor.getPattern().pattern());
          sb.append("\n");
          sb.append("      and are pointing to existing tickets on ");
          sb.append(client.name());
          sb.append(" Issue-Tracker");
          details = sb.toString();

          ret.add(commitValidationFailure(synopsis, details));
        }
        break;
      case OPTIONAL:
      default:
        break;
    }
    return ret;
  }

  private ItsAssociationPolicy getItsAssociationPolicy() {
    return gerritConfig.getEnum("commentLink", pluginName, "association",
        ItsAssociationPolicy.OPTIONAL);
  }

  private CommitValidationMessage commitValidationFailure(
      String synopsis, String details) throws CommitValidationException {
    CommitValidationMessage ret =
        new CommitValidationMessage(synopsis + "\n" + details, false);
    if (getItsAssociationPolicy() == ItsAssociationPolicy.MANDATORY) {
      throw new CommitValidationException(synopsis,
          Collections.singletonList(ret));
    }
    return ret;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    return validCommit(receiveEvent.command, receiveEvent.commit);
  }
}
