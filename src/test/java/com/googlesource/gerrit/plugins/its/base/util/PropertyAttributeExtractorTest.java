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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.Change.Status;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.server.data.AccountAttribute;
import com.google.gerrit.server.data.ApprovalAttribute;
import com.google.gerrit.server.data.ChangeAttribute;
import com.google.gerrit.server.data.PatchSetAttribute;
import com.google.gerrit.server.data.RefUpdateAttribute;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.util.HashMap;
import java.util.Map;

public class PropertyAttributeExtractorTest extends LoggingMockingTestCase {
  private Injector injector;

  private ItsFacade facade;

  public void testAccountAttributeNull() {
    PropertyAttributeExtractor extractor = injector.getInstance(PropertyAttributeExtractor.class);

    Map<String, String> actual = extractor.extractFrom(null, "prefix");

    Map<String, String> expected = new HashMap<>();

    assertEquals("Properties do not match", expected, actual);
  }

  public void testAccountAttribute() {
    AccountAttribute accountAttribute = new AccountAttribute();
    accountAttribute.email = "testEmail";
    accountAttribute.name = "testName";
    accountAttribute.username = "testUsername";

    PropertyAttributeExtractor extractor = injector.getInstance(PropertyAttributeExtractor.class);

    Map<String, String> actual = extractor.extractFrom(accountAttribute, "prefix");

    ImmutableMap<String, String> expected =
        new ImmutableMap.Builder<String, String>()
            .put("prefixEmail", "testEmail")
            .put("prefixName", "testName")
            .put("prefixUsername", "testUsername")
            .build();
    assertEquals("Properties do not match", expected, actual);
  }

  public void testChangeAttribute() {
    AccountAttribute owner = new AccountAttribute();
    owner.email = "testEmail";
    owner.name = "testName";
    owner.username = "testUsername";

    ChangeAttribute changeAttribute = new ChangeAttribute();
    changeAttribute.branch = "testBranch";
    changeAttribute.topic = "testTopic";
    changeAttribute.subject = "testSubject";
    changeAttribute.id = "testId";
    changeAttribute.number = 4711;
    changeAttribute.url = "http://www.example.org/test";
    changeAttribute.owner = owner;
    changeAttribute.commitMessage = "Commit Message";
    changeAttribute.status = Change.Status.NEW;

    when(facade.createLinkForWebui("http://www.example.org/test", "http://www.example.org/test"))
        .thenReturn("http://www.example.org/test");

    PropertyAttributeExtractor extractor = injector.getInstance(PropertyAttributeExtractor.class);

    Map<String, String> actual = extractor.extractFrom(changeAttribute);

    ImmutableMap<String, String> expected =
        new ImmutableMap.Builder<String, String>()
            .put("branch", "testBranch")
            .put("topic", "testTopic")
            .put("subject", "testSubject")
            .put("escapedSubject", "testSubject")
            .put("changeId", "testId")
            .put("changeNumber", "4711")
            .put("changeUrl", "http://www.example.org/test")
            .put("status", Change.Status.NEW.name())
            .put("ownerEmail", "testEmail")
            .put("ownerName", "testName")
            .put("ownerUsername", "testUsername")
            .put("commitMessage", "Commit Message")
            .put("formatChangeUrl", "http://www.example.org/test")
            .put("private", "false")
            .put("wip", "false")
            .build();
    assertEquals("Properties do not match", expected, actual);
  }

  public void testChangeAttributeNoOwnerEmail() {
    AccountAttribute owner = new AccountAttribute();
    owner.name = "testName";
    owner.username = "testUsername";

    ChangeAttribute changeAttribute = new ChangeAttribute();
    changeAttribute.branch = "testBranch";
    changeAttribute.topic = "testTopic";
    changeAttribute.subject = "testSubject";
    changeAttribute.id = "testId";
    changeAttribute.number = 4711;
    changeAttribute.url = "http://www.example.org/test";
    changeAttribute.owner = owner;
    changeAttribute.commitMessage = "Commit Message";
    changeAttribute.status = Change.Status.NEW;

    when(facade.createLinkForWebui("http://www.example.org/test", "http://www.example.org/test"))
        .thenReturn("http://www.example.org/test");

    PropertyAttributeExtractor extractor = injector.getInstance(PropertyAttributeExtractor.class);

    Map<String, String> actual = extractor.extractFrom(changeAttribute);

    ImmutableMap<String, String> expected =
        new ImmutableMap.Builder<String, String>()
            .put("branch", "testBranch")
            .put("topic", "testTopic")
            .put("subject", "testSubject")
            .put("escapedSubject", "testSubject")
            .put("changeId", "testId")
            .put("changeNumber", "4711")
            .put("changeUrl", "http://www.example.org/test")
            .put("status", Change.Status.NEW.name())
            .put("ownerName", "testName")
            .put("ownerUsername", "testUsername")
            .put("commitMessage", "Commit Message")
            .put("formatChangeUrl", "http://www.example.org/test")
            .put("private", "false")
            .put("wip", "false")
            .build();
    assertEquals("Properties do not match", expected, actual);
  }

