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
package com.googlesource.gerrit.plugins.hooks.util;

import static org.easymock.EasyMock.expect;

import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gerrit.reviewdb.client.Change.Status;
import com.google.gerrit.server.config.FactoryModule;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ApprovalAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.hooks.workflow.Property;

public class PropertyAttributeExtractorTest extends LoggingMockingTestCase {
  private Injector injector;

  private Property.Factory propertyFactory;

  public void testAccountAttributeNull() {
    replayMocks();

    PropertyAttributeExtractor extractor =
        injector.getInstance(PropertyAttributeExtractor.class);

    Set<Property> actual = extractor.extractFrom(null, "prefix");

    Set<Property> expected = Sets.newHashSet();

    assertEquals("Properties do not match", expected, actual);
  }

  public void testAccountAttribute() {
    AccountAttribute accountAttribute = new AccountAttribute();
    accountAttribute.email = "testEmail";
    accountAttribute.name = "testName";
    accountAttribute.username = "testUsername";

    Property propertyEmail= createMock(Property.class);
    expect(propertyFactory.create("prefix-email", "testEmail"))
        .andReturn(propertyEmail);

    Property propertyName = createMock(Property.class);
    expect(propertyFactory.create("prefix-name", "testName"))
        .andReturn(propertyName);

    Property propertyUsername = createMock(Property.class);
    expect(propertyFactory.create("prefix-username", "testUsername"))
        .andReturn(propertyUsername);

    replayMocks();

    PropertyAttributeExtractor extractor =
        injector.getInstance(PropertyAttributeExtractor.class);

    Set<Property> actual = extractor.extractFrom(accountAttribute, "prefix");

    Set<Property> expected = Sets.newHashSet();
    expected.add(propertyEmail);
    expected.add(propertyName);
    expected.add(propertyUsername);
    assertEquals("Properties do not match", expected, actual);
  }

  public void testChangeAttribute() {
    AccountAttribute owner = new AccountAttribute();
    owner.email = "testEmail";
    owner.name = "testName";
    owner.username = "testUsername";

    ChangeAttribute changeAttribute = new ChangeAttribute();
    changeAttribute.project = "testProject";
    changeAttribute.branch = "testBranch";
    changeAttribute.topic = "testTopic";
    changeAttribute.subject = "testSubject";
    changeAttribute.id = "testId";
    changeAttribute.number = "4711";
    changeAttribute.url = "http://www.example.org/test";
    changeAttribute.owner = owner;

    Property propertyProject = createMock(Property.class);
    expect(propertyFactory.create("project", "testProject"))
        .andReturn(propertyProject);

    Property propertyBranch = createMock(Property.class);
    expect(propertyFactory.create("branch", "testBranch"))
        .andReturn(propertyBranch);

    Property propertyTopic = createMock(Property.class);
    expect(propertyFactory.create("topic", "testTopic"))
        .andReturn(propertyTopic);

    Property propertySubject = createMock(Property.class);
    expect(propertyFactory.create("subject", "testSubject"))
        .andReturn(propertySubject);

    Property propertyId = createMock(Property.class);
    expect(propertyFactory.create("change-id", "testId"))
        .andReturn(propertyId);

    Property propertyNumber = createMock(Property.class);
    expect(propertyFactory.create("change-number", "4711"))
        .andReturn(propertyNumber);

    Property propertyUrl = createMock(Property.class);
    expect(propertyFactory.create("change-url", "http://www.example.org/test"))
        .andReturn(propertyUrl);

    Property propertyStatus = createMock(Property.class);
    expect(propertyFactory.create("status", null))
        .andReturn(propertyStatus);

    Property propertyEmail= createMock(Property.class);
    expect(propertyFactory.create("owner-email", "testEmail"))
        .andReturn(propertyEmail);

    Property propertyName = createMock(Property.class);
    expect(propertyFactory.create("owner-name", "testName"))
        .andReturn(propertyName);

    Property propertyUsername = createMock(Property.class);
    expect(propertyFactory.create("owner-username", "testUsername"))
        .andReturn(propertyUsername);


    replayMocks();

    PropertyAttributeExtractor extractor =
        injector.getInstance(PropertyAttributeExtractor.class);

    Set<Property> actual = extractor.extractFrom(changeAttribute);

    Set<Property> expected = Sets.newHashSet();
    expected.add(propertyProject);
    expected.add(propertyBranch);
    expected.add(propertyTopic);
    expected.add(propertySubject);
    expected.add(propertyId);
    expected.add(propertyNumber);
    expected.add(propertyUrl);
    expected.add(propertyStatus);
    expected.add(propertyEmail);
    expected.add(propertyName);
    expected.add(propertyUsername);
    assertEquals("Properties do not match", expected, actual);
  }

