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

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Level;

import com.google.common.collect.Sets;
import com.google.gerrit.server.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.hooks.workflow.ActionRequest;
import com.googlesource.gerrit.plugins.hooks.workflow.Property;

public class LogEventTest extends LoggingMockingTestCase {
  private Injector injector;

  public void testEmpty() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("");

    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, new HashSet<Property>());
  }

  public void testLevelDefault() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("");

    Set<Property> properties = Sets.newHashSet();
    properties.add(new PropertyMock("KeyA", "ValueA", "PropertyA"));
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("PropertyA", Level.INFO);
  }

  public void testLevelError() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("error");

    Set<Property> properties = Sets.newHashSet();
    properties.add(new PropertyMock("KeyA", "ValueA", "PropertyA"));
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("PropertyA", Level.ERROR);
  }

  public void testLevelWarn() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("warn");

    Set<Property> properties = Sets.newHashSet();
    properties.add(new PropertyMock("KeyA", "ValueA", "PropertyA"));
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("PropertyA", Level.WARN);
  }

  public void testLevelInfo() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("info");

    Set<Property> properties = Sets.newHashSet();
    properties.add(new PropertyMock("KeyA", "ValueA", "PropertyA"));
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("PropertyA", Level.INFO);
  }

  public void testLevelDebug() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("debug");

    Set<Property> properties = Sets.newHashSet();
    properties.add(new PropertyMock("KeyA", "ValueA", "PropertyA"));
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("PropertyA", Level.DEBUG);
  }

  public void testMultipleProperties() throws IOException {
    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(actionRequest.getParameter(1)).andReturn("info");


    Set<Property> properties = Sets.newHashSet();
    properties.add(new PropertyMock("KeyA", "ValueA", "PropertyA"));
    properties.add(new PropertyMock("KeyB", "ValueB", "PropertyB"));
    properties.add(new PropertyMock("KeyC", "ValueC", "PropertyC"));
    replayMocks();

    LogEvent logEvent = createLogEvent();
    logEvent.execute("4711", actionRequest, properties);

    assertLogMessageContains("PropertyA", Level.INFO);
    assertLogMessageContains("PropertyB", Level.INFO);
    assertLogMessageContains("PropertyC", Level.INFO);
  }

  private LogEvent createLogEvent() {
    return injector.getInstance(LogEvent.class);
  }

  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
    }
  }

  private class PropertyMock extends Property {
    private final String toString;

    public PropertyMock(String key, String value, String toString) {
      super(key, value);
      this.toString = toString;
    }

    public String toString() {
      return toString;
    }
  }
}