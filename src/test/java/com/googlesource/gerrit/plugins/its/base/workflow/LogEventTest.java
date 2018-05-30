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
package com.googlesource.gerrit.plugins.its.base.workflow;

import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableMap;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import java.io.IOException;
import java.util.Map;
import org.apache.log4j.Level;

public class LogEventTest extends LoggingMockingTestCase {
  private Injector injector;

  public void testNull() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn(null);

    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, ImmutableMap.of());
  }

  public void testEmpty() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("");

    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, ImmutableMap.of());
  }

  public void testLevelDefault() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("");

    Map<String, String> properties = ImmutableMap.of("KeyA", "ValueA");
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("KeyA = ValueA", Level.INFO);
  }

  public void testLevelError() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("error");

    Map<String, String> properties = ImmutableMap.of("KeyA", "ValueA");
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("KeyA = ValueA", Level.ERROR);
  }

  public void testLevelWarn() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("warn");

    Map<String, String> properties = ImmutableMap.of("KeyA", "ValueA");
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("KeyA = ValueA", Level.WARN);
  }

  public void testLevelInfo() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("info");

    Map<String, String> properties = ImmutableMap.of("KeyA", "ValueA");
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("KeyA = ValueA", Level.INFO);
  }

  public void testLevelDebug() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("debug");

    Map<String, String> properties = ImmutableMap.of("KeyA", "ValueA");
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("KeyA = ValueA", Level.DEBUG);
  }

  public void testMultipleProperties() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("info");

    Map<String, String> properties =
        ImmutableMap.<String, String>builder()
            .put("KeyA", "ValueA")
            .put("KeyB", "ValueB")
            .put("KeyC", "ValueC")
            .build();
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("KeyA = ValueA", Level.INFO);
    assertLogMessageContains("KeyB = ValueB", Level.INFO);
    assertLogMessageContains("KeyC = ValueC", Level.INFO);
  }

  private LogEvent createLogEvent() {
    return injector.getInstance(LogEvent.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {}
  }
}
