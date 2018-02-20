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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.RefEvent;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsConfig;
import com.googlesource.gerrit.plugins.its.base.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.its.base.util.PropertyExtractor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ActionControllerTest extends LoggingMockingTestCase {
  private Injector injector;

  private PropertyExtractor propertyExtractor;
  private RuleBase ruleBase;
  private ActionExecutor actionExecutor;
  private ItsConfig itsConfig;

  public void testNoPropertySets() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Set<Map<String, String>> propertySets = new HashSet<>();
    expect(propertyExtractor.extractFrom(event)).andReturn(propertySets).anyTimes();

    replayMocks();

    actionController.onEvent(event);
  }

  public void testNoActionsOrNotIssues() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Set<Map<String, String>> propertySets = new HashSet<>();
    Map<String, String> properties = ImmutableMap.of("fake", "property");
    propertySets.add(properties);

    expect(propertyExtractor.extractFrom(event)).andReturn(propertySets).anyTimes();

    // When no issues are found in the commit message, the list of actions is empty
    // as there are no matchs with an empty map of properties.
    Collection<ActionRequest> actions = Collections.emptySet();
    expect(ruleBase.actionRequestsFor(properties)).andReturn(actions).once();

    replayMocks();

    actionController.onEvent(event);
  }

  public void testSinglePropertyMapSingleActionSingleIssue() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Map<String, String> properties = ImmutableMap.of("issue", "testIssue");

    Set<Map<String, String>> propertySets = ImmutableSet.of(properties);

    expect(propertyExtractor.extractFrom(event)).andReturn(propertySets).anyTimes();

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    Collection<ActionRequest> actionRequests = ImmutableList.of(actionRequest1);
    expect(ruleBase.actionRequestsFor(properties)).andReturn(actionRequests).once();

    actionExecutor.execute(actionRequests, properties);

    replayMocks();

    actionController.onEvent(event);
  }

  public void testMultiplePropertyMapsMultipleActionMultipleIssue() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Map<String, String> properties1 = ImmutableMap.of("issue", "testIssue");
    Map<String, String> properties2 = ImmutableMap.of("issue", "testIssue2");

    Set<Map<String, String>> propertySets = ImmutableSet.of(properties1, properties2);

    expect(propertyExtractor.extractFrom(event)).andReturn(propertySets).anyTimes();

    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    Collection<ActionRequest> actionRequests1 = ImmutableList.of(actionRequest1);

    ActionRequest actionRequest2 = createMock(ActionRequest.class);
    ActionRequest actionRequest3 = createMock(ActionRequest.class);
    Collection<ActionRequest> actionRequests2 = ImmutableList.of(actionRequest2, actionRequest3);

    expect(ruleBase.actionRequestsFor(properties1)).andReturn(actionRequests1).once();
    expect(ruleBase.actionRequestsFor(properties2)).andReturn(actionRequests2).once();

    actionExecutor.execute(actionRequests1, properties1);
    actionExecutor.execute(actionRequests2, properties2);

    replayMocks();

    actionController.onEvent(event);
  }

  private ActionController createActionController() {
    return injector.getInstance(ActionController.class);
  }

  private void setupCommonMocks() {
    expect(itsConfig.isEnabled(anyObject(RefEvent.class))).andReturn(true).anyTimes();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());

    setupCommonMocks();
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      propertyExtractor = createMock(PropertyExtractor.class);
      bind(PropertyExtractor.class).toInstance(propertyExtractor);

      ruleBase = createMock(RuleBase.class);
      bind(RuleBase.class).toInstance(ruleBase);

      actionExecutor = createMock(ActionExecutor.class);
      bind(ActionExecutor.class).toInstance(actionExecutor);

      itsConfig = createMock(ItsConfig.class);
      bind(ItsConfig.class).toInstance(itsConfig);
    }
  }
}
