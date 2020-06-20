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

package com.googlesource.gerrit.plugins.its.base.util;

import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.PatchSet;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.data.ApprovalAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.PatchSetEvent;
import com.google.gerrit.server.events.PrivateStateChangedEvent;
import com.google.gerrit.server.events.RefEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.gerrit.server.events.WorkInProgressStateChangedEvent;
import com.google.inject.Inject;
import com.googlesource.gerrit.plugins.its.base.workflow.RefEventProperties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.jgit.lib.ObjectId;

/** Extractor to translate an {@link ChangeEvent} to a map of properties}. */
public class PropertyExtractor {
  private final ItsProjectExtractor itsProjectExtractor;
  private final IssueExtractor issueExtractor;
  private final PropertyAttributeExtractor propertyAttributeExtractor;
  private final String pluginName;

  @Inject
  PropertyExtractor(
      IssueExtractor issueExtractor,
      ItsProjectExtractor itsProjectExtractor,
      PropertyAttributeExtractor propertyAttributeExtractor,
      @PluginName String pluginName) {
    this.issueExtractor = issueExtractor;
    this.itsProjectExtractor = itsProjectExtractor;
    this.propertyAttributeExtractor = propertyAttributeExtractor;
    this.pluginName = pluginName;
  }

  /**
   * creates a patch id for change id string and patchset id string.
   *
   * @param changeId String representation of the patch sets {@code Change.Id@}
   * @param patchId String representation of the patch sets {@code Patchset.Id@}
   * @return PatchSet.Id for the specified patch set. If the String to int conversion fails for any
   *     of the parameters, null is returned.
   */
  private PatchSet.Id newPatchSetId(String changeId, String patchId) {
    try {
      return PatchSet.id(Change.id(Integer.parseInt(changeId)), Integer.parseInt(patchId));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Map<String, Set<String>> extractMapFrom(PatchSetEvent event, Map<String, String> common) {
    ChangeAttribute change = event.change.get();
    PatchSetAttribute patchSet = event.patchSet.get();
    common.putAll(propertyAttributeExtractor.extractFrom(change));
    common.putAll(propertyAttributeExtractor.extractFrom(patchSet));
    PatchSet.Id patchSetId =
        newPatchSetId(Integer.toString(change.number), Integer.toString(patchSet.number));
    return issueExtractor.getIssueIds(change.project, patchSet.revision, patchSetId);
  }

  private Map<String, Set<String>> extractFrom(
      ChangeAbandonedEvent event, Map<String, String> common) {
    common.putAll(propertyAttributeExtractor.extractFrom(event.abandoner.get(), "abandoner"));
    common.put("reason", event.reason);
    return extractMapFrom(event, common);
  }

  private Map<String, Set<String>> extractFrom(
      ChangeMergedEvent event, Map<String, String> common) {
    common.putAll(propertyAttributeExtractor.extractFrom(event.submitter.get(), "submitter"));
    return extractMapFrom(event, common);
  }

  private Map<String, Set<String>> extractFrom(
      ChangeRestoredEvent event, Map<String, String> common) {
    common.putAll(propertyAttributeExtractor.extractFrom(event.restorer.get(), "restorer"));
    common.put("reason", event.reason);
    return extractMapFrom(event, common);
  }

  private Map<String, Set<String>> extractFrom(RefUpdatedEvent event, Map<String, String> common) {
    if (event.submitter != null) {
      common.putAll(propertyAttributeExtractor.extractFrom(event.submitter.get(), "submitter"));
    }
    common.putAll(propertyAttributeExtractor.extractFrom(event.refUpdate.get()));
    RefUpdateAttribute refUpdateEvent = event.refUpdate.get();
    String commitId =
        (refUpdateEvent.newRev.equals(ObjectId.zeroId().name())
            ? refUpdateEvent.oldRev
            : refUpdateEvent.newRev);
    return issueExtractor.getIssueIds(event.getProjectNameKey().get(), commitId);
  }

  private Map<String, Set<String>> extractFrom(
      PatchSetCreatedEvent event, Map<String, String> common) {
    common.putAll(propertyAttributeExtractor.extractFrom(event.uploader.get(), "uploader"));
    return extractMapFrom(event, common);
  }

  private Map<String, Set<String>> extractFrom(
      CommentAddedEvent event, Map<String, String> common) {
    common.putAll(propertyAttributeExtractor.extractFrom(event.author.get(), "commenter"));
    common.put("comment", event.comment);
    ApprovalAttribute[] approvals = event.approvals.get();
    if (approvals != null) {
      for (ApprovalAttribute approvalAttribute : approvals) {
        common.putAll(propertyAttributeExtractor.extractFrom(approvalAttribute));
      }
    }
    return extractMapFrom(event, common);
  }

  private Map<String, Set<String>> extractFrom(
      WorkInProgressStateChangedEvent event, Map<String, String> common) {
    common.putAll(propertyAttributeExtractor.extractFrom(event.changer.get(), "changer"));
    return extractFrom((ChangeEvent) event, common);
  }

  private Map<String, Set<String>> extractFrom(
      PrivateStateChangedEvent event, Map<String, String> common) {
    common.putAll(propertyAttributeExtractor.extractFrom(event.changer.get(), "changer"));
    return extractFrom((ChangeEvent) event, common);
  }

  private Map<String, Set<String>> extractFrom(ChangeEvent event, Map<String, String> common) {
    common.put("event-type", event.type);
    ChangeAttribute change = event.change.get();
    common.putAll(propertyAttributeExtractor.extractFrom(change));
    common.put("refName", event.refName);

    // Got no patch set information, extract from commit message.
    return issueExtractor.getIssueIdsFromCommitMessage(change.commitMessage);
  }

  /**
   * A set of properties extracted from an event.
   *
   * <p>As events may relate to more that a single issue and a group of properties should be tied to
   * a single issue, we need to return {@code Set<Map>} of properties. As properties we understand a
   * map of event attributes. Using this approach, a PatchSetCreatedEvent for a patch set with
   * commit message:
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
   * Thereby, sites can choose to cause different actions for different issues associated to the
   * same event. So in the above example, a comment "mentioned in change 123" may be added for issue
   * 42, and a comment "fixed by change 123” may be added for issue 4711.
   *
   * @param event The event to extract property maps from.
   * @return set of property maps extracted from the event.
   */
  public RefEventProperties extractFrom(RefEvent event) {
    Map<String, Set<String>> associations = null;
    Map<String, String> common = new HashMap<>();
    common.put("event", event.getClass().getName());
    String project = event.getProjectNameKey().get();
    common.put("event-type", event.type);
    common.put("project", project);

    itsProjectExtractor
        .getItsProject(project)
        .ifPresent(itsProject -> common.put("its-project", itsProject));
    common.put("ref", event.getRefName());
    common.put("itsName", pluginName);

    if (event instanceof ChangeAbandonedEvent) {
      associations = extractFrom((ChangeAbandonedEvent) event, common);
    } else if (event instanceof ChangeMergedEvent) {
      associations = extractFrom((ChangeMergedEvent) event, common);
    } else if (event instanceof ChangeRestoredEvent) {
      associations = extractFrom((ChangeRestoredEvent) event, common);
    } else if (event instanceof CommentAddedEvent) {
      associations = extractFrom((CommentAddedEvent) event, common);
    } else if (event instanceof PatchSetCreatedEvent) {
      associations = extractFrom((PatchSetCreatedEvent) event, common);
    } else if (event instanceof RefUpdatedEvent) {
      associations = extractFrom((RefUpdatedEvent) event, common);
    } else if (event instanceof PrivateStateChangedEvent) {
      associations = extractFrom((PrivateStateChangedEvent) event, common);
    } else if (event instanceof WorkInProgressStateChangedEvent) {
      associations = extractFrom((WorkInProgressStateChangedEvent) event, common);
    }

    Set<Map<String, String>> ret = new HashSet<>();
    if (associations != null) {
      for (Entry<String, Set<String>> assoc : associations.entrySet()) {
        Map<String, String> properties = new HashMap<>();
        properties.put("issue", assoc.getKey());
        properties.put("association", String.join(" ", assoc.getValue()));
        properties.putAll(common);
        ret.add(properties);
      }
    }
    return new RefEventProperties(common, ret);
  }
}