  public void testChangeAttributeFull() {
    AccountAttribute owner = new AccountAttribute();
    owner.email = "testEmail";
    owner.name = "testName";
    owner.username = "testUsername";

    ChangeAttribute changeAttribute = new ChangeAttribute();
    changeAttribute.project = "testProject";
    changeAttribute.branch = "testBranch";
    changeAttribute.topic = "testTopic";
    changeAttribute.subject = "testSubject";
    changeAttribute.id = "testId";
    changeAttribute.number = "4711";
    changeAttribute.url = "http://www.example.org/test";
    changeAttribute.status = Status.ABANDONED;
    changeAttribute.owner = owner;

    Property propertyProject = createMock(Property.class);
    expect(propertyFactory.create("project", "testProject"))
        .andReturn(propertyProject);

    Property propertyBranch = createMock(Property.class);
    expect(propertyFactory.create("branch", "testBranch"))
        .andReturn(propertyBranch);

    Property propertyTopic = createMock(Property.class);
    expect(propertyFactory.create("topic", "testTopic"))
        .andReturn(propertyTopic);

    Property propertySubject = createMock(Property.class);
    expect(propertyFactory.create("subject", "testSubject"))
        .andReturn(propertySubject);

    Property propertyId = createMock(Property.class);
    expect(propertyFactory.create("change-id", "testId"))
        .andReturn(propertyId);

    Property propertyNumber = createMock(Property.class);
    expect(propertyFactory.create("change-number", "4711"))
        .andReturn(propertyNumber);

    Property propertyUrl = createMock(Property.class);
    expect(propertyFactory.create("change-url", "http://www.example.org/test"))
        .andReturn(propertyUrl);

    Property propertyStatus = createMock(Property.class);
    expect(propertyFactory.create("status", "ABANDONED"))
        .andReturn(propertyStatus);

    Property propertyEmail= createMock(Property.class);
    expect(propertyFactory.create("owner-email", "testEmail"))
        .andReturn(propertyEmail);

    Property propertyName = createMock(Property.class);
    expect(propertyFactory.create("owner-name", "testName"))
        .andReturn(propertyName);

    Property propertyUsername = createMock(Property.class);
    expect(propertyFactory.create("owner-username", "testUsername"))
        .andReturn(propertyUsername);


    replayMocks();

    PropertyAttributeExtractor extractor =
        injector.getInstance(PropertyAttributeExtractor.class);

    Set<Property> actual = extractor.extractFrom(changeAttribute);

    Set<Property> expected = Sets.newHashSet();
    expected.add(propertyProject);
    expected.add(propertyBranch);
    expected.add(propertyTopic);
    expected.add(propertySubject);
    expected.add(propertyId);
    expected.add(propertyNumber);
    expected.add(propertyUrl);
    expected.add(propertyStatus);
    expected.add(propertyEmail);
    expected.add(propertyName);
    expected.add(propertyUsername);
    assertEquals("Properties do not match", expected, actual);
  }

