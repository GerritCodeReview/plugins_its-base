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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.common.ChangeListener;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gwtorm.server.OrmException;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.hooks.its.ItsConfig;
import com.googlesource.gerrit.plugins.hooks.util.CommitMessageFetcher;

public class GerritHookFilter implements ChangeListener {
  private static final Logger log = LoggerFactory.getLogger(GerritHookFilter.class);

  @Inject
  private CommitMessageFetcher commitMessageFetcher;

  @Inject
  private ItsConfig itsConfig;

  public String getComment(String projectName, String commitId)
      throws IOException {
    return commitMessageFetcher.fetch(projectName, commitId);
  }

  public void doFilter(PatchSetCreatedEvent hook) throws IOException,
      OrmException {
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
    if (!itsConfig.isEnabled(event)) {
      return;
    }

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
        log.debug("Event " + event + " not recognised and ignored");
      }
    } catch (Throwable e) {
      log.error("Event " + event + " processing failed", e);
    }
  }

  public String getUrl(PatchSetCreatedEvent hook) {
    return null;
  }
}
