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

import java.io.IOException;

import com.google.gerrit.server.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;

public class ActionRequestTest extends LoggingMockingTestCase {
  private Injector injector;

  public void testUnparsedParameterless() throws IOException {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action");
    assertEquals("Unparsed string does not match", "action",
        actionRequest.getUnparsed());
  }

  public void testUnparsedNull() throws IOException {
    replayMocks();

    ActionRequest actionRequest = createActionRequest(null);
    assertEquals("Unparsed string does not match", "",
        actionRequest.getUnparsed());
  }

  public void testUnparsedSingleParameter() throws IOException {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param");
    assertEquals("Unparsed string does not match", "action param",
        actionRequest.getUnparsed());
  }

  public void testUnparsedMultipleParameters() throws IOException {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param1 param2");
    assertEquals("Unparsed string does not match", "action param1 param2",
        actionRequest.getUnparsed());
  }

  public void testNameParameterless() throws IOException {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action");
    assertEquals("Unparsed string does not match", "action",
        actionRequest.getName());
  }

  public void testNameSingleParameter() throws IOException {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param");
    assertEquals("Unparsed string does not match", "action",
        actionRequest.getName());
  }

  public void testNameMultipleParameters() throws IOException {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param1 param2");
    assertEquals("Unparsed string does not match", "action",
        actionRequest.getName());
  }

  public void testNameNull() throws IOException {
    replayMocks();

    ActionRequest actionRequest = createActionRequest(null);
    assertEquals("Unparsed string does not match", "",
        actionRequest.getName());
  }

  private ActionRequest createActionRequest(String specification) {
    ActionRequest.Factory factory = injector.getInstance(
        ActionRequest.Factory.class);
    return factory.create(specification);
  }

  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      factory(ActionRequest.Factory.class);
    }
  }
}