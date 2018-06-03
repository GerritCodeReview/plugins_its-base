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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
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

    RefEventProperties refEventProperties =
        new RefEventProperties(Collections.emptySet(), Collections.emptySet());
    expect(propertyExtractor.extractFrom(event)).andReturn(refEventProperties).anyTimes();
    expect(ruleBase.actionRequestsFor(Collections.emptySet()))
        .andReturn(Collections.emptySet())
        .once();

    replayMocks();

    actionController.onEvent(event);
  }

  public void testNoActions() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Set<Set<Property>> propertySets = Sets.newHashSet();
    Set<Property> propertySet = Collections.emptySet();
    propertySets.add(propertySet);

    expect(propertyExtractor.extractFrom(event))
        .andReturn(new RefEventProperties(Collections.emptySet(), propertySets))
        .anyTimes();

    Collection<ActionRequest> actions = Collections.emptySet();
    expect(ruleBase.actionRequestsFor(propertySet)).andReturn(actions).times(2);

    replayMocks();

    actionController.onEvent(event);
  }

  public void testNoIssues() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Set<Set<Property>> propertySets = Sets.newHashSet();
    Set<Property> propertySet = Collections.emptySet();
    propertySets.add(propertySet);

    Property propertyProject = createMock(Property.class);
    expect(propertyProject.getKey()).andReturn("its-project").anyTimes();
    expect(propertyProject.getValue()).andReturn("testProject").anyTimes();
    Set<Property> propertyProjectSet = Sets.newHashSet(propertyProject);

    expect(propertyExtractor.extractFrom(event))
        .andReturn(new RefEventProperties(propertyProjectSet, propertySets))
        .anyTimes();

    Collection<ActionRequest> actions = Lists.newArrayListWithCapacity(1);
    ActionRequest action1 = createMock(ActionRequest.class);
    actions.add(action1);
    expect(ruleBase.actionRequestsFor(propertySet)).andReturn(actions).times(1);
    expect(ruleBase.actionRequestsFor(propertyProjectSet)).andReturn(actions).times(1);
    actionExecutor.executeOnProject("testProject", actions, propertyProjectSet);

    replayMocks();

    actionController.onEvent(event);
  }

  public void testSinglePropertySetSingleActionSingleIssue() {
    ActionController actionController = createActionController();

    ChangeEvent event = createMock(ChangeEvent.class);

    Property propertyProject = createMock(Property.class);
    expect(propertyProject.getKey()).andReturn("its-project").anyTimes();
    expect(propertyProject.getValue()).andReturn("testProject").anyTimes();
    Set<Property> propertyProjectSet = Sets.newHashSet(propertyProject);

    Property issuePropertyIssue1 = createMock(Property.class);
    expect(issuePropertyIssue1.getKey()).andReturn("issue").anyTimes();
    expect(issuePropertyIssue1.getValue()).andReturn("testIssue").anyTimes();

    Set<Property> issuePropertySet = Sets.newHashSet();
    issuePropertySet.add(issuePropertyIssue1);

    Set<Set<Property>> issuePropertySets = Sets.newHashSet();
    issuePropertySets.add(issuePropertySet);

    expect(propertyExtractor.extractFrom(event))
        .andReturn(new RefEventProperties(propertyProjectSet, issuePropertySets))
        .anyTimes();

    Collection<ActionRequest> actionRequests = Lists.newArrayListWithCapacity(1);
    ActionRequest actionRequest1 = createMock(ActionRequest.class);
    actionRequests.add(actionRequest1);
    expect(ruleBase.actionRequestsFor(issuePropertySet)).andReturn(actionRequests).once();
    expect(ruleBase.actionRequestsFor(propertyProjectSet)).andReturn(actionRequests).once();

    actionExecutor.executeOnIssue("testIssue", actionRequests, issuePropertySet);
    actionExecutor.executeOnProject("testProject", actionRequests, propertyProjectSet);

    replayMocks();

    actionController.onEvent(event);
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

    Property propertyProject = createMock(Property.class);
    expect(propertyProject.getKey()).andReturn("its-project").anyTimes();
    expect(propertyProject.getValue()).andReturn("testProject").anyTimes();

    Set<Property> issuePropertySet1 = Sets.newHashSet();
    issuePropertySet1.add(propertyIssue1);

    Set<Property> issuePropertySet2 = Sets.newHashSet();
    issuePropertySet2.add(propertyIssue1);
    issuePropertySet2.add(propertyIssue2);

    Set<Set<Property>> propertySets = Sets.newHashSet();
    propertySets.add(issuePropertySet1);
    propertySets.add(issuePropertySet2);

    Set<Property> projectPropertySet = Sets.newHashSet(propertyProject);

    expect(propertyExtractor.extractFrom(event))
        .andReturn(new RefEventProperties(projectPropertySet, propertySets))
        .anyTimes();

    Collection<ActionRequest> issueActionRequests1 = Lists.newArrayListWithCapacity(1);
    ActionRequest issueActionRequest1 = createMock(ActionRequest.class);
    issueActionRequests1.add(issueActionRequest1);

    Collection<ActionRequest> issueActionRequests2 = Lists.newArrayListWithCapacity(2);
    ActionRequest issueActionRequest2 = createMock(ActionRequest.class);
    issueActionRequests2.add(issueActionRequest2);
    ActionRequest issueActionRequest3 = createMock(ActionRequest.class);
    issueActionRequests2.add(issueActionRequest3);

    Collection<ActionRequest> projectActionRequests = Lists.newArrayListWithCapacity(1);
    ActionRequest projectActionRequest1 = createMock(ActionRequest.class);
    projectActionRequests.add(projectActionRequest1);

    expect(ruleBase.actionRequestsFor(issuePropertySet1)).andReturn(issueActionRequests1).once();
    expect(ruleBase.actionRequestsFor(issuePropertySet2)).andReturn(issueActionRequests2).once();
    expect(ruleBase.actionRequestsFor(projectPropertySet)).andReturn(projectActionRequests).once();

    actionExecutor.executeOnIssue("testIssue", issueActionRequests1, issuePropertySet1);
    actionExecutor.executeOnIssue("testIssue", issueActionRequests2, issuePropertySet2);
    actionExecutor.executeOnIssue("testIssue2", issueActionRequests2, issuePropertySet2);
    actionExecutor.executeOnProject("testProject", projectActionRequests, projectPropertySet);

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
