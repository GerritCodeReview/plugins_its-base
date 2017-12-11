// Copyright (C) 2017 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.its.base.workflow;

import com.google.common.base.Strings;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import java.io.IOException;
import java.util.Map;

/**
 * Adds a short predefined comments to an issue.
 *
 * <p>Comments are added for merging, abandoning, restoring of changes and adding of patch sets.
 */
public class AddStandardComment implements Action {
  public interface Factory {
    AddStandardComment create();
  }

  private ItsFacade its;

  private String getCommentChangeEvent(String action, String prefix, Map<String, String> map) {
    String ret = "";
    String changeNumber = getValueFromMap(map, "", "change-number", "changeNumber");
    if (!changeNumber.isEmpty()) {
      changeNumber += " ";
    }
    ret += "Change " + changeNumber + action;
    String submitter = getValueFromMap(map, prefix, "-name", "Name", "-username", "Username");
    if (!submitter.isEmpty()) {
      ret += " by " + submitter;
    }
    String subject = Strings.nullToEmpty(map.get("subject"));
    if (!subject.isEmpty()) {
      ret += ":\n" + subject;
    }
    String reason = Strings.nullToEmpty(map.get("reason"));
    if (!reason.isEmpty()) {
      ret += "\n\nReason:\n" + reason;
    }
    String url = getValueFromMap(map, "", "change-url", "changeUrl");
    if (!url.isEmpty()) {
      ret += "\n\n" + its.createLinkForWebui(url, url);
    }
    return ret;
  }

  private String getValueFromMap(Map<String, String> map, String keyPrefix, String... keyOptions) {
    for (String key : keyOptions) {
      String ret = Strings.nullToEmpty(map.get(keyPrefix + key));
      if (!ret.isEmpty()) {
        return ret;
      }
    }
    return "";
  }

  @Override
  public void execute(
      ItsFacade its, String issue, ActionRequest actionRequest, Map<String, String> properties)
      throws IOException {
    this.its = its;
    String comment = buildComment(properties);

    if (!Strings.isNullOrEmpty(comment)) {
      its.addComment(issue, comment);
    }
  }

  private String buildComment(Map<String, String> properties) {
    switch (properties.get("event-type")) {
      case "change-abandoned":
        return getCommentChangeEvent("abandoned", "abandoner", properties);
      case "change-merged":
        return getCommentChangeEvent("merged", "submitter", properties);
      case "change-restored":
        return getCommentChangeEvent("restored", "restorer", properties);
      case "patchset-created":
        return getCommentChangeEvent("had a related patch set uploaded", "uploader", properties);
      default:
        return "";
    }
  }
}
