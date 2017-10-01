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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.reviewdb.client.Change;
import com.google.gerrit.reviewdb.client.PatchSet;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ApprovalAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.gerrit.server.events.ChangeAbandonedEvent;
import com.google.gerrit.server.events.ChangeMergedEvent;
import com.google.gerrit.server.events.ChangeRestoredEvent;
import com.google.gerrit.server.events.CommentAddedEvent;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.PatchSetCreatedEvent;
import com.google.gerrit.server.events.RefUpdatedEvent;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;

import java.util.HashMap;
import java.util.Set;

public class PropertyExtractorTest extends LoggingMockingTestCase {
  private Injector injector;

  private IssueExtractor issueExtractor;
  private Property.Factory propertyFactory;
  private PropertyAttributeExtractor propertyAttributeExtractor;

  public void testDummyChangeEvent() {
    PropertyExtractor propertyExtractor = injector.getInstance(
        PropertyExtractor.class);

    Property property1 = createMock(Property.class);
    expect(propertyFactory.create("event", "com.googlesource.gerrit.plugins." +
        "its.base.util.PropertyExtractorTest$DummyEvent"))
        .andReturn(property1);

    replayMocks();

    Set<Set<Property>> actual = propertyExtractor.extractFrom(
        new DummyEvent());

    Set<Set<Property>> expected = Sets.newHashSet();
    assertEquals("Properties do not match", expected, actual);
  }

