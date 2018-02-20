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

package com.googlesource.gerrit.plugins.its.base.util;

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ApprovalAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import java.util.HashMap;
import java.util.Map;

/** Extractor to translate the various {@code *Attribute}s to properties. */
class PropertyAttributeExtractor {
  private ItsFacade its;

  @Inject
  PropertyAttributeExtractor(ItsFacade its) {
    this.its = its;
  }

  Map<String, String> extractFrom(AccountAttribute accountAttribute, String prefix) {
    Map<String, String> properties = new HashMap<>();
    if (accountAttribute != null) {
      properties.put(prefix + "Email", accountAttribute.email);
      properties.put(prefix + "Username", accountAttribute.username);
      properties.put(prefix + "Name", accountAttribute.name);
    }
    return properties;
  }

  Map<String, String> extractFrom(ChangeAttribute changeAttribute) {
    return ImmutableMap.<String, String>builder()
        .put("branch", changeAttribute.branch)
        .put("topic", changeAttribute.topic != null ? changeAttribute.topic : "")
        .put("subject", changeAttribute.subject)
        .put("commitMessage", changeAttribute.commitMessage)
        .put("changeId", changeAttribute.id)
        .put("changeNumber", String.valueOf(changeAttribute.number))
        .put("changeUrl", changeAttribute.url)
        .put("formatChangeUrl", its.createLinkForWebui(changeAttribute.url, changeAttribute.url))
        .put("status", changeAttribute.status != null ? changeAttribute.status.toString() : "")
        .putAll(extractFrom(changeAttribute.owner, "owner"))
        .build();
  }

  Map<String, String> extractFrom(PatchSetAttribute patchSetAttribute) {
    return ImmutableMap.<String, String>builder()
        .put("revision", patchSetAttribute.revision)
        .put("patchSetNumber", String.valueOf(patchSetAttribute.number))
        .put("ref", patchSetAttribute.ref)
        .put("createdOn", patchSetAttribute.createdOn.toString())
        .put("parents", patchSetAttribute.parents.toString())
        .put("deletions", Integer.toString(patchSetAttribute.sizeDeletions))
        .put("insertions", Integer.toString(patchSetAttribute.sizeInsertions))
        .putAll(extractFrom(patchSetAttribute.uploader, "uploader"))
        .putAll(extractFrom(patchSetAttribute.author, "author"))
        .build();
  }

  Map<String, String> extractFrom(RefUpdateAttribute refUpdateAttribute) {
    return ImmutableMap.<String, String>builder()
        .put("revision", refUpdateAttribute.newRev)
        .put("revisionOld", refUpdateAttribute.oldRev)
        .put("ref", refUpdateAttribute.refName)
        .build();
  }

  public Map<String, String> extractFrom(ApprovalAttribute approvalAttribute) {
    return ImmutableMap.<String, String>builder()
        .put("approval" + approvalAttribute.type.replace("-", ""), approvalAttribute.value)
        .build();
  }
}
