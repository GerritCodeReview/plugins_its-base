//Copyright (C) 2013 The Android Open Source Project
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

package com.googlesource.gerrit.plugins.hooks.util;

import java.util.Set;

import com.google.common.collect.Sets;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ApprovalAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.hooks.workflow.Property;

/**
 * Extractor to translate the various {@code *Attribute}s to
 * {@link Property Properties}.
 */
public class PropertyAttributeExtractor {
  private Property.Factory propertyFactory;

  @Inject
  PropertyAttributeExtractor(Property.Factory propertyFactory) {
    this.propertyFactory = propertyFactory;
  }

  public Set<Property> extractFrom(AccountAttribute accountAttribute,
      String prefix) {
    Set<Property> properties = Sets.newHashSet();
    if (accountAttribute != null) {
      properties.add(propertyFactory.create(prefix + "-email",
          accountAttribute.email));
      properties.add(propertyFactory.create(prefix + "-username",
          accountAttribute.username));
      properties.add(propertyFactory.create(prefix + "-name",
          accountAttribute.name));
    }
    return properties;
  }

  public Set<Property> extractFrom(ChangeAttribute changeAttribute) {
    Set<Property> properties = Sets.newHashSet();
    properties.add(propertyFactory.create("project", changeAttribute.project));
    properties.add(propertyFactory.create("branch", changeAttribute.branch));
    properties.add(propertyFactory.create("topic", changeAttribute.topic));
    properties.add(propertyFactory.create("subject", changeAttribute.subject));
    properties.add(propertyFactory.create("change-id", changeAttribute.id));
    properties.add(propertyFactory.create("change-number", changeAttribute.number));
    properties.add(propertyFactory.create("change-url", changeAttribute.url));
    String status = null;
    if (changeAttribute.status != null) {
      status = changeAttribute.status.toString();
    }
    properties.add(propertyFactory.create("status", status));
    properties.addAll(extractFrom(changeAttribute.owner, "owner"));
    return properties;
  }

  public Set<Property>extractFrom(PatchSetAttribute patchSetAttribute) {
    Set<Property> properties = Sets.newHashSet();
    properties.add(propertyFactory.create("revision",
        patchSetAttribute.revision));
    properties.add(propertyFactory.create("patch-set-number",
        patchSetAttribute.number));
    properties.add(propertyFactory.create("ref", patchSetAttribute.ref));
    properties.add(propertyFactory.create("created-on",
        patchSetAttribute.createdOn.toString()));
    properties.add(propertyFactory.create("parents",
        patchSetAttribute.parents.toString()));
    properties.add(propertyFactory.create("deletions",
        Integer.toString(patchSetAttribute.sizeDeletions)));
    properties.add(propertyFactory.create("insertions",
        Integer.toString(patchSetAttribute.sizeInsertions)));
    properties.addAll(extractFrom(patchSetAttribute.uploader,
        "uploader"));
    properties.addAll(extractFrom(patchSetAttribute.author,
        "author"));
    return properties;
  }

  public Set<Property>extractFrom(RefUpdateAttribute refUpdateAttribute) {
    Set<Property> properties = Sets.newHashSet();
    properties.add(propertyFactory.create("revision",
        refUpdateAttribute.newRev));
    properties.add(propertyFactory.create("revision-old",
        refUpdateAttribute.oldRev));
    properties.add(propertyFactory.create("project",
        refUpdateAttribute.project));
    properties.add(propertyFactory.create("ref",
        refUpdateAttribute.refName));
    return properties;
  }

  public Set<Property>extractFrom(ApprovalAttribute approvalAttribute) {
    Set<Property> properties = Sets.newHashSet();
    properties.add(propertyFactory.create("approval-" +
        approvalAttribute.type, approvalAttribute.value));
    return properties;
  }
}
