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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gerrit.server.config.FactoryModule;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.inject.Guice;
import com.google.inject.Injector;

import com.googlesource.gerrit.plugins.hooks.its.ItsConfig;
import com.googlesource.gerrit.plugins.hooks.testutil.LoggingMockingTestCase;
import com.googlesource.gerrit.plugins.hooks.util.PropertyExtractor;

public class ActionControllerTest extends LoggingMockingTestCase {
  private Injector injector;

  private PropertyExtractor propertyExtractor;
  private RuleBase ruleBase;
  private ActionExecutor actionExecutor;

  public void testNoPropertySets() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Set<Set<Property>> propertySets = Collections.emptySet();
    expect(propertyExtractor.extractFrom(event)).andReturn(propertySets)
        .anyTimes();

    replayMocks();

    actionController.onChangeEvent(event);
  }

  public void testNoActions() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Set<Set<Property>> propertySets = Sets.newHashSet();
    Set<Property> propertySet = Collections.emptySet();
    propertySets.add(propertySet);

    expect(propertyExtractor.extractFrom(event)).andReturn(propertySets)
        .anyTimes();

    Collection<ActionRequest> actions = Collections.emptySet();
    expect(ruleBase.actionRequestsFor(propertySet)).andReturn(actions).once();

    replayMocks();

    actionController.onChangeEvent(event);
  }

  public void testNoIssues() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Set<Set<Property>> propertySets = Sets.newHashSet();
    Set<Property> propertySet = Collections.emptySet();
    propertySets.add(propertySet);

    expect(propertyExtractor.extractFrom(event)).andReturn(propertySets)
        .anyTimes();

    Collection<ActionRequest> actions = Lists.newArrayListWithCapacity(1);
    ActionRequest action1 = createMock(ActionRequest.class);
    actions.add(action1);
    expect(ruleBase.actionRequestsFor(propertySet)).andReturn(actions).once();

    replayMocks();

    actionController.onChangeEvent(event);
  }

  public void testSinglePropertySetSingleActionSingleIssue() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Property propertyIssue1 = createMock(Property.class);
    expect(propertyIssue1.getKey()).andReturn("issue").anyTimes();
    expect(propertyIssue1.getValue()).andReturn("testIssue").anyTimes();

    Set<Property> propertySet = Sets.newHashSet();
    propertySet.add(propertyIssue1);

    Set<Set<Property>> propertySets = Sets.newHashSet();
    propertySets.add(propertySet);

    expect(propertyExtractor.extractFrom(event)).andReturn(propertySets)
        .anyTimes();

    Collection<ActionRequest> actionRequests =
        Lists.newArrayListWithCapacity(1);
    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    actionRequests.add(actionRequest1);
    expect(ruleBase.actionRequestsFor(propertySet)).andReturn(actionRequests)
        .once();

    actionExecutor.execute("testIssue", actionRequests, propertySet);

    replayMocks();

    actionController.onChangeEvent(event);
  }

  public void testMultiplePropertySetsMultipleActionMultipleIssue() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Property propertyIssue1 = createMock(Property.class);
    expect(propertyIssue1.getKey()).andReturn("issue").anyTimes();
    expect(propertyIssue1.getValue()).andReturn("testIssue").anyTimes();

    Property propertyIssue2 = createMock(Property.class);
    expect(propertyIssue2.getKey()).andReturn("issue").anyTimes();
    expect(propertyIssue2.getValue()).andReturn("testIssue2").anyTimes();

    Set<Property> propertySet1 = Sets.newHashSet();
    propertySet1.add(propertyIssue1);

    Set<Property> propertySet2 = Sets.newHashSet();
    propertySet2.add(propertyIssue1);
    propertySet2.add(propertyIssue2);

    Set<Set<Property>> propertySets = Sets.newHashSet();
    propertySets.add(propertySet1);
    propertySets.add(propertySet2);

    expect(propertyExtractor.extractFrom(event)).andReturn(propertySets)
        .anyTimes();

    Collection<ActionRequest> actionRequests1 =
        Lists.newArrayListWithCapacity(1);
    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    actionRequests1.add(actionRequest1);

    Collection<ActionRequest> actionRequests2 =
        Lists.newArrayListWithCapacity(2);
    ActionRequest actionRequest2 = createMock(ActionRequest.class);
    actionRequests2.add(actionRequest2);
    ActionRequest actionRequest3 = createMock(ActionRequest.class);
    actionRequests2.add(actionRequest3);

    expect(ruleBase.actionRequestsFor(propertySet1)).andReturn(actionRequests1)
        .once();
    expect(ruleBase.actionRequestsFor(propertySet2)).andReturn(actionRequests2)
        .once();

    actionExecutor.execute("testIssue", actionRequests1, propertySet1);
    actionExecutor.execute("testIssue", actionRequests2, propertySet2);
    actionExecutor.execute("testIssue2", actionRequests2, propertySet2);

    replayMocks();

    actionController.onChangeEvent(event);
  }
  private ActionController createActionController() {
    return injector.getInstance(ActionController.class);
  }

  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
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

      bind(ItsConfig.class).toInstance(new ItsConfig(null, null) {
        @Override
        public boolean isEnabled(ChangeEvent event) {
          return true;
        }
      });
    }
  }
}