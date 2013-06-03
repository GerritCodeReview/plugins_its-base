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
package com.googlesource.gerrit.plugins.hooks.workflow;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;

import com.google.common.collect.Sets;
import com.google.gerrit.server.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.hooks.its.ItsFacade;
import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;

public class ActionExecutorTest extends LoggingMockingTestCase {
  private Injector injector;

  private ItsFacade its;

  public void testExecuteItem() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getUnparsed()).andReturn("unparsed action 1");

    its.performAction("4711", "unparsed action 1");

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.execute("4711", actionRequest);
  }

  public void testExecuteItemException() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getUnparsed()).andReturn("unparsed action 1");

    its.performAction("4711", "unparsed action 1");
    expectLastCall().andThrow(new IOException("injected exception 1"));

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.execute("4711", actionRequest);

    assertLogThrowableMessageContains("injected exception 1");
  }

  public void testExecuteIterable() throws IOException {
    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    expect(actionRequest1.getUnparsed()).andReturn("unparsed action 1");

    ActionRequest actionRequest2 = createMock(ActionRequest.class);
    expect(actionRequest2.getUnparsed()).andReturn("unparsed action 2");

    its.performAction("4711", "unparsed action 1");
    its.performAction("4711", "unparsed action 2");

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.execute("4711", Sets.newHashSet(
        actionRequest1, actionRequest2));
  }

  public void testExecuteIterableExceptions() throws IOException {
    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    expect(actionRequest1.getUnparsed()).andReturn("unparsed action 1");

    ActionRequest actionRequest2 = createMock(ActionRequest.class);
    expect(actionRequest2.getUnparsed()).andReturn("unparsed action 2");

    ActionRequest actionRequest3 = createMock(ActionRequest.class);
    expect(actionRequest3.getUnparsed()).andReturn("unparsed action 3");

    its.performAction("4711", "unparsed action 1");
    expectLastCall().andThrow(new IOException("injected exception 1"));
    its.performAction("4711", "unparsed action 2");
    its.performAction("4711", "unparsed action 3");
    expectLastCall().andThrow(new IOException("injected exception 3"));

    replayMocks();

    ActionExecutor actionExecutor = createActionExecutor();
    actionExecutor.execute("4711", Sets.newHashSet(
        actionRequest1, actionRequest2, actionRequest3));

    assertLogThrowableMessageContains("injected exception 1");
    assertLogThrowableMessageContains("injected exception 3");
  }

  private ActionExecutor createActionExecutor() {
    return injector.getInstance(ActionExecutor.class);
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