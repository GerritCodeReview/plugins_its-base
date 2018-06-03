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

package com.googlesource.gerrit.plugins.its.base.workflow.action;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Adds a short predefined comments to an issue.
 *
 * <p>Comments are added for merging, abandoning, restoring of changes and adding of patch sets.
 */
public class AddStandardComment implements Action {
  public interface Factory {
    AddStandardComment create();
  }

  private final ItsFacade its;

  @Inject
  public AddStandardComment(ItsFacade its) {
    this.its = its;
  }

  private String getCommentChangeEvent(String action, String prefix, Map<String, String> map) {
    String ret = "";
    String changeNumber = Strings.nullToEmpty(map.get("changeNumber"));
    if (!changeNumber.isEmpty()) {
      changeNumber += " ";
    }
    ret += "Change " + changeNumber + action;
    String submitter = getValueFromMap(map, prefix, "Name", "Username");
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
    String url = Strings.nullToEmpty(map.get("formatChangeUrl"));
    if (!url.isEmpty()) {
      ret += "\n\n" + url;
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
  public void execute(String issue, ActionRequest actionRequest, Set<Property> properties)
      throws IOException {
    String comment = buildComment(properties);
    if (!Strings.isNullOrEmpty(comment)) {
      its.addComment(issue, comment);
    }
  }

  private String buildComment(Set<Property> properties) {
    Map<String, String> map = Maps.newHashMap();
    for (Property property : properties) {
      String current = property.getValue();
      if (!Strings.isNullOrEmpty(current)) {
        String key = property.getKey();
        String old = Strings.nullToEmpty(map.get(key));
        if (!old.isEmpty()) {
          old += ", ";
        }
        map.put(key, old + current);
      }
    }
    switch (map.get("event-type")) {
      case "change-abandoned":
        return getCommentChangeEvent("abandoned", "abandoner", map);
      case "change-merged":
        return getCommentChangeEvent("merged", "submitter", map);
      case "change-restored":
        return getCommentChangeEvent("restored", "restorer", map);
      case "patchset-created":
        return getCommentChangeEvent("had a related patch set uploaded", "uploader", map);
      default:
        return "";
    }
  }
}