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

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.server.data.ApprovalAttribute;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.DraftPublishedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.inject.Inject;

import com.googlesource.gerrit.plugins.hooks.workflow.Property;

/**
 * Extractor to translate an {@link ChangeEvent} to
 * {@link Property Properties}.
 */
public class PropertyExtractor {
  private IssueExtractor issueExtractor;
  private Property.Factory propertyFactory;
  private PropertyAttributeExtractor propertyAttributeExtractor;

  @Inject
  PropertyExtractor(IssueExtractor issueExtractor,
      Property.Factory propertyFactory,
      PropertyAttributeExtractor propertyAttributeExtractor) {
    this.issueExtractor = issueExtractor;
    this.propertyFactory = propertyFactory;
    this.propertyAttributeExtractor = propertyAttributeExtractor;
  }

  /**
   * creates a patch id for change id string and patchset id string.
   * @param changeId String representation of the patch sets {@code Change.Id@}
   * @param patchId String representation of the patch sets {@code Patchset.Id@}
   * @return PatchSet.Id for the specified patch set. If the String to int
   *    conversion fails for any of the parameters, null is returned.
   */
  private PatchSet.Id newPatchSetId(String changeId, String patchId) {
    try {
        return new PatchSet.Id(new Change.Id(Integer.parseInt(changeId)),
            Integer.parseInt(patchId));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Map<String,Set<String>> extractFrom(ChangeAbandonedEvent event,
      Set<Property> common) {
    common.add(propertyFactory.create("event-type", event.type));
    common.addAll(propertyAttributeExtractor.extractFrom(event.change));
    common.addAll(propertyAttributeExtractor.extractFrom(event.abandoner, "abandoner"));
    common.addAll(propertyAttributeExtractor.extractFrom(event.patchSet));
    common.add(propertyFactory.create("reason", event.reason));
    PatchSet.Id patchSetId = newPatchSetId(event.change.number,
        event.patchSet.number);
    return issueExtractor.getIssueIds(event.change.project,
        event.patchSet.revision, patchSetId);
  }

  private Map<String,Set<String>> extractFrom(ChangeMergedEvent event,
      Set<Property> common) {
    common.add(propertyFactory.create("event-type", event.type));
    common.addAll(propertyAttributeExtractor.extractFrom(event.change));
    common.addAll(propertyAttributeExtractor.extractFrom(event.submitter, "submitter"));
    common.addAll(propertyAttributeExtractor.extractFrom(event.patchSet));
    PatchSet.Id patchSetId = newPatchSetId(event.change.number,
        event.patchSet.number);
    return issueExtractor.getIssueIds(event.change.project,
        event.patchSet.revision, patchSetId);
  }

  private Map<String,Set<String>> extractFrom(ChangeRestoredEvent event,
      Set<Property> common) {
    common.add(propertyFactory.create("event-type", event.type));
    common.addAll(propertyAttributeExtractor.extractFrom(event.change));
    common.addAll(propertyAttributeExtractor.extractFrom(event.restorer, "restorer"));
    common.addAll(propertyAttributeExtractor.extractFrom(event.patchSet));
    common.add(propertyFactory.create("reason", event.reason));
    PatchSet.Id patchSetId = newPatchSetId(event.change.number,
        event.patchSet.number);
    return issueExtractor.getIssueIds(event.change.project,
        event.patchSet.revision, patchSetId);
  }

  private Map<String,Set<String>> extractFrom(DraftPublishedEvent event,
      Set<Property> common) {
    common.add(propertyFactory.create("event-type", event.type));
    common.addAll(propertyAttributeExtractor.extractFrom(event.change));
    common.addAll(propertyAttributeExtractor.extractFrom(event.patchSet));
    common.addAll(propertyAttributeExtractor.extractFrom(event.uploader, "uploader"));
    PatchSet.Id patchSetId = newPatchSetId(event.change.number,
        event.patchSet.number);
    return issueExtractor.getIssueIds(event.change.project,
        event.patchSet.revision, patchSetId);
  }

  private Map<String,Set<String>> extractFrom(RefUpdatedEvent event,
      Set<Property> common) {
    common.add(propertyFactory.create("event-type", event.type));
    common.addAll(propertyAttributeExtractor.extractFrom(event.submitter, "submitter"));
    common.addAll(propertyAttributeExtractor.extractFrom(event.refUpdate));
    return issueExtractor.getIssueIds(event.refUpdate.project,
        event.refUpdate.newRev);
  }

  private Map<String,Set<String>> extractFrom(PatchSetCreatedEvent event,
      Set<Property> common) {
    common.add(propertyFactory.create("event-type", event.type));
    common.addAll(propertyAttributeExtractor.extractFrom(event.change));
    common.addAll(propertyAttributeExtractor.extractFrom(event.patchSet));
    common.addAll(propertyAttributeExtractor.extractFrom(event.uploader, "uploader"));
    PatchSet.Id patchSetId = newPatchSetId(event.change.number,
        event.patchSet.number);
    return issueExtractor.getIssueIds(event.change.project,
        event.patchSet.revision, patchSetId);
  }

  private Map<String,Set<String>> extractFrom(CommentAddedEvent event,
      Set<Property> common) {
    common.add(propertyFactory.create("event-type", event.type));
    common.addAll(propertyAttributeExtractor.extractFrom(event.change));
    common.addAll(propertyAttributeExtractor.extractFrom(event.patchSet));
    common.addAll(propertyAttributeExtractor.extractFrom(event.author, "commenter"));
    if (event.approvals != null) {
      for (ApprovalAttribute approvalAttribute : event.approvals) {
        common.addAll(propertyAttributeExtractor.extractFrom(
            approvalAttribute));
      }
    }
    common.add(propertyFactory.create("comment", event.comment));
    PatchSet.Id patchSetId = newPatchSetId(event.change.number,
        event.patchSet.number);
    return issueExtractor.getIssueIds(event.change.project,
        event.patchSet.revision, patchSetId);
  }

  /**
   * A set of property sets extracted from an event.
   *
   * As events may relate to more that a single issue, and properties sets are
   * should be tied to a single issue, returning {@code Set<Property>} is not
   * sufficient, and we need to return {@code Set<Set<Property>>}. Using this
   * approach, a PatchSetCreatedEvent for a patch set with commit message:
   *
   * <pre>
   *   (bug 4711) Fix treatment of special characters in title
   *
   *   This commit mitigates the effects of bug 42, but does not fix them.
   *
   *   Change-Id: I1234567891123456789212345678931234567894
   * </pre>
   *
   * may return both
   *
   * <pre>
   *   issue: 4711
   *   association: subject
   *   event: patchset-created
   * </pre>
   *
   * and
   *
   * <pre>
   *   issue: 42
   *   association: body
   *   event: patchset-created
   * </pre>
   *
   * Thereby, sites can choose to cause different actions for different issues
   * associated to the same event. So in the above example, a comment
   * "mentioned in change 123" may be added for issue 42, and a comment
   * "fixed by change 123” may be added for issue 4711.
   *
   * @param event The event to extract property sets from.
   * @return sets of property sets extracted from the event.
   */
  public Set<Set<Property>> extractFrom(ChangeEvent event) {
    Map<String,Set<String>> associations = null;
    Set<Set<Property>> ret = Sets.newHashSet();

    Set<Property> common = Sets.newHashSet();
    common.add(propertyFactory.create("event", event.getClass().getName()));

    if (event instanceof ChangeAbandonedEvent) {
      associations = extractFrom((ChangeAbandonedEvent) event, common);
    } else if (event instanceof ChangeMergedEvent) {
      associations = extractFrom((ChangeMergedEvent) event, common);
    } else if (event instanceof ChangeRestoredEvent) {
      associations = extractFrom((ChangeRestoredEvent) event, common);
    } else if (event instanceof CommentAddedEvent) {
      associations = extractFrom((CommentAddedEvent) event, common);
    } else if (event instanceof DraftPublishedEvent) {
      associations = extractFrom((DraftPublishedEvent) event, common);
    } else if (event instanceof PatchSetCreatedEvent) {
      associations = extractFrom((PatchSetCreatedEvent) event, common);
    } else if (event instanceof RefUpdatedEvent) {
      associations = extractFrom((RefUpdatedEvent) event, common);
    }

    if (associations != null) {
      for (String issue : associations.keySet()) {
        Set<Property> properties = Sets.newHashSet();
        Property property = propertyFactory.create("issue", issue);
        properties.add(property);
        for (String occurrence: associations.get(issue)) {
          property = propertyFactory.create("association", occurrence);
          properties.add(property);
        }
        properties.addAll(common);
        ret.add(properties);
      }
    }
    return ret;
  }
}
