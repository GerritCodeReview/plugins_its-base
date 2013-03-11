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
import java.net.URL;

import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.server.config.GerritServerConfig;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;

public class GerritHookFilterAddRelatedLinkToChangeId extends
    GerritHookFilter  {

  Logger log = LoggerFactory
      .getLogger(GerritHookFilterAddRelatedLinkToChangeId.class);

  @Inject
  private ItsFacade its;

  @Inject
  @GerritServerConfig
  private Config gerritConfig;

  @Override
  public void doFilter(PatchSetCreatedEvent patchsetCreated) throws IOException {
    if (!(gerritConfig.getBoolean(its.name(), null, "commentOnPatchSetCreated",
        true))) {
      return;
    }

    String gitComment =
        getComment(patchsetCreated.change.project,
            patchsetCreated.patchSet.revision);
    String[] issues = getIssueIds(gitComment);

    for (String issue : issues) {
      its.addRelatedLink(issue, new URL(patchsetCreated.change.url),
          "Gerrit Patch-Set: " + patchsetCreated.change.id + "/"
              + patchsetCreated.patchSet.number);
    }
  }
}
