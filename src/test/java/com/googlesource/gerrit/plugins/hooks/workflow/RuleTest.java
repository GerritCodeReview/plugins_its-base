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

import com.google.common.collect.Lists;
import com.google.gerrit.server.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class RuleTest extends LoggingMockingTestCase {
  private Injector injector;

  public void testGetName() {
    Rule rule = createRule("testRule");
    assertEquals("Rule name does not match", "testRule", rule.getName());
  }

  public void testActionsForUnconditionalRule() {
    Collection<Property> properties = Collections.emptySet();

    Rule rule = createRule("testRule");

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    rule.addActionRequest(actionRequest1);

    replayMocks();

    Collection<ActionRequest> actual = rule.actionRequestsFor(properties);

    Collection<ActionRequest> expected = Lists.newArrayListWithCapacity(1);
    expected.add(actionRequest1);
    assertEquals("Matched actionRequests do not match", expected, actual);
  }

  public void testActionRequestsForConditionalRuleEmptyProperties() {
    Collection<Property> properties = Collections.emptySet();

    Rule rule = createRule("testRule");

    Condition condition1 = createMock(Condition.class);
    expect(condition1.isMetBy(properties)).andReturn(false).anyTimes();
    rule.addCondition(condition1);

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    rule.addActionRequest(actionRequest1);

    replayMocks();

    Collection<ActionRequest> actual = rule.actionRequestsFor(properties);

    List<ActionRequest> expected = Collections.emptyList();
    assertEquals("Matched actionRequests do not match", expected, actual);
  }

  public void testActionRequestsForConditionalRules() {
    Collection<Property> properties = Collections.emptySet();

    Rule rule = createRule("testRule");

    Condition condition1 = createMock(Condition.class);
    expect(condition1.isMetBy(properties)).andReturn(true).anyTimes();
    rule.addCondition(condition1);

    Condition condition2 = createMock(Condition.class);
    expect(condition2.isMetBy(properties)).andReturn(false).anyTimes();
    rule.addCondition(condition2);

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    rule.addActionRequest(actionRequest1);

    replayMocks();

    Collection<ActionRequest> actual = rule.actionRequestsFor(properties);

    List<ActionRequest> expected = Collections.emptyList();
    assertEquals("Matched actionRequests do not match", expected, actual);
  }

  public void testActionRequestsForMultipleActionRequests() {
    Collection<Property> properties = Collections.emptySet();

    Rule rule = createRule("testRule");

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    rule.addActionRequest(actionRequest1);

    ActionRequest actionRequest2 = createMock(ActionRequest.class);
    rule.addActionRequest(actionRequest2);

    replayMocks();

    Collection<ActionRequest> actual = rule.actionRequestsFor(properties);

    List<ActionRequest> expected = Lists.newArrayListWithCapacity(1);
    expected.add(actionRequest1);
    expected.add(actionRequest2);
    assertEquals("Matched actionRequests do not match", expected, actual);
  }

  private Rule createRule(String name) {
    Rule.Factory factory = injector.getInstance(Rule.Factory.class);
    return factory.create(name);
  }

  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      factory(Rule.Factory.class);
    }
  }
}