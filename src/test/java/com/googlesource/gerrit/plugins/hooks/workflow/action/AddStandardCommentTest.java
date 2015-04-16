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
package com.googlesource.gerrit.plugins.hooks.workflow.action;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.Sets;
import com.google.gerrit.server.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;
import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.hooks.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.hooks.workflow.Property;

import java.io.IOException;
import java.util.Set;

public class AddStandardCommentTest extends LoggingMockingTestCase {
  private Injector injector;

  private ItsFacade its;

  public void testChangeMergedPlain() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Set<Property> properties = Sets.newHashSet();

    Property propertyEventType = createMock(Property.class);
    expect(propertyEventType.getKey()).andReturn("event-type").anyTimes();
    expect(propertyEventType.getValue()).andReturn("change-merged").anyTimes();
    properties.add(propertyEventType);

    its.addComment("42", "Change merged");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("42", actionRequest, properties);
  }

  public void testChangeMergedFull() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Set<Property> properties = Sets.newHashSet();

    Property propertyEventType = createMock(Property.class);
    expect(propertyEventType.getKey()).andReturn("event-type").anyTimes();
    expect(propertyEventType.getValue()).andReturn("change-merged").anyTimes();
    properties.add(propertyEventType);

    Property propertySubject = createMock(Property.class);
    expect(propertySubject.getKey()).andReturn("subject").anyTimes();
    expect(propertySubject.getValue()).andReturn("Test-Change-Subject").anyTimes();
    properties.add(propertySubject);

    Property propertyChangeNumber = createMock(Property.class);
    expect(propertyChangeNumber.getKey()).andReturn("change-number")
        .anyTimes();
    expect(propertyChangeNumber.getValue()).andReturn("4711").anyTimes();
    properties.add(propertyChangeNumber);

    Property propertySubmitterName = createMock(Property.class);
    expect(propertySubmitterName.getKey()).andReturn("submitter-name")
        .anyTimes();
    expect(propertySubmitterName.getValue()).andReturn("John Doe").anyTimes();
    properties.add(propertySubmitterName);

    Property propertyChangeUrl= createMock(Property.class);
    expect(propertyChangeUrl.getKey()).andReturn("change-url").anyTimes();
    expect(propertyChangeUrl.getValue()).andReturn("http://example.org/change")
        .anyTimes();
    properties.add(propertyChangeUrl);

    expect(its.createLinkForWebui("http://example.org/change",
        "http://example.org/change")).andReturn("HtTp://ExAmPlE.OrG/ChAnGe");

    its.addComment("176", "Change 4711 merged by John Doe:\n" +
        "Test-Change-Subject\n" +
        "\n" +
        "HtTp://ExAmPlE.OrG/ChAnGe");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("176", actionRequest, properties);
  }

  public void testChangeAbandonedPlain() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Set<Property> properties = Sets.newHashSet();

    Property propertyEventType = createMock(Property.class);
    expect(propertyEventType.getKey()).andReturn("event-type").anyTimes();
    expect(propertyEventType.getValue()).andReturn("change-abandoned").anyTimes();
    properties.add(propertyEventType);

    its.addComment("42", "Change abandoned");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("42", actionRequest, properties);
  }

  public void testChangeAbandonedFull() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Set<Property> properties = Sets.newHashSet();

    Property propertyEventType = createMock(Property.class);
    expect(propertyEventType.getKey()).andReturn("event-type").anyTimes();
    expect(propertyEventType.getValue()).andReturn("change-abandoned").anyTimes();
    properties.add(propertyEventType);

    Property propertyReason = createMock(Property.class);
    expect(propertyReason.getKey()).andReturn("reason").anyTimes();
    expect(propertyReason.getValue()).andReturn("Test-Reason").anyTimes();
    properties.add(propertyReason);

    Property propertySubject = createMock(Property.class);
    expect(propertySubject.getKey()).andReturn("subject").anyTimes();
    expect(propertySubject.getValue()).andReturn("Test-Change-Subject").anyTimes();
    properties.add(propertySubject);

    Property propertyChangeNumber = createMock(Property.class);
    expect(propertyChangeNumber.getKey()).andReturn("change-number")
        .anyTimes();
    expect(propertyChangeNumber.getValue()).andReturn("4711").anyTimes();
    properties.add(propertyChangeNumber);

    Property propertySubmitterName = createMock(Property.class);
    expect(propertySubmitterName.getKey()).andReturn("abandoner-name")
        .anyTimes();
    expect(propertySubmitterName.getValue()).andReturn("John Doe").anyTimes();
    properties.add(propertySubmitterName);

    Property propertyChangeUrl= createMock(Property.class);
    expect(propertyChangeUrl.getKey()).andReturn("change-url").anyTimes();
    expect(propertyChangeUrl.getValue()).andReturn("http://example.org/change")
        .anyTimes();
    properties.add(propertyChangeUrl);

    expect(its.createLinkForWebui("http://example.org/change",
        "http://example.org/change")).andReturn("HtTp://ExAmPlE.OrG/ChAnGe");

    its.addComment("176", "Change 4711 abandoned by John Doe:\n" +
        "Test-Change-Subject\n" +
        "\n" +
        "Reason:\n" +
        "Test-Reason\n" +
        "\n" +
        "HtTp://ExAmPlE.OrG/ChAnGe");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("176", actionRequest, properties);
  }

  public void testChangeRestoredPlain() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Set<Property> properties = Sets.newHashSet();

    Property propertyEventType = createMock(Property.class);
    expect(propertyEventType.getKey()).andReturn("event-type").anyTimes();
    expect(propertyEventType.getValue()).andReturn("change-restored").anyTimes();
    properties.add(propertyEventType);

    its.addComment("42", "Change restored");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("42", actionRequest, properties);
  }

  public void testChangeRestoredFull() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Set<Property> properties = Sets.newHashSet();

    Property propertyEventType = createMock(Property.class);
    expect(propertyEventType.getKey()).andReturn("event-type").anyTimes();
    expect(propertyEventType.getValue()).andReturn("change-restored").anyTimes();
    properties.add(propertyEventType);

    Property propertyReason = createMock(Property.class);
    expect(propertyReason.getKey()).andReturn("reason").anyTimes();
    expect(propertyReason.getValue()).andReturn("Test-Reason").anyTimes();
    properties.add(propertyReason);

    Property propertySubject = createMock(Property.class);
    expect(propertySubject.getKey()).andReturn("subject").anyTimes();
    expect(propertySubject.getValue()).andReturn("Test-Change-Subject").anyTimes();
    properties.add(propertySubject);

    Property propertyChangeNumber = createMock(Property.class);
    expect(propertyChangeNumber.getKey()).andReturn("change-number")
        .anyTimes();
    expect(propertyChangeNumber.getValue()).andReturn("4711").anyTimes();
    properties.add(propertyChangeNumber);

    Property propertySubmitterName = createMock(Property.class);
    expect(propertySubmitterName.getKey()).andReturn("restorer-name")
        .anyTimes();
    expect(propertySubmitterName.getValue()).andReturn("John Doe").anyTimes();
    properties.add(propertySubmitterName);

    Property propertyChangeUrl= createMock(Property.class);
    expect(propertyChangeUrl.getKey()).andReturn("change-url").anyTimes();
    expect(propertyChangeUrl.getValue()).andReturn("http://example.org/change")
        .anyTimes();
    properties.add(propertyChangeUrl);

    expect(its.createLinkForWebui("http://example.org/change",
        "http://example.org/change")).andReturn("HtTp://ExAmPlE.OrG/ChAnGe");

    its.addComment("176", "Change 4711 restored by John Doe:\n" +
        "Test-Change-Subject\n" +
        "\n" +
        "Reason:\n" +
        "Test-Reason\n" +
        "\n" +
        "HtTp://ExAmPlE.OrG/ChAnGe");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("176", actionRequest, properties);
  }

  public void testPatchSetCreatedPlain() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Set<Property> properties = Sets.newHashSet();

    Property propertyEventType = createMock(Property.class);
    expect(propertyEventType.getKey()).andReturn("event-type").anyTimes();
    expect(propertyEventType.getValue()).andReturn("patchset-created").anyTimes();
    properties.add(propertyEventType);

    its.addComment("42", "Change had a related patch set uploaded");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("42", actionRequest, properties);
  }

  public void testPatchSetCreatedFull() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);

    Set<Property> properties = Sets.newHashSet();

    Property propertyEventType = createMock(Property.class);
    expect(propertyEventType.getKey()).andReturn("event-type").anyTimes();
    expect(propertyEventType.getValue()).andReturn("patchset-created").anyTimes();
    properties.add(propertyEventType);

    Property propertySubject = createMock(Property.class);
    expect(propertySubject.getKey()).andReturn("subject").anyTimes();
    expect(propertySubject.getValue()).andReturn("Test-Change-Subject").anyTimes();
    properties.add(propertySubject);

    Property propertyChangeNumber = createMock(Property.class);
    expect(propertyChangeNumber.getKey()).andReturn("change-number")
        .anyTimes();
    expect(propertyChangeNumber.getValue()).andReturn("4711").anyTimes();
    properties.add(propertyChangeNumber);

    Property propertySubmitterName = createMock(Property.class);
    expect(propertySubmitterName.getKey()).andReturn("uploader-name")
        .anyTimes();
    expect(propertySubmitterName.getValue()).andReturn("John Doe").anyTimes();
    properties.add(propertySubmitterName);

    Property propertyChangeUrl= createMock(Property.class);
    expect(propertyChangeUrl.getKey()).andReturn("change-url").anyTimes();
    expect(propertyChangeUrl.getValue()).andReturn("http://example.org/change")
        .anyTimes();
    properties.add(propertyChangeUrl);

    expect(its.createLinkForWebui("http://example.org/change",
        "http://example.org/change")).andReturn("HtTp://ExAmPlE.OrG/ChAnGe");

    its.addComment("176", "Change 4711 had a related patch set uploaded by " +
        "John Doe:\n" +
        "Test-Change-Subject\n" +
        "\n" +
        "HtTp://ExAmPlE.OrG/ChAnGe");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("176", actionRequest, properties);
  }

  public void setUp() throws Exception {
    super.setUp();

    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      its = createMock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(its);
    }
  }
}