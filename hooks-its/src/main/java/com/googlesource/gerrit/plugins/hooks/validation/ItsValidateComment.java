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
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.ReceiveCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;
import com.googlesource.gerrit.plugins.hooks.its.ItsName;
import com.googlesource.gerrit.plugins.hooks.util.IssueExtractor;

public class ItsValidateComment implements CommitValidationListener {

  private static final Logger log = LoggerFactory
      .getLogger(ItsValidateComment.class);

  @Inject
  private ItsFacade client;

  @Inject
  @GerritServerConfig
  private Config gerritConfig;

  @Inject @ItsName
  private String itsName;

  @Inject
  private IssueExtractor issueExtractor;

  public List<CommitValidationMessage> validCommit(ReceiveCommand cmd, RevCommit commit) throws CommitValidationException {

    HashMap<Pattern, ItsAssociationPolicy> regexes = getCommentRegexMap();
    if (regexes.size() == 0) {
      return Collections.emptyList();
    }

    String message = commit.getFullMessage();
    log.debug("Searching comment " + message.trim() + " for patterns "
        + regexes);

    String issueId = null;
    ItsAssociationPolicy associationPolicy = ItsAssociationPolicy.OPTIONAL;
    Pattern pattern = null;
    for (Entry<Pattern, ItsAssociationPolicy> entry : regexes.entrySet()) {
      pattern = entry.getKey();
      Matcher matcher = pattern.matcher(message);
      associationPolicy = entry.getValue();
      if (matcher.find()) {
        issueId = issueExtractor.extractMatchedWorkItems(matcher);
        log.debug("Pattern matched on comment '{}' with issue id '{}'",
            message.trim(), issueId);
        break;
      }
    }

    String validationMessage = null;
    if (pattern != null && issueId == null) {
      validationMessage =
          "Missing issue-id in commit message\n"
              + "Commit "
              + commit.getId().getName()
              + " not associated to any issue\n"
              + "\n"
              + "Hint: insert one or more issue-id anywhere in the commit message.\n"
              + "      Issue-ids are strings matching " + pattern.pattern() + "\n"
              + "      and are pointing to existing tickets on "
              + client.name() + " Issue-Tracker";
    } else if (pattern != null && !isWorkitemPresent(issueId, message)) {
      validationMessage =
          "Issue " + issueId + " not found or visible in " + client.name()
              + " Issue-Tracker";
    } else {
      return Collections.emptyList();
    }

    switch (associationPolicy) {
      case MANDATORY:
        throw new CommitValidationException(validationMessage.split("\n")[0],
            Collections.singletonList(new CommitValidationMessage("\n"
                + validationMessage + "\n", false)));

      case SUGGESTED:
        return Collections.singletonList(new CommitValidationMessage("\n"
            + validationMessage + "\n", false));

      default:
        return Collections.emptyList();
    }
  }

  private boolean isWorkitemPresent(String issueId, String comment) {
    boolean exist = false;
    if (issueId != null) {
      try {
        if (!client.exists(issueId)) {
          log.warn("Workitem " + issueId + " declared in the comment "
              + comment + " but not found on ITS");
        } else {
          exist = true;
          log.debug("Workitem " + issueId + " found");
        }
      } catch (IOException ex) {
        log.warn("Unexpected error accessint ITS", ex);
      }
    } else {
      log.debug("Rejecting commit: no pattern matched on comment " + comment);
    }
    return exist;
  }

  private HashMap<Pattern, ItsAssociationPolicy> getCommentRegexMap() {
    HashMap<Pattern, ItsAssociationPolicy> regexMap = new HashMap<Pattern, ItsAssociationPolicy>();

    Pattern pattern = issueExtractor.getPattern();
    if (pattern != null) {
      regexMap
          .put(pattern, gerritConfig.getEnum("commentLink",
              itsName, "association", ItsAssociationPolicy.OPTIONAL));
    }

    return regexMap;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(
      CommitReceivedEvent receiveEvent) throws CommitValidationException {
    return validCommit(receiveEvent.command, receiveEvent.commit);
  }
}