  public void testChangeAttributeFull() {
    AccountAttribute owner = new AccountAttribute();
    owner.email = "testEmail";
    owner.name = "testName";
    owner.username = "testUsername";

    ChangeAttribute changeAttribute = new ChangeAttribute();
    changeAttribute.branch = "testBranch";
    changeAttribute.topic = "testTopic";
    changeAttribute.subject = "testSubject";
    changeAttribute.id = "testId";
    changeAttribute.number = 4711;
    changeAttribute.url = "http://www.example.org/test";
    changeAttribute.status = Status.ABANDONED;
    changeAttribute.owner = owner;
    changeAttribute.commitMessage = "Commit Message";

    when(facade.createLinkForWebui("http://www.example.org/test", "http://www.example.org/test"))
        .thenReturn("http://www.example.org/test");

    PropertyAttributeExtractor extractor = injector.getInstance(PropertyAttributeExtractor.class);

    Map<String, String> actual = extractor.extractFrom(changeAttribute);

    ImmutableMap<String, String> expected =
        new ImmutableMap.Builder<String, String>()
            .put("branch", "testBranch")
            .put("topic", "testTopic")
            .put("subject", "testSubject")
            .put("escapedSubject", "testSubject")
            .put("changeId", "testId")
            .put("changeNumber", "4711")
            .put("changeUrl", "http://www.example.org/test")
            .put("status", Change.Status.ABANDONED.name())
            .put("ownerEmail", "testEmail")
            .put("ownerName", "testName")
            .put("ownerUsername", "testUsername")
            .put("commitMessage", "Commit Message")
            .put("formatChangeUrl", "http://www.example.org/test")
            .put("private", "false")
            .put("wip", "false")
            .build();
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
    patchSetAttribute.number = 42;
    patchSetAttribute.ref = "testRef";
    patchSetAttribute.createdOn = 1234567890L;
    patchSetAttribute.parents = Lists.newArrayList("parent1", "parent2");
    patchSetAttribute.sizeDeletions = 7;
    patchSetAttribute.sizeInsertions = 12;
    patchSetAttribute.uploader = uploader;
    patchSetAttribute.author = author;

    PropertyAttributeExtractor extractor = injector.getInstance(PropertyAttributeExtractor.class);

    Map<String, String> actual = extractor.extractFrom(patchSetAttribute);

    ImmutableMap<String, String> expected =
        new ImmutableMap.Builder<String, String>()
            .put("revision", "1234567891123456789212345678931234567894")
            .put("patchSetNumber", "42")
            .put("ref", "testRef")
            .put("createdOn", "1234567890")
            .put("parents", "[parent1, parent2]")
            .put("deletions", "7")
            .put("insertions", "12")
            .put("uploaderEmail", "testEmail1")
            .put("uploaderName", "testName1")
            .put("uploaderUsername", "testUsername1")
            .put("authorEmail", "testEmail2")
            .put("authorName", "testName2")
            .put("authorUsername", "testUsername2")
            .build();
    assertEquals("Properties do not match", expected, actual);
  }

  public void testRefUpdateAttribute() {
    RefUpdateAttribute refUpdateAttribute = new RefUpdateAttribute();
    refUpdateAttribute.newRev = "1234567891123456789212345678931234567894";
    refUpdateAttribute.oldRev = "9876543211987654321298765432139876543214";
    refUpdateAttribute.refName = "refs/heads/master";

    PropertyAttributeExtractor extractor = injector.getInstance(PropertyAttributeExtractor.class);

    Map<String, String> actual = extractor.extractFrom(refUpdateAttribute);

    ImmutableMap<String, String> expected =
        new ImmutableMap.Builder<String, String>()
            .put("revision", "1234567891123456789212345678931234567894")
            .put("revisionOld", "9876543211987654321298765432139876543214")
            .put("ref", "refs/heads/master")
            .put("refSuffix", "master")
            .put("refPrefix", "refs/heads/")
            .build();
    assertEquals("Properties do not match", expected, actual);
  }

  public void testApprovalAttribute() {
    ApprovalAttribute approvalAttribute = new ApprovalAttribute();
    approvalAttribute.type = "TestType";
    approvalAttribute.value = "TestValue";

    PropertyAttributeExtractor extractor = injector.getInstance(PropertyAttributeExtractor.class);

    Map<String, String> actual = extractor.extractFrom(approvalAttribute);

    Map<String, String> expected = ImmutableMap.of("approvalTestType", "TestValue");
    assertEquals("Properties do not match", expected, actual);
  }

  public void testApprovalAttributeWithDash() {
    ApprovalAttribute approvalAttribute = new ApprovalAttribute();
    approvalAttribute.type = "Test-Type";
    approvalAttribute.value = "TestValue";

    PropertyAttributeExtractor extractor = injector.getInstance(PropertyAttributeExtractor.class);

    Map<String, String> actual = extractor.extractFrom(approvalAttribute);

    Map<String, String> expected = ImmutableMap.of("approvalTestType", "TestValue");
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
      facade = mock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(facade);
    }
  }
}
