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

package com.googlesource.gerrit.plugins.its.base.validation;

import com.google.common.collect.Lists;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.events.CommitReceivedEvent;
import com.google.gerrit.server.git.validators.CommitValidationException;
import com.google.gerrit.server.git.validators.CommitValidationListener;
import com.google.gerrit.server.git.validators.CommitValidationMessage;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacadeFactory;
import com.googlesource.gerrit.plugins.its.base.util.IssueExtractor;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.eclipse.jgit.revwalk.RevCommit;

public class ItsValidateComment implements CommitValidationListener {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Inject private ItsFacade client;

  @Inject @PluginName private String pluginName;

  @Inject private ItsConfig itsConfig;

  @Inject private ItsFacadeFactory itsFacadeFactory;

  @Inject private IssueExtractor issueExtractor;

  private enum ItsExistenceCheckResult {
    EXISTS,
    DOESNT_EXIST,
    CONNECTIVITY_FAILURE
  }

  private List<CommitValidationMessage> validCommit(Project.NameKey project, RevCommit commit)
      throws CommitValidationException {
    List<CommitValidationMessage> ret = Lists.newArrayList();
    ItsAssociationPolicy associationPolicy = itsConfig.getItsAssociationPolicy();

    switch (associationPolicy) {
      case MANDATORY:
      case SUGGESTED:
        String commitMessage = commit.getFullMessage();
        String[] issueIds = issueExtractor.getIssueIds(commitMessage);
        String synopsis = null;
        String details = null;
        if (issueIds.length > 0) {
          List<String> nonExistingIssueIds = Lists.newArrayList();
          client = itsFacadeFactory.getFacade(project);
          for (String issueId : issueIds) {
            ItsExistenceCheckResult existenceCheckResult;
            try {
              existenceCheckResult =
                  client.exists(issueId)
                      ? ItsExistenceCheckResult.EXISTS
                      : ItsExistenceCheckResult.DOESNT_EXIST;
            } catch (IOException e) {
              synopsis =
                  "Failed to check whether or not issue "
                      + issueId
                      + " exists, due to connectivity issue. Commit will be accepted.";
              logger.atWarning().withCause(e).log("%s", synopsis);
              details = e.toString();
              existenceCheckResult = ItsExistenceCheckResult.CONNECTIVITY_FAILURE;
              ret.add(commitValidationFailure(synopsis, details, existenceCheckResult));
            }
            if (existenceCheckResult == ItsExistenceCheckResult.DOESNT_EXIST) {
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
            sb.append(pluginName);
            sb.append(" Issue-Tracker");
            details = sb.toString();

            ret.add(
                commitValidationFailure(synopsis, details, ItsExistenceCheckResult.DOESNT_EXIST));
          }
        } else if (!itsConfig
            .getDummyIssuePattern()
            .map(p -> p.matcher(commitMessage).find())
            .orElse(false)) {
          synopsis = "Missing issue-id in commit message";

          StringBuilder sb = new StringBuilder();
          sb.append("Commit ");
          sb.append(commit.getId().getName());
          sb.append(" not associated to any issue\n");
          sb.append("\n");
          sb.append("Hint: insert one or more issue-id anywhere in the ");
          sb.append("commit message.\n");
          sb.append("      Issue-ids are strings matching ");
          sb.append(itsConfig.getIssuePattern().pattern());
          sb.append("\n");
          sb.append("      and are pointing to existing tickets on ");
          sb.append(pluginName);
          sb.append(" Issue-Tracker");
          details = sb.toString();

          ret.add(commitValidationFailure(synopsis, details, ItsExistenceCheckResult.DOESNT_EXIST));
        }
        break;
      case OPTIONAL:
      default:
        break;
    }
    return ret;
  }

  private CommitValidationMessage commitValidationFailure(
      String synopsis, String details, ItsExistenceCheckResult existenceCheck)
      throws CommitValidationException {
    CommitValidationMessage ret = new CommitValidationMessage(synopsis + "\n" + details, false);
    if (itsConfig.getItsAssociationPolicy() == ItsAssociationPolicy.MANDATORY
        && existenceCheck != ItsExistenceCheckResult.CONNECTIVITY_FAILURE) {
      throw new CommitValidationException(synopsis, Collections.singletonList(ret));
    }
    return ret;
  }

  @Override
  public List<CommitValidationMessage> onCommitReceived(CommitReceivedEvent receiveEvent)
      throws CommitValidationException {
    Project.NameKey projectName = receiveEvent.getProjectNameKey();
    ItsConfig.setCurrentProjectName(projectName);

    if (itsConfig.isEnabled(projectName, receiveEvent.getRefName())) {
      return validCommit(projectName, receiveEvent.commit);
    }

    return Collections.emptyList();
  }
}
