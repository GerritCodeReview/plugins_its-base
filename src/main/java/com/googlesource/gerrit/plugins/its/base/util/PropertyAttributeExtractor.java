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

import com.google.common.collect.Sets;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ApprovalAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;
import java.util.Set;
import org.apache.commons.lang.StringEscapeUtils;

/** Extractor to translate the various {@code *Attribute}s to {@link Property Properties}. */
class PropertyAttributeExtractor {
  private Property.Factory propertyFactory;
  private ItsFacade its;

  @Inject
  PropertyAttributeExtractor(ItsFacade its, Property.Factory propertyFactory) {
    this.its = its;
    this.propertyFactory = propertyFactory;
  }

  Set<Property> extractFrom(AccountAttribute accountAttribute, String prefix) {
    Set<Property> properties = Sets.newHashSet();
    if (accountAttribute != null) {
      // deprecated, to be removed soon. migrate to ones without dash.
      properties.add(propertyFactory.create(prefix + "-email", accountAttribute.email));
      properties.add(propertyFactory.create(prefix + "-username", accountAttribute.username));
      properties.add(propertyFactory.create(prefix + "-name", accountAttribute.name));

      // New style configs for vm and soy
      properties.add(propertyFactory.create(prefix + "Email", accountAttribute.email));
      properties.add(propertyFactory.create(prefix + "Username", accountAttribute.username));
      properties.add(propertyFactory.create(prefix + "Name", accountAttribute.name));
    }
    return properties;
  }

  Set<Property> extractFrom(ChangeAttribute changeAttribute) {
    Set<Property> properties = Sets.newHashSet();
    properties.add(propertyFactory.create("branch", changeAttribute.branch));
    properties.add(propertyFactory.create("topic", changeAttribute.topic));
    properties.add(propertyFactory.create("subject", changeAttribute.subject));
    properties.add(
        propertyFactory.create(
            "escapedSubject", StringEscapeUtils.escapeJava(changeAttribute.subject)));

    // deprecated, to be removed soon. migrate to ones without dash.
    properties.add(propertyFactory.create("commit-message", changeAttribute.commitMessage));
    properties.add(propertyFactory.create("change-id", changeAttribute.id));
    properties.add(propertyFactory.create("change-number", String.valueOf(changeAttribute.number)));
    properties.add(propertyFactory.create("change-url", changeAttribute.url));

    // New style configs for vm and soy
    properties.add(propertyFactory.create("commitMessage", changeAttribute.commitMessage));
    properties.add(propertyFactory.create("changeId", changeAttribute.id));
    properties.add(propertyFactory.create("changeNumber", String.valueOf(changeAttribute.number)));
    properties.add(propertyFactory.create("changeUrl", changeAttribute.url));

    // Soy specfic config though will work with Velocity too
    properties.add(
        propertyFactory.create(
            "formatChangeUrl", its.createLinkForWebui(changeAttribute.url, changeAttribute.url)));

    String status = null;
    if (changeAttribute.status != null) {
      status = changeAttribute.status.toString();
    }
    properties.add(propertyFactory.create("status", status));
    properties.addAll(extractFrom(changeAttribute.owner, "owner"));
    return properties;
  }

  Set<Property> extractFrom(PatchSetAttribute patchSetAttribute) {
    Set<Property> properties = Sets.newHashSet();
    properties.add(propertyFactory.create("revision", patchSetAttribute.revision));
    // deprecated, to be removed soon. migrate to ones without dash.
    properties.add(
        propertyFactory.create("patch-set-number", String.valueOf(patchSetAttribute.number)));

    // New style configs for vm and soy
    properties.add(
        propertyFactory.create("patchSetNumber", String.valueOf(patchSetAttribute.number)));

    properties.add(propertyFactory.create("ref", patchSetAttribute.ref));

    // deprecated, to be removed soon. migrate to ones without dash.
    properties.add(propertyFactory.create("created-on", patchSetAttribute.createdOn.toString()));

    // New style configs for vm and soy
    properties.add(propertyFactory.create("createdOn", patchSetAttribute.createdOn.toString()));

    properties.add(propertyFactory.create("parents", patchSetAttribute.parents.toString()));
    properties.add(
        propertyFactory.create("deletions", Integer.toString(patchSetAttribute.sizeDeletions)));
    properties.add(
        propertyFactory.create("insertions", Integer.toString(patchSetAttribute.sizeInsertions)));
    properties.addAll(extractFrom(patchSetAttribute.uploader, "uploader"));
    properties.addAll(extractFrom(patchSetAttribute.author, "author"));
    return properties;
  }

  Set<Property> extractFrom(RefUpdateAttribute refUpdateAttribute) {
    Set<Property> properties = Sets.newHashSet();
    properties.add(propertyFactory.create("revision", refUpdateAttribute.newRev));

    // deprecated, to be removed soon. migrate to ones without dash.
    properties.add(propertyFactory.create("revision-old", refUpdateAttribute.oldRev));

    // New style configs for vm and soy
    properties.add(propertyFactory.create("revisionOld", refUpdateAttribute.oldRev));
    properties.add(propertyFactory.create("ref", refUpdateAttribute.refName));
    return properties;
  }

  Set<Property> extractFrom(ApprovalAttribute approvalAttribute) {
    Set<Property> properties = Sets.newHashSet();
    // deprecated, to be removed soon. migrate to ones without dash.
    properties.add(
        propertyFactory.create("approval-" + approvalAttribute.type, approvalAttribute.value));

    // New style configs for vm and soy
    properties.add(
        propertyFactory.create(
            "approval" + approvalAttribute.type.replace("-", ""), approvalAttribute.value));
    return properties;
  }
}