  public void testChangeAbandonedEvent() {
    ChangeAbandonedEvent event =
        new ChangeAbandonedEvent(testChange("testProject", "testBranch"));

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Property propertyChange = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(changeAttribute))
        .andReturn(Sets.newHashSet(propertyChange));

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.abandoner= Suppliers.ofInstance(accountAttribute);
    Property propertySubmitter = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(accountAttribute,
        "abandoner")).andReturn(Sets.newHashSet(propertySubmitter));

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Property propertyPatchSet = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute))
        .andReturn(Sets.newHashSet(propertyPatchSet));

    event.reason = "testReason";
    Property propertyReason = createMock(Property.class);
    expect(propertyFactory.create("reason", "testReason"))
        .andReturn(propertyReason);

    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Set<Property> common = Sets.newHashSet();
    common.add(propertyChange);
    common.add(propertySubmitter);
    common.add(propertyPatchSet);
    common.add(propertyReason);

    eventHelper(event, "ChangeAbandonedEvent", "change-abandoned", common,
        true);
  }

  public void testChangeMergedEvent() {
    ChangeMergedEvent event =
        new ChangeMergedEvent(testChange("testProject", "testBranch"));

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Property propertyChange = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(changeAttribute))
        .andReturn(Sets.newHashSet(propertyChange));

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.submitter = Suppliers.ofInstance(accountAttribute);
    Property propertySubmitter = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(accountAttribute,
        "submitter")).andReturn(Sets.newHashSet(propertySubmitter));

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Property propertyPatchSet = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute))
        .andReturn(Sets.newHashSet(propertyPatchSet));

    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Set<Property> common = Sets.newHashSet();
    common.add(propertyChange);
    common.add(propertySubmitter);
    common.add(propertyPatchSet);

    eventHelper(event, "ChangeMergedEvent", "change-merged", common, true);
  }

  public void testChangeRestoredEvent() {
    ChangeRestoredEvent event =
        new ChangeRestoredEvent(testChange("testProject", "testBranch"));

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Property propertyChange = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(changeAttribute))
        .andReturn(Sets.newHashSet(propertyChange));

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.restorer = Suppliers.ofInstance(accountAttribute);
    Property propertySubmitter = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(accountAttribute,
        "restorer")).andReturn(Sets.newHashSet(propertySubmitter));

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Property propertyPatchSet = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute))
        .andReturn(Sets.newHashSet(propertyPatchSet));

    event.reason = "testReason";
    Property propertyReason = createMock(Property.class);
    expect(propertyFactory.create("reason", "testReason"))
        .andReturn(propertyReason);

    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Set<Property> common = Sets.newHashSet();
    common.add(propertyChange);
    common.add(propertySubmitter);
    common.add(propertyPatchSet);
    common.add(propertyReason);

    eventHelper(event, "ChangeRestoredEvent", "change-restored", common, true);
  }

  public void testCommentAddedEventWOApprovals() {
    CommentAddedEvent event =
        new CommentAddedEvent(testChange("testProject", "testBranch"));

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Property propertyChange = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(changeAttribute))
        .andReturn(Sets.newHashSet(propertyChange));

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.author = Suppliers.ofInstance(accountAttribute);
    Property propertySubmitter = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(accountAttribute,
        "commenter")).andReturn(Sets.newHashSet(propertySubmitter));

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Property propertyPatchSet = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute))
        .andReturn(Sets.newHashSet(propertyPatchSet));

    event.comment = "testComment";
    Property propertyComment = createMock(Property.class);
    expect(propertyFactory.create("comment", "testComment"))
        .andReturn(propertyComment);

    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Set<Property> common = Sets.newHashSet();
    common.add(propertyChange);
    common.add(propertySubmitter);
    common.add(propertyPatchSet);
    common.add(propertyComment);

    eventHelper(event, "CommentAddedEvent", "comment-added", common, true);
  }

  public void testCommentAddedEventWApprovals() {
    CommentAddedEvent event =
        new CommentAddedEvent(testChange("testProject", "testBranch"));

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Property propertyChange = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(changeAttribute))
        .andReturn(Sets.newHashSet(propertyChange));

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.author = Suppliers.ofInstance(accountAttribute);
    Property propertySubmitter = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(accountAttribute,
        "commenter")).andReturn(Sets.newHashSet(propertySubmitter));

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Property propertyPatchSet = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute))
        .andReturn(Sets.newHashSet(propertyPatchSet));

    ApprovalAttribute approvalAttribute1 = createMock(ApprovalAttribute.class);
    Property propertyApproval1 = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(approvalAttribute1))
        .andReturn(Sets.newHashSet(propertyApproval1));
    ApprovalAttribute approvalAttribute2 = createMock(ApprovalAttribute.class);
    Property propertyApproval2 = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(approvalAttribute2))
        .andReturn(Sets.newHashSet(propertyApproval2));
    ApprovalAttribute approvalAttributes[] = { approvalAttribute1,
        approvalAttribute2 };
    event.approvals = Suppliers.ofInstance(approvalAttributes);

    event.comment = "testComment";
    Property propertyComment = createMock(Property.class);
    expect(propertyFactory.create("comment", "testComment"))
        .andReturn(propertyComment);

    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Set<Property> common = Sets.newHashSet();
    common.add(propertyChange);
    common.add(propertySubmitter);
    common.add(propertyPatchSet);
    common.add(propertyComment);
    common.add(propertyApproval1);
    common.add(propertyApproval2);

    eventHelper(event, "CommentAddedEvent", "comment-added", common, true);
  }

  public void testPatchSetCreatedEvent() {
    PatchSetCreatedEvent event =
        new PatchSetCreatedEvent(testChange("testProject", "testBranch"));

    ChangeAttribute changeAttribute = createMock(ChangeAttribute.class);
    event.change = Suppliers.ofInstance(changeAttribute);
    Property propertyChange = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(changeAttribute))
        .andReturn(Sets.newHashSet(propertyChange));

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.uploader = Suppliers.ofInstance(accountAttribute);
    Property propertySubmitter = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(accountAttribute,
        "uploader")).andReturn(Sets.newHashSet(propertySubmitter));

    PatchSetAttribute patchSetAttribute = createMock(PatchSetAttribute.class);
    event.patchSet = Suppliers.ofInstance(patchSetAttribute);
    Property propertyPatchSet = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(patchSetAttribute))
        .andReturn(Sets.newHashSet(propertyPatchSet));

    changeAttribute.project = "testProject";
    changeAttribute.number = 176;
    patchSetAttribute.revision = "testRevision";
    patchSetAttribute.number = 3;

    Set<Property> common = Sets.newHashSet();
    common.add(propertyChange);
    common.add(propertySubmitter);
    common.add(propertyPatchSet);

    eventHelper(event, "PatchSetCreatedEvent", "patchset-created", common,
        true);
  }

  public void testRefUpdatedEvent() {
    RefUpdatedEvent event = new RefUpdatedEvent();

    AccountAttribute accountAttribute = createMock(AccountAttribute.class);
    event.submitter = Suppliers.ofInstance(accountAttribute);
    Property propertySubmitter = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(accountAttribute,
        "submitter")).andReturn(Sets.newHashSet(propertySubmitter));

    RefUpdateAttribute refUpdateAttribute =
        createMock(RefUpdateAttribute.class);
    event.refUpdate = Suppliers.ofInstance(refUpdateAttribute);
    Property propertyRefUpdated = createMock(Property.class);
    expect(propertyAttributeExtractor.extractFrom(refUpdateAttribute))
        .andReturn(Sets.newHashSet(propertyRefUpdated));

    refUpdateAttribute.project = "testProject";
    refUpdateAttribute.newRev = "testRevision";

    Set<Property> common = Sets.newHashSet();
    common.add(propertySubmitter);
    common.add(propertyRefUpdated);

    eventHelper(event, "RefUpdatedEvent", "ref-updated", common, false);
  }

  private void eventHelper(Event event, String className, String type,
      Set<Property> common, boolean withRevision) {
    PropertyExtractor propertyExtractor = injector.getInstance(
        PropertyExtractor.class);

    Property propertyItsName = createMock(Property.class);
    expect(propertyFactory.create("its-name", "ItsTestName"))
        .andReturn(propertyItsName).anyTimes();

    Property propertyEvent = createMock(Property.class);
    expect(propertyFactory.create("event", "com.google.gerrit.server.events." +
        className)).andReturn(propertyEvent);

    Property propertyEventType = createMock(Property.class);
    expect(propertyFactory.create("event-type", type))
        .andReturn(propertyEventType);

    Property propertyAssociationFooter = createMock(Property.class);
    expect(propertyFactory.create("association", "footer"))
        .andReturn(propertyAssociationFooter);

    Property propertyAssociationAnywhere = createMock(Property.class);
    expect(propertyFactory.create("association", "anywhere"))
        .andReturn(propertyAssociationAnywhere).times(2);

    Property propertyAssociationBody = createMock(Property.class);
    expect(propertyFactory.create("association", "body"))
        .andReturn(propertyAssociationBody);

    Property propertyIssue42 = createMock(Property.class);
    expect(propertyFactory.create("issue", "42"))
        .andReturn(propertyIssue42);

    Property propertyIssue4711 = createMock(Property.class);
    expect(propertyFactory.create("issue", "4711"))
        .andReturn(propertyIssue4711);

    HashMap<String,Set<String>> issueMap = Maps.newHashMap();
    issueMap.put("4711", Sets.newHashSet("body", "anywhere"));
    issueMap.put("42", Sets.newHashSet("footer", "anywhere"));
    if (withRevision) {
      PatchSet.Id patchSetId = new PatchSet.Id(new Change.Id(176), 3);
      expect(issueExtractor.getIssueIds("testProject", "testRevision",
          patchSetId)).andReturn(issueMap);
    } else {
      expect(issueExtractor.getIssueIds("testProject", "testRevision"))
      .andReturn(issueMap);
    }

    replayMocks();

    Set<Set<Property>> actual = propertyExtractor.extractFrom(event);

    Set<Set<Property>> expected = Sets.newHashSet();
    Set<Property> properties = Sets.newHashSet();
    properties.add(propertyItsName);
    properties.add(propertyEvent);
    properties.add(propertyEventType);
    properties.add(propertyAssociationAnywhere);
    properties.add(propertyAssociationFooter);
    properties.add(propertyIssue42);
    properties.addAll(common);
    expected.add(properties);

    properties = Sets.newHashSet();
    properties.add(propertyItsName);
    properties.add(propertyEvent);
    properties.add(propertyEventType);
    properties.add(propertyAssociationAnywhere);
    properties.add(propertyAssociationBody);
    properties.add(propertyIssue4711);
    properties.addAll(common);
    expected.add(properties);
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
      bind(String.class).annotatedWith(PluginName.class)
          .toInstance("ItsTestName");

      issueExtractor = createMock(IssueExtractor.class);
      bind(IssueExtractor.class).toInstance(issueExtractor);

      propertyAttributeExtractor = createMock(PropertyAttributeExtractor.class);
      bind(PropertyAttributeExtractor.class).toInstance(
          propertyAttributeExtractor);

      propertyFactory = createMock(Property.Factory.class);
      bind(Property.Factory.class).toInstance(propertyFactory);
      //factory(Property.Factory.class);
    }
  }

  private class DummyEvent extends Event {
    public DummyEvent() {
      super(null);
    }
  }
}