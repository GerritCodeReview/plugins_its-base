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

import static org.easymock.EasyMock.expect;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ApprovalAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.RefEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PropertyExtractorTest extends LoggingMockingTestCase {
  private Injector injector;

  private ItsProjectExtractor itsProjectExtractor;
  private IssueExtractor issueExtractor;
  private PropertyAttributeExtractor propertyAttributeExtractor;

  public void testDummyChangeEvent() {
    PropertyExtractor propertyExtractor = injector.getInstance(PropertyExtractor.class);

    expect(itsProjectExtractor.getItsProject("testProject")).andReturn(Optional.empty());

    replayMocks();

    Set<Map<String, String>> actual =
        propertyExtractor.extractFrom(new DummyEvent()).getIssuesProperties();

    Set<Map<String, String>> expected = new HashSet<>();
    assertEquals("Properties do not match", expected, actual);
  }

  public void testChangeAbandonedEvent() {
    ChangeAbandonedEvent event = new ChangeAbandonedEvent(testChange("testProject", "testBranch"));

    expect(itsProjectExtractor.getItsProject("testProject")).andReturn(Optional.empty());

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Map<String, String> changeProperties =
        ImmutableMap.of("project", "testProject", "changeNumber", "176");
    expect(propertyAttributeExtractor.extractFrom(changeAttribute)).andReturn(changeProperties);

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.abandoner = Suppliers.ofInstance(accountAttribute);
    Map<String, String> accountProperties = ImmutableMap.of("abandonerName", "testName");
    expect(propertyAttributeExtractor.extractFrom(accountAttribute, "abandoner"))
        .andReturn(accountProperties);

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Map<String, String> patchSetProperties =
        ImmutableMap.of("revision", "testRevision", "patchSetNumber", "3");
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute)).andReturn(patchSetProperties);

    event.reason = "testReason";
    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Map<String, String> common =
        ImmutableMap.<String, String>builder()
            .putAll(changeProperties)
            .putAll(accountProperties)
            .putAll(patchSetProperties)
            .put("reason", "testReason")
            .put("ref", "refs/heads/testBranch")
            .build();

    eventHelper(event, "ChangeAbandonedEvent", "change-abandoned", common, true);
  }

  public void testChangeMergedEvent() {
    ChangeMergedEvent event = new ChangeMergedEvent(testChange("testProject", "testBranch"));

    expect(itsProjectExtractor.getItsProject("testProject")).andReturn(Optional.empty());

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Map<String, String> changeProperties =
        ImmutableMap.of("project", "testProject", "changeNumber", "176");
    expect(propertyAttributeExtractor.extractFrom(changeAttribute)).andReturn(changeProperties);

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.submitter = Suppliers.ofInstance(accountAttribute);
    Map<String, String> accountProperties = ImmutableMap.of("submitterName", "testName");
    expect(propertyAttributeExtractor.extractFrom(accountAttribute, "submitter"))
        .andReturn(accountProperties);

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Map<String, String> patchSetProperties =
        ImmutableMap.of("revision", "testRevision", "patchSetNumber", "3");
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute)).andReturn(patchSetProperties);

    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Map<String, String> common =
        ImmutableMap.<String, String>builder()
            .putAll(changeProperties)
            .putAll(accountProperties)
            .putAll(patchSetProperties)
            .put("ref", "refs/heads/testBranch")
            .build();

    eventHelper(event, "ChangeMergedEvent", "change-merged", common, true);
  }

  public void testChangeRestoredEvent() {
    ChangeRestoredEvent event = new ChangeRestoredEvent(testChange("testProject", "testBranch"));

    expect(itsProjectExtractor.getItsProject("testProject")).andReturn(Optional.empty());

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Map<String, String> changeProperties =
        ImmutableMap.of("project", "testProject", "changeNumber", "176");
    expect(propertyAttributeExtractor.extractFrom(changeAttribute)).andReturn(changeProperties);

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.restorer = Suppliers.ofInstance(accountAttribute);
    Map<String, String> accountProperties = ImmutableMap.of("restorerName", "testName");
    expect(propertyAttributeExtractor.extractFrom(accountAttribute, "restorer"))
        .andReturn(accountProperties);

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Map<String, String> patchSetProperties =
        ImmutableMap.of("revision", "testRevision", "patchSetNumber", "3");
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute)).andReturn(patchSetProperties);

    event.reason = "testReason";
    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Map<String, String> common =
        ImmutableMap.<String, String>builder()
            .putAll(changeProperties)
            .putAll(accountProperties)
            .putAll(patchSetProperties)
            .put("reason", "testReason")
            .put("ref", "refs/heads/testBranch")
            .build();

    eventHelper(event, "ChangeRestoredEvent", "change-restored", common, true);
  }

  public void testCommentAddedEventWOApprovals() {
    CommentAddedEvent event = new CommentAddedEvent(testChange("testProject", "testBranch"));

    expect(itsProjectExtractor.getItsProject("testProject")).andReturn(Optional.empty());

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Map<String, String> changeProperties =
        ImmutableMap.of("project", "testProject", "changeNumber", "176");
    expect(propertyAttributeExtractor.extractFrom(changeAttribute)).andReturn(changeProperties);

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.author = Suppliers.ofInstance(accountAttribute);
    Map<String, String> accountProperties = ImmutableMap.of("commenterName", "testName");
    expect(propertyAttributeExtractor.extractFrom(accountAttribute, "commenter"))
        .andReturn(accountProperties);

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Map<String, String> patchSetProperties =
        ImmutableMap.of("revision", "testRevision", "patchSetNumber", "3");
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute)).andReturn(patchSetProperties);

    event.approvals = Suppliers.ofInstance(null);

    event.comment = "testComment";
    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Map<String, String> common =
        ImmutableMap.<String, String>builder()
            .putAll(changeProperties)
            .putAll(accountProperties)
            .putAll(patchSetProperties)
            .put("ref", "refs/heads/testBranch")
            .put("comment", "testComment")
            .build();

    eventHelper(event, "CommentAddedEvent", "comment-added", common, true);
  }

  public void testCommentAddedEventWApprovals() {
    CommentAddedEvent event = new CommentAddedEvent(testChange("testProject", "testBranch"));

    expect(itsProjectExtractor.getItsProject("testProject")).andReturn(Optional.empty());

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Map<String, String> changeProperties =
        ImmutableMap.of("project", "testProject", "changeNumber", "176");
    expect(propertyAttributeExtractor.extractFrom(changeAttribute)).andReturn(changeProperties);

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.author = Suppliers.ofInstance(accountAttribute);
    Map<String, String> accountProperties = ImmutableMap.of("commenterName", "testName");
    expect(propertyAttributeExtractor.extractFrom(accountAttribute, "commenter"))
        .andReturn(accountProperties);

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Map<String, String> patchSetProperties =
        ImmutableMap.of("revision", "testRevision", "patchSetNumber", "3");
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute)).andReturn(patchSetProperties);

    ApprovalAttribute approvalAttribute1 = createMock(ApprovalAttribute.class);
    Map<String, String> approuvalProperties1 = ImmutableMap.of("approvalCodeReview", "0");
    expect(propertyAttributeExtractor.extractFrom(approvalAttribute1))
        .andReturn(approuvalProperties1);
    ApprovalAttribute approvalAttribute2 = createMock(ApprovalAttribute.class);
    Map<String, String> approuvalProperties2 = ImmutableMap.of("approvalVerified", "0");
    expect(propertyAttributeExtractor.extractFrom(approvalAttribute2))
        .andReturn(approuvalProperties2);
    ApprovalAttribute[] approvalAttributes = {approvalAttribute1, approvalAttribute2};
    event.approvals = Suppliers.ofInstance(approvalAttributes);

    event.comment = "testComment";
    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Map<String, String> common =
        ImmutableMap.<String, String>builder()
            .putAll(changeProperties)
            .putAll(accountProperties)
            .putAll(patchSetProperties)
            .putAll(approuvalProperties1)
            .putAll(approuvalProperties2)
            .put("ref", "refs/heads/testBranch")
            .put("comment", "testComment")
            .build();

    eventHelper(event, "CommentAddedEvent", "comment-added", common, true);
  }

  public void testPatchSetCreatedEvent() {
    PatchSetCreatedEvent event = new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    expect(itsProjectExtractor.getItsProject("testProject")).andReturn(Optional.empty());

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Map<String, String> changeProperties =
        ImmutableMap.of("project", "testProject", "changeNumber", "176");
    expect(propertyAttributeExtractor.extractFrom(changeAttribute)).andReturn(changeProperties);

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.uploader = Suppliers.ofInstance(accountAttribute);
    Map<String, String> accountProperties = ImmutableMap.of("uploaderName", "testName");
    expect(propertyAttributeExtractor.extractFrom(accountAttribute, "uploader"))
        .andReturn(accountProperties);

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Map<String, String> patchSetProperties =
        ImmutableMap.of("revision", "testRevision", "patchSetNumber", "3");
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute)).andReturn(patchSetProperties);

    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Map<String, String> common =
        ImmutableMap.<String, String>builder()
            .putAll(changeProperties)
            .putAll(accountProperties)
            .putAll(patchSetProperties)
            .put("ref", "refs/heads/testBranch")
            .build();

    eventHelper(event, "PatchSetCreatedEvent", "patchset-created", common, true);
  }

  public void testRefUpdatedEvent() {
    RefUpdatedEvent event = new RefUpdatedEvent();

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.submitter = Suppliers.ofInstance(accountAttribute);
    Map<String, String> accountProperties = ImmutableMap.of("submitterName", "testName");
    expect(propertyAttributeExtractor.extractFrom(accountAttribute, "submitter"))
        .andReturn(accountProperties);

    RefUpdateAttribute refUpdateAttribute = createMock(RefUpdateAttribute.class);
    event.refUpdate = Suppliers.ofInstance(refUpdateAttribute);
    Map<String, String> refUpdatedProperties =
        ImmutableMap.of("revision", "testRevision", "revisionOld", "oldRevision");
    expect(propertyAttributeExtractor.extractFrom(refUpdateAttribute))
        .andReturn(refUpdatedProperties);

    refUpdateAttribute.project = "testProject";
    refUpdateAttribute.newRev = "testRevision";
    refUpdateAttribute.oldRev = "oldRevision";
    refUpdateAttribute.refName = "testBranch";

    expect(itsProjectExtractor.getItsProject("testProject")).andReturn(Optional.empty());

    Map<String, String> common =
        ImmutableMap.<String, String>builder()
            .putAll(accountProperties)
            .putAll(refUpdatedProperties)
            .put("ref", "testBranch")
            .put("project", "testProject")
            .build();

    eventHelper(event, "RefUpdatedEvent", "ref-updated", common, false);
  }

  private void eventHelper(
      RefEvent event,
      String className,
      String type,
      Map<String, String> common,
      boolean withRevision) {
    PropertyExtractor propertyExtractor = injector.getInstance(PropertyExtractor.class);

    HashMap<String, Set<String>> issueMap = Maps.newHashMap();
    issueMap.put("4711", Sets.newHashSet("body", "anywhere"));
    issueMap.put("42", Sets.newHashSet("footer", "anywhere"));
    if (withRevision) {
      PatchSet.Id patchSetId = new PatchSet.Id(new Change.Id(176), 3);
      expect(issueExtractor.getIssueIds("testProject", "testRevision", patchSetId))
          .andReturn(issueMap);
    } else {
      expect(issueExtractor.getIssueIds("testProject", "testRevision")).andReturn(issueMap);
    }

    replayMocks();

    Set<Map<String, String>> actual = propertyExtractor.extractFrom(event).getIssuesProperties();

    Map<String, String> propertiesIssue4711 =
        ImmutableMap.<String, String>builder()
            .put("itsName", "ItsTestName")
            .put("event", "com.google.gerrit.server.events." + className)
            .put("event-type", type)
            .put("association", "body anywhere")
            .put("issue", "4711")
            .putAll(common)
            .build();
    Map<String, String> propertiesIssue42 =
        ImmutableMap.<String, String>builder()
            .put("itsName", "ItsTestName")
            .put("event", "com.google.gerrit.server.events." + className)
            .put("event-type", type)
            .put("association", "anywhere footer")
            .put("issue", "42")
            .putAll(common)
            .build();
    Set<Map<String, String>> expected = new HashSet<>();
    expected.add(propertiesIssue4711);
    expected.add(propertiesIssue42);

    assertEquals("Properties do not match", expected, actual);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      bind(String.class).annotatedWith(PluginName.class).toInstance("ItsTestName");

      itsProjectExtractor = createMock(ItsProjectExtractor.class);
      bind(ItsProjectExtractor.class).toInstance(itsProjectExtractor);

      issueExtractor = createMock(IssueExtractor.class);
      bind(IssueExtractor.class).toInstance(issueExtractor);

      propertyAttributeExtractor = createMock(PropertyAttributeExtractor.class);
      bind(PropertyAttributeExtractor.class).toInstance(propertyAttributeExtractor);
    }
  }

  private class DummyEvent extends RefEvent {
    public DummyEvent() {
      super(null);
    }

    @Override
    public String getRefName() {
      return null;
    }

    @Override
    public NameKey getProjectNameKey() {
      return new Project.NameKey("testProject");
    }
  }
}
