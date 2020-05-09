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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.entities.Project;
import com.google.gerrit.extensions.annotations.Exports;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.extensions.registration.DynamicMap;
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
  private static final String CUSTOM_ACTION_NAME = "custom-action-name";

  private Injector injector;

  private ItsFacade its;
  private ItsFacadeFactory itsFacadeFactory;
  private AddComment.Factory addCommentFactory;
  private AddStandardComment.Factory addStandardCommentFactory;
  private AddSoyComment.Factory addSoyCommentFactory;
  private LogEvent.Factory logEventFactory;
  private AddPropertyToField.Factory addPropertyToFieldFactory;
  private CreateVersionFromProperty.Factory createVersionFromPropertyFactory;
  private CustomAction customAction;

  private Map<String, String> properties =
      ImmutableMap.of("issue", "4711", "project", "testProject");
  private Map<String, String> projectProperties =
      ImmutableMap.<String, String>builder()
          .putAll(properties)
          .put("its-project", "itsTestProject")
          .build();

  public void testExecuteItem() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getName()).thenReturn("unparsed");
    when(actionRequest.getUnparsed()).thenReturn("unparsed action 1");
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    verify(its).performAction("4711", "unparsed action 1");
  }

  public void testExecuteItemException() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getName()).thenReturn("unparsed");
    when(actionRequest.getUnparsed()).thenReturn("unparsed action 1");
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    doThrow(new IOException("injected exception 1"))
        .when(its)
        .performAction("4711", "unparsed action 1");

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    assertLogThrowableMessageContains("injected exception 1");
  }

  public void testExecuteIterable() throws IOException {
    ActionRequest actionRequest1 = mock(ActionRequest.class);
    when(actionRequest1.getName()).thenReturn("unparsed");
    when(actionRequest1.getUnparsed()).thenReturn("unparsed action 1");

    ActionRequest actionRequest2 = mock(ActionRequest.class);
    when(actionRequest2.getName()).thenReturn("unparsed");
    when(actionRequest2.getUnparsed()).thenReturn("unparsed action 2");
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest1, actionRequest2);

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    verify(its).performAction("4711", "unparsed action 1");
    verify(its).performAction("4711", "unparsed action 2");
  }

  public void testExecuteIterableExceptions() throws IOException {
    ActionRequest actionRequest1 = mock(ActionRequest.class);
    when(actionRequest1.getName()).thenReturn("unparsed");
    when(actionRequest1.getUnparsed()).thenReturn("unparsed action 1");

    ActionRequest actionRequest2 = mock(ActionRequest.class);
    when(actionRequest2.getName()).thenReturn("unparsed");
    when(actionRequest2.getUnparsed()).thenReturn("unparsed action 2");

    ActionRequest actionRequest3 = mock(ActionRequest.class);
    when(actionRequest3.getName()).thenReturn("unparsed");
    when(actionRequest3.getUnparsed()).thenReturn("unparsed action 3");
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    Set<ActionRequest> actionRequests =
        ImmutableSet.of(actionRequest1, actionRequest2, actionRequest3);

    doThrow(new IOException("injected exception 1"))
        .when(its)
        .performAction("4711", "unparsed action 1");
    doThrow(new IOException("injected exception 3"))
        .when(its)
        .performAction("4711", "unparsed action 3");

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    assertLogThrowableMessageContains("injected exception 1");
    assertLogThrowableMessageContains("injected exception 3");

    verify(its).performAction("4711", "unparsed action 2");
  }

  public void testAddCommentDelegation() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getName()).thenReturn("add-comment");

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    AddComment addComment = mock(AddComment.class);
    when(addCommentFactory.create()).thenReturn(addComment);
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    verify(addComment).execute(its, "4711", actionRequest, properties);
  }

  public void testAddSoyCommentDelegation() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getName()).thenReturn("add-soy-comment");

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    AddSoyComment addSoyComment = mock(AddSoyComment.class);
    when(addSoyCommentFactory.create()).thenReturn(addSoyComment);
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    verify(addSoyComment).execute(its, "4711", actionRequest, properties);
  }

  public void testAddStandardCommentDelegation() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getName()).thenReturn("add-standard-comment");

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    AddStandardComment addStandardComment = mock(AddStandardComment.class);
    when(addStandardCommentFactory.create()).thenReturn(addStandardComment);
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    verify(addStandardComment).execute(its, "4711", actionRequest, properties);
  }

  public void testLogEventDelegation() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getName()).thenReturn("log-event");

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    LogEvent logEvent = mock(LogEvent.class);
    when(logEventFactory.create()).thenReturn(logEvent);
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    verify(logEvent).execute(its, "4711", actionRequest, properties);
  }

  public void testCreateVersionFromPropertyDelegation() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getName()).thenReturn("create-version-from-property");

    CreateVersionFromProperty createVersionFromProperty = mock(CreateVersionFromProperty.class);
    when(createVersionFromPropertyFactory.create()).thenReturn(createVersionFromProperty);
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnProject(Collections.singleton(actionRequest), projectProperties);

    verify(createVersionFromProperty)
        .execute(its, "itsTestProject", actionRequest, projectProperties);
  }

  public void testAddPropertyToFieldDelegation() throws IOException {
    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getName()).thenReturn("add-property-to-field");

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    AddPropertyToField addPropertyToField = mock(AddPropertyToField.class);
    when(addPropertyToFieldFactory.create()).thenReturn(addPropertyToField);
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    verify(addPropertyToField).execute(its, "4711", actionRequest, properties);
  }

  public void testExecuteIssueCustomAction() throws IOException {
    when(customAction.getType()).thenReturn(ActionType.ISSUE);

    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getName()).thenReturn(CUSTOM_ACTION_NAME);
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnIssue(actionRequests, properties);

    verify(customAction).execute(its, "4711", actionRequest, properties);
  }

  public void testExecuteProjectCustomAction() throws IOException {
    when(customAction.getType()).thenReturn(ActionType.PROJECT);

    ActionRequest actionRequest = mock(ActionRequest.class);
    when(actionRequest.getName()).thenReturn(CUSTOM_ACTION_NAME);
    when(itsFacadeFactory.getFacade(Project.nameKey(properties.get("project")))).thenReturn(its);

    Set<ActionRequest> actionRequests = ImmutableSet.of(actionRequest);

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.executeOnProject(actionRequests, projectProperties);

    verify(customAction).execute(its, "itsTestProject", actionRequest, projectProperties);
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
      its = mock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(its);

      addCommentFactory = mock(AddComment.Factory.class);
      bind(AddComment.Factory.class).toInstance(addCommentFactory);

      addSoyCommentFactory = mock(AddSoyComment.Factory.class);
      bind(AddSoyComment.Factory.class).toInstance(addSoyCommentFactory);

      addStandardCommentFactory = mock(AddStandardComment.Factory.class);
      bind(AddStandardComment.Factory.class).toInstance(addStandardCommentFactory);

      logEventFactory = mock(LogEvent.Factory.class);
      bind(LogEvent.Factory.class).toInstance(logEventFactory);

      itsFacadeFactory = mock(ItsFacadeFactory.class);
      bind(ItsFacadeFactory.class).toInstance(itsFacadeFactory);

      addPropertyToFieldFactory = mock(AddPropertyToField.Factory.class);
      bind(AddPropertyToField.Factory.class).toInstance(addPropertyToFieldFactory);

      createVersionFromPropertyFactory = mock(CreateVersionFromProperty.Factory.class);
      bind(CreateVersionFromProperty.Factory.class).toInstance(createVersionFromPropertyFactory);

      DynamicMap.mapOf(binder(), CustomAction.class);
      customAction = mock(CustomAction.class);

      bind(CustomAction.class)
          .annotatedWith(Exports.named(CUSTOM_ACTION_NAME))
          .toInstance(customAction);
    }
  }
}