  public void testPatchSetAttribute() {
    AccountAttribute uploader = new AccountAttribute();
    uploader.email = "testEmail1";
    uploader.name = "testName1";
    uploader.username = "testUsername1";

    AccountAttribute author = new AccountAttribute();
    author.email = "testEmail2";
    author.name = "testName2";
    author.username = "testUsername2";

    PatchSetAttribute patchSetAttribute = new PatchSetAttribute();
    patchSetAttribute.revision = "1234567891123456789212345678931234567894";
    patchSetAttribute.number = "42";
    patchSetAttribute.ref = "testRef";
    patchSetAttribute.createdOn = 1234567890L;
    patchSetAttribute.parents = Lists.newArrayList("parent1", "parent2");
    patchSetAttribute.sizeDeletions = 7;
    patchSetAttribute.sizeInsertions = 12;
    patchSetAttribute.uploader = uploader;
    patchSetAttribute.author = author;

    Property propertyRevision = createMock(Property.class);
    expect(propertyFactory.create("revision",
        "1234567891123456789212345678931234567894"))
        .andReturn(propertyRevision);

    Property propertyNumber = createMock(Property.class);
    expect(propertyFactory.create("patch-set-number", "42"))
        .andReturn(propertyNumber);

    Property propertyRef = createMock(Property.class);
    expect(propertyFactory.create("ref", "testRef"))
        .andReturn(propertyRef);

    Property propertyCreatedOn = createMock(Property.class);
    expect(propertyFactory.create("created-on", "1234567890"))
        .andReturn(propertyCreatedOn);

    Property propertyParents = createMock(Property.class);
    expect(propertyFactory.create("parents", "[parent1, parent2]"))
        .andReturn(propertyParents);

    Property propertyDeletions = createMock(Property.class);
    expect(propertyFactory.create("deletions", "7"))
        .andReturn(propertyDeletions);

    Property propertyInsertions = createMock(Property.class);
    expect(propertyFactory.create("insertions", "12"))
        .andReturn(propertyInsertions);

    Property propertyEmail1 = createMock(Property.class);
    expect(propertyFactory.create("uploader-email", "testEmail1"))
        .andReturn(propertyEmail1);

    Property propertyName1 = createMock(Property.class);
    expect(propertyFactory.create("uploader-name", "testName1"))
        .andReturn(propertyName1);

    Property propertyUsername1 = createMock(Property.class);
    expect(propertyFactory.create("uploader-username", "testUsername1"))
        .andReturn(propertyUsername1);

    Property propertyEmail2 = createMock(Property.class);
    expect(propertyFactory.create("author-email", "testEmail2"))
        .andReturn(propertyEmail2);

    Property propertyName2 = createMock(Property.class);
    expect(propertyFactory.create("author-name", "testName2"))
        .andReturn(propertyName2);

    Property propertyUsername2 = createMock(Property.class);
    expect(propertyFactory.create("author-username", "testUsername2"))
        .andReturn(propertyUsername2);

    replayMocks();

    PropertyAttributeExtractor extractor =
        injector.getInstance(PropertyAttributeExtractor.class);

    Set<Property> actual = extractor.extractFrom(patchSetAttribute);

    Set<Property> expected = Sets.newHashSet();
    expected.add(propertyRevision);
    expected.add(propertyNumber);
    expected.add(propertyRef);
    expected.add(propertyCreatedOn);
    expected.add(propertyParents);
    expected.add(propertyDeletions);
    expected.add(propertyInsertions);
    expected.add(propertyEmail1);
    expected.add(propertyName1);
    expected.add(propertyUsername1);
    expected.add(propertyEmail2);
    expected.add(propertyName2);
    expected.add(propertyUsername2);
    assertEquals("Properties do not match", expected, actual);
  }

  public void testRefUpdateAttribute() {
    RefUpdateAttribute refUpdateAttribute = new RefUpdateAttribute();
    refUpdateAttribute.newRev = "1234567891123456789212345678931234567894";
    refUpdateAttribute.oldRev = "9876543211987654321298765432139876543214";
    refUpdateAttribute.project = "testProject";
    refUpdateAttribute.refName = "testRef";

    Property propertyRevision = createMock(Property.class);
    expect(propertyFactory.create("revision",
        "1234567891123456789212345678931234567894"))
        .andReturn(propertyRevision);

    Property propertyRevisionOld = createMock(Property.class);
    expect(propertyFactory.create("revision-old",
        "9876543211987654321298765432139876543214"))
        .andReturn(propertyRevisionOld);

    Property propertyProject = createMock(Property.class);
    expect(propertyFactory.create("project", "testProject"))
        .andReturn(propertyProject);

    Property propertyRef = createMock(Property.class);
    expect(propertyFactory.create("ref", "testRef"))
        .andReturn(propertyRef);

    replayMocks();

    PropertyAttributeExtractor extractor =
        injector.getInstance(PropertyAttributeExtractor.class);

    Set<Property> actual = extractor.extractFrom(refUpdateAttribute);

    Set<Property> expected = Sets.newHashSet();
    expected.add(propertyRevision);
    expected.add(propertyRevisionOld);
    expected.add(propertyProject);
    expected.add(propertyRef);
    assertEquals("Properties do not match", expected, actual);
  }

  public void testApprovalAttribute() {
    ApprovalAttribute approvalAttribute = new ApprovalAttribute();
    approvalAttribute.type = "TestType";
    approvalAttribute.value = "TestValue";

    Property propertyApproval = createMock(Property.class);
    expect(propertyFactory.create("approval-TestType", "TestValue"))
        .andReturn(propertyApproval);

    replayMocks();

    PropertyAttributeExtractor extractor =
        injector.getInstance(PropertyAttributeExtractor.class);

    Set<Property> actual = extractor.extractFrom(approvalAttribute);

    Set<Property> expected = Sets.newHashSet();
    expected.add(propertyApproval);
    assertEquals("Properties do not match", expected, actual);
  }

  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      propertyFactory = createMock(Property.Factory.class);
      bind(Property.Factory.class).toInstance(propertyFactory);
    }
  }
}