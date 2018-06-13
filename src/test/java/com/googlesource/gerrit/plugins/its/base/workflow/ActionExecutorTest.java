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
package com.googlesource.gerrit.plugins.its.base.workflow;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.reviewdb.client.Project;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacadeFactory;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ActionExecutorTest extends LoggingMockingTestCase {
  private Injector injector;

  private ItsFacade its;
  private ItsFacadeFactory itsFacadeFactory;
  private AddComment.Factory addCommentFactory;
  private AddStandardComment.Factory addStandardCommentFactory;
  private AddSoyComment.Factory addSoyCommentFactory;
  private LogEvent.Factory logEventFactory;
  private CreateVersionFromProperty.Factory createVersionFromPropertyFactory;
  private MarkPropertyAsReleasedVersion.Factory markPropertyAsReleasedVersionFactory;

  private Map<String, String> properties =
      ImmutableMap.of("issue", "4711", "project", "testProject");
  private Map<String, String> projectProperties =
      ImmutableMap.<String, String>builder()
          .putAll(properties)
          .put("its-project", "itsTestProject")
          .build();

  public void testExecuteItem() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getName()).andReturn("unparsed");
    expect(actionRequest.getUnparsed()).andReturn("unparsed action 1");
    expect(itsFacadeFactory.getFacade(new Project.NameKey(properties.get("project"))))
        .andReturn(its);

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    its.performAction("4711", "unparsed action 1");

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);
  }

  public void testExecuteItemException() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getName()).andReturn("unparsed");
    expect(actionRequest.getUnparsed()).andReturn("unparsed action 1");
    expect(itsFacadeFactory.getFacade(new Project.NameKey(properties.get("project"))))
        .andReturn(its);

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    its.performAction("4711", "unparsed action 1");
    expectLastCall().andThrow(new IOException("injected exception 1"));

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    assertLogThrowableMessageContains("injected exception 1");
  }

  public void testExecuteIterable() throws IOException {
    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    expect(actionRequest1.getName()).andReturn("unparsed");
    expect(actionRequest1.getUnparsed()).andReturn("unparsed action 1");

    ActionRequest actionRequest2 = createMock(ActionRequest.class);
    expect(actionRequest2.getName()).andReturn("unparsed");
    expect(actionRequest2.getUnparsed()).andReturn("unparsed action 2");
    expect(itsFacadeFactory.getFacade(new Project.NameKey(properties.get("project"))))
        .andReturn(its)
        .anyTimes();

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest1, actionRequest2);

    its.performAction("4711", "unparsed action 1");
    its.performAction("4711", "unparsed action 2");

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);
  }

  public void testExecuteIterableExceptions() throws IOException {
    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    expect(actionRequest1.getName()).andReturn("unparsed");
    expect(actionRequest1.getUnparsed()).andReturn("unparsed action 1");

    ActionRequest actionRequest2 = createMock(ActionRequest.class);
    expect(actionRequest2.getName()).andReturn("unparsed");
    expect(actionRequest2.getUnparsed()).andReturn("unparsed action 2");

    ActionRequest actionRequest3 = createMock(ActionRequest.class);
    expect(actionRequest3.getName()).andReturn("unparsed");
    expect(actionRequest3.getUnparsed()).andReturn("unparsed action 3");
    expect(itsFacadeFactory.getFacade(new Project.NameKey(properties.get("project"))))
        .andReturn(its)
        .anyTimes();

    Set<ActionRequest> actionRequests =
        ImmutableSet.of(actionRequest1, actionRequest2, actionRequest3);

    its.performAction("4711", "unparsed action 1");
    expectLastCall().andThrow(new IOException("injected exception 1"));
    its.performAction("4711", "unparsed action 2");
    its.performAction("4711", "unparsed action 3");
    expectLastCall().andThrow(new IOException("injected exception 3"));

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    assertLogThrowableMessageContains("injected exception 1");
    assertLogThrowableMessageContains("injected exception 3");
  }

  public void testAddCommentDelegation() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getName()).andReturn("add-comment");

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    AddComment addComment = createMock(AddComment.class);
    expect(addCommentFactory.create()).andReturn(addComment);
    expect(itsFacadeFactory.getFacade(new Project.NameKey(properties.get("project"))))
        .andReturn(its);

    addComment.execute(its, "4711", actionRequest, properties);

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);
  }

  public void testAddSoyCommentDelegation() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getName()).andReturn("add-soy-comment");

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    AddSoyComment addSoyComment = createMock(AddSoyComment.class);
    expect(addSoyCommentFactory.create()).andReturn(addSoyComment);
    expect(itsFacadeFactory.getFacade(new Project.NameKey(properties.get("project"))))
        .andReturn(its);

    addSoyComment.execute(its, "4711", actionRequest, properties);

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);
  }

  public void testAddStandardCommentDelegation() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getName()).andReturn("add-standard-comment");

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    AddStandardComment addStandardComment = createMock(AddStandardComment.class);
    expect(addStandardCommentFactory.create()).andReturn(addStandardComment);
    expect(itsFacadeFactory.getFacade(new Project.NameKey(properties.get("project"))))
        .andReturn(its);

    addStandardComment.execute(its, "4711", actionRequest, properties);

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);
  }

  public void testLogEventDelegation() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getName()).andReturn("log-event");

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    LogEvent logEvent = createMock(LogEvent.class);
    expect(logEventFactory.create()).andReturn(logEvent);
    expect(itsFacadeFactory.getFacade(new Project.NameKey(properties.get("project"))))
        .andReturn(its);

    logEvent.execute(its, "4711", actionRequest, properties);

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);
  }

  public void testCreateVersionFromPropertyDelegation() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getName()).andReturn("create-version-from-property");

    CreateVersionFromProperty createVersionFromProperty =
        createMock(CreateVersionFromProperty.class);
    expect(createVersionFromPropertyFactory.create()).andReturn(createVersionFromProperty);
    expect(itsFacadeFactory.getFacade(new Project.NameKey(properties.get("project"))))
        .andReturn(its);

    createVersionFromProperty.execute(its, "itsTestProject", actionRequest, projectProperties);

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnProject(Collections.singleton(actionRequest), projectProperties);
  }

  public void testMarkPropertyAsReleasedVersionDelegation() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getName()).andReturn("mark-property-as-released-version");

    MarkPropertyAsReleasedVersion markPropertyAsReleasedVersion =
        createMock(MarkPropertyAsReleasedVersion.class);
    expect(markPropertyAsReleasedVersionFactory.create()).andReturn(markPropertyAsReleasedVersion);
    expect(itsFacadeFactory.getFacade(new Project.NameKey(projectProperties.get("project"))))
        .andReturn(its);

    markPropertyAsReleasedVersion.execute(its, "itsTestProject", actionRequest, projectProperties);

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnProject(Collections.singleton(actionRequest), projectProperties);
  }

  private ActionExecutor createActionExecutor() {
    return injector.getInstance(ActionExecutor.class);
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

      addCommentFactory = createMock(AddComment.Factory.class);
      bind(AddComment.Factory.class).toInstance(addCommentFactory);

      addSoyCommentFactory = createMock(AddSoyComment.Factory.class);
      bind(AddSoyComment.Factory.class).toInstance(addSoyCommentFactory);

      addStandardCommentFactory = createMock(AddStandardComment.Factory.class);
      bind(AddStandardComment.Factory.class).toInstance(addStandardCommentFactory);

      logEventFactory = createMock(LogEvent.Factory.class);
      bind(LogEvent.Factory.class).toInstance(logEventFactory);

      itsFacadeFactory = createMock(ItsFacadeFactory.class);
      bind(ItsFacadeFactory.class).toInstance(itsFacadeFactory);

      createVersionFromPropertyFactory = createMock(CreateVersionFromProperty.Factory.class);
      bind(CreateVersionFromProperty.Factory.class).toInstance(createVersionFromPropertyFactory);

      markPropertyAsReleasedVersionFactory =
          createMock(MarkPropertyAsReleasedVersion.Factory.class);
      bind(MarkPropertyAsReleasedVersion.Factory.class)
          .toInstance(markPropertyAsReleasedVersionFactory);
    }
  }
}
