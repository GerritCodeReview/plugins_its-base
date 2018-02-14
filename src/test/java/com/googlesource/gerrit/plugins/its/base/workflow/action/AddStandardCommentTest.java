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
package com.googlesource.gerrit.plugins.its.base.workflow.action;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.Sets;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.its.base.workflow.Property;
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
    expect(propertyChangeNumber.getKey()).andReturn("changeNumber").anyTimes();
    expect(propertyChangeNumber.getValue()).andReturn("4711").anyTimes();
    properties.add(propertyChangeNumber);

    Property propertySubmitterName = createMock(Property.class);
    expect(propertySubmitterName.getKey()).andReturn("submitterName").anyTimes();
    expect(propertySubmitterName.getValue()).andReturn("John Doe").anyTimes();
    properties.add(propertySubmitterName);

    Property propertyChangeUrl = createMock(Property.class);
    expect(propertyChangeUrl.getKey()).andReturn("formatChangeUrl").anyTimes();
    expect(propertyChangeUrl.getValue()).andReturn("HtTp://ExAmPlE.OrG/ChAnGe").anyTimes();
    properties.add(propertyChangeUrl);

    its.addComment(
        "176",
        "Change 4711 merged by John Doe:\n"
            + "Test-Change-Subject\n"
            + "\n"
            + "HtTp://ExAmPlE.OrG/ChAnGe");
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
    expect(propertyChangeNumber.getKey()).andReturn("changeNumber").anyTimes();
    expect(propertyChangeNumber.getValue()).andReturn("4711").anyTimes();
    properties.add(propertyChangeNumber);

    Property propertySubmitterName = createMock(Property.class);
    expect(propertySubmitterName.getKey()).andReturn("abandonerName").anyTimes();
    expect(propertySubmitterName.getValue()).andReturn("John Doe").anyTimes();
    properties.add(propertySubmitterName);

    Property propertyChangeUrl = createMock(Property.class);
    expect(propertyChangeUrl.getKey()).andReturn("formatChangeUrl").anyTimes();
    expect(propertyChangeUrl.getValue()).andReturn("HtTp://ExAmPlE.OrG/ChAnGe").anyTimes();
    properties.add(propertyChangeUrl);

    its.addComment(
        "176",
        "Change 4711 abandoned by John Doe:\n"
            + "Test-Change-Subject\n"
            + "\n"
            + "Reason:\n"
            + "Test-Reason\n"
            + "\n"
            + "HtTp://ExAmPlE.OrG/ChAnGe");
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
    expect(propertyChangeNumber.getKey()).andReturn("changeNumber").anyTimes();
    expect(propertyChangeNumber.getValue()).andReturn("4711").anyTimes();
    properties.add(propertyChangeNumber);

    Property propertySubmitterName = createMock(Property.class);
    expect(propertySubmitterName.getKey()).andReturn("restorerName").anyTimes();
    expect(propertySubmitterName.getValue()).andReturn("John Doe").anyTimes();
    properties.add(propertySubmitterName);

    Property propertyChangeUrl = createMock(Property.class);
    expect(propertyChangeUrl.getKey()).andReturn("formatChangeUrl").anyTimes();
    expect(propertyChangeUrl.getValue()).andReturn("HtTp://ExAmPlE.OrG/ChAnGe").anyTimes();
    properties.add(propertyChangeUrl);

    its.addComment(
        "176",
        "Change 4711 restored by John Doe:\n"
            + "Test-Change-Subject\n"
            + "\n"
            + "Reason:\n"
            + "Test-Reason\n"
            + "\n"
            + "HtTp://ExAmPlE.OrG/ChAnGe");
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
    expect(propertyChangeNumber.getKey()).andReturn("changeNumber").anyTimes();
    expect(propertyChangeNumber.getValue()).andReturn("4711").anyTimes();
    properties.add(propertyChangeNumber);

    Property propertySubmitterName = createMock(Property.class);
    expect(propertySubmitterName.getKey()).andReturn("uploaderName").anyTimes();
    expect(propertySubmitterName.getValue()).andReturn("John Doe").anyTimes();
    properties.add(propertySubmitterName);

    Property propertyChangeUrl = createMock(Property.class);
    expect(propertyChangeUrl.getKey()).andReturn("formatChangeUrl").anyTimes();
    expect(propertyChangeUrl.getValue()).andReturn("HtTp://ExAmPlE.OrG/ChAnGe").anyTimes();
    properties.add(propertyChangeUrl);

    its.addComment(
        "176",
        "Change 4711 had a related patch set uploaded by "
            + "John Doe:\n"
            + "Test-Change-Subject\n"
            + "\n"
            + "HtTp://ExAmPlE.OrG/ChAnGe");
    replayMocks();

    Action action = injector.getInstance(AddStandardComment.class);
    action.execute("176", actionRequest, properties);
  }

  @Override
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
