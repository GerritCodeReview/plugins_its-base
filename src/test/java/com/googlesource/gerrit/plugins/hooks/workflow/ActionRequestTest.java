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

import com.google.gerrit.server.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;

import java.util.Arrays;

public class ActionRequestTest extends LoggingMockingTestCase {
  private Injector injector;

  public void testUnparsedParameterless() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action");
    assertEquals("Unparsed string does not match", "action",
        actionRequest.getUnparsed());
  }

  public void testUnparsedNull() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest(null);
    assertEquals("Unparsed string does not match", "",
        actionRequest.getUnparsed());
  }

  public void testUnparsedSingleParameter() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param");
    assertEquals("Unparsed string does not match", "action param",
        actionRequest.getUnparsed());
  }

  public void testUnparsedMultipleParameters() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param1 param2");
    assertEquals("Unparsed string does not match", "action param1 param2",
        actionRequest.getUnparsed());
  }

  public void testNameParameterless() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action");
    assertEquals("Unparsed string does not match", "action",
        actionRequest.getName());
  }

  public void testNameSingleParameter() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param");
    assertEquals("Unparsed string does not match", "action",
        actionRequest.getName());
  }

  public void testNameMultipleParameters() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param1 param2");
    assertEquals("Unparsed string does not match", "action",
        actionRequest.getName());
  }

  public void testNameNull() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest(null);
    assertEquals("Unparsed string does not match", "",
        actionRequest.getName());
  }

  public void testParameter1Parameterless() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action");
    assertEquals("Unparsed string does not match", "",
        actionRequest.getParameter(1));
  }

  public void testParameter1Null() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest(null);
    assertEquals("Unparsed string does not match", "",
        actionRequest.getParameter(1));
  }

  public void testParameter1SingleParameter() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param");
    assertEquals("Unparsed string does not match", "param",
        actionRequest.getParameter(1));
  }

  public void testParemeter1MultipleParameters() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param1 param2");
    assertEquals("Unparsed string does not match", "param1",
        actionRequest.getParameter(1));
  }

  public void testParameter3Parameterless() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action");
    assertEquals("Unparsed string does not match", "",
        actionRequest.getParameter(3));
  }

  public void testParameter3Null() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest(null);
    assertEquals("Unparsed string does not match", "",
        actionRequest.getParameter(3));
  }

  public void testParameter3SingleParameter() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param");
    assertEquals("Unparsed string does not match", "",
        actionRequest.getParameter(3));
  }

  public void testParemeter3With2Parameters() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param1 param2");
    assertEquals("Unparsed string does not match", "",
        actionRequest.getParameter(3));
  }

  public void testParemeter3With3Parameters() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param1 param2 " +
        "param3");
    assertEquals("Unparsed string does not match", "param3",
        actionRequest.getParameter(3));
  }

  public void testParemeter3With4Parameters() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param1 param2 " +
        "param3 param4");
    assertEquals("Unparsed string does not match", "param3",
        actionRequest.getParameter(3));
  }

  public void testParametersParameterless() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action");

    String[] expected = new String[0];
    assertEquals("Parameters do not match", Arrays.asList(expected),
        Arrays.asList(actionRequest.getParameters()));
  }

  public void testParametersNull() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest(null);

    String[] expected = new String[0];
    assertEquals("Parameters do not match", Arrays.asList(expected),
        Arrays.asList(actionRequest.getParameters()));
  }

  public void testParametersSingleParameter() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param");

    String[] expected = new String[] { "param" };
    assertEquals("Parameters do not match", Arrays.asList(expected),
        Arrays.asList(actionRequest.getParameters()));
  }

  public void testParameters3Parameter() {
    replayMocks();

    ActionRequest actionRequest = createActionRequest("action param1 param2 " +
        "param3");

    String[] expected = new String[] { "param1", "param2", "param3" };
    assertEquals("Parameters do not match", Arrays.asList(expected),
        Arrays.asList(actionRequest.getParameters()));
  }

  private ActionRequest createActionRequest(String specification) {
    ActionRequest.Factory factory = injector.getInstance(
        ActionRequest.Factory.class);
    return factory.create(specification);
  }

  @Override
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