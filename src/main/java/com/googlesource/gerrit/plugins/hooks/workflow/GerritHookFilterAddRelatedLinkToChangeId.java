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

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.server.ReviewDb;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gwtorm.server.OrmException;
import com.google.gwtorm.server.ResultSet;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;
import com.googlesource.gerrit.plugins.hooks.util.IssueExtractor;

import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

public class GerritHookFilterAddRelatedLinkToChangeId extends
    GerritHookFilter  {

  Logger log = LoggerFactory
      .getLogger(GerritHookFilterAddRelatedLinkToChangeId.class);

  @Inject
  private ItsFacade its;

  @Inject
  @GerritServerConfig
  private Config gerritConfig;

  @Inject
  private IssueExtractor issueExtractor;

  @Inject
  private ReviewDb db;

  @Inject @PluginName
  private String pluginName;


  /**
   * Filter issues to those that occur for the first time in a change
   *
   * @param issues The issues to filter.
   * @param patchSet Filter for this patch set.
   * @return the issues that occur for the first time.
   * @throws IOException
   * @throws OrmException
   */
  private List<String> filterForFirstLinkedIssues(String[] issues,
      PatchSetCreatedEvent patchSet) throws IOException, OrmException {
    List<String> ret = Lists.newArrayList(issues);
    int patchSetNumberCurrent = Integer.parseInt(patchSet.patchSet.number);

    if (patchSetNumberCurrent > 1) {
      String project = patchSet.change.project;
      int changeNumber = Integer.parseInt(patchSet.change.number);
      Change.Id changeId = new Change.Id(changeNumber);

      // It would be nice to get patch sets directly via
      //   patchSetCreated.change.patchSets
      // but it turns out that it's null for our events. So we fetch the patch
      // sets from the db instead.
      ResultSet<PatchSet> patchSets = db.patchSets().byChange(changeId);
      Iterator<PatchSet> patchSetIter = patchSets.iterator();

      while (!ret.isEmpty() && patchSetIter.hasNext()) {
        PatchSet previousPatchSet = patchSetIter.next();
        if (previousPatchSet.getPatchSetId() < patchSetNumberCurrent) {
          String commitMessage = getComment(project,
              previousPatchSet.getRevision().get());
          for (String issue : issueExtractor.getIssueIds(commitMessage)) {
            ret.remove(issue);
          }
        }
      }
    }
    return ret;
  }

  @Override
  public void doFilter(PatchSetCreatedEvent patchsetCreated)
      throws IOException, OrmException {
    boolean addPatchSetComment = gerritConfig.getBoolean(pluginName, null,
        "commentOnPatchSetCreated", true);

    boolean addChangeComment = "1".equals(patchsetCreated.patchSet.number) &&
        gerritConfig.getBoolean(pluginName, null, "commentOnChangeCreated",
            false);

    boolean addFirstLinkedPatchSetComment = gerritConfig.getBoolean(pluginName,
        null, "commentOnFirstLinkedPatchSetCreated", false);

    if (addPatchSetComment || addFirstLinkedPatchSetComment || addChangeComment) {
      String gitComment =
          getComment(patchsetCreated.change.project,
              patchsetCreated.patchSet.revision);

      String[] issues = issueExtractor.getIssueIds(gitComment);

      List<String> firstLinkedIssues = null;
      if (addFirstLinkedPatchSetComment) {
        firstLinkedIssues = filterForFirstLinkedIssues(issues, patchsetCreated);
      }

      for (String issue : issues) {
        if (addChangeComment) {
          its.addRelatedLink(issue, new URL(patchsetCreated.change.url),
              "Gerrit Change " + patchsetCreated.change.id);
        }

        if (addPatchSetComment) {
          its.addRelatedLink(issue, new URL(patchsetCreated.change.url),
              "Gerrit Patch-Set " + patchsetCreated.change.id + "/"
                  + patchsetCreated.patchSet.number);
        }

        if (addFirstLinkedPatchSetComment && firstLinkedIssues.contains(issue)) {
          its.addRelatedLink(issue, new URL(patchsetCreated.change.url),
              "Gerrit Patch-Set " + patchsetCreated.change.id + "/"
                  + patchsetCreated.patchSet.number);
        }
      }
    }
  }
}
