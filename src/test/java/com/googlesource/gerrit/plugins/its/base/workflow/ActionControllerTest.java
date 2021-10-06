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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gerrit.entities.Project;
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
  private static Project.NameKey testProjectName = Project.nameKey("test-project");
  private Injector injector;

  private PropertyExtractor propertyExtractor;
  private RuleBase ruleBase;
  private ActionExecutor actionExecutor;
  private ItsConfig itsConfig;

  public void testNoPropertySets() {
    ActionController actionController = createActionController();

    ChangeEvent event = mock(ChangeEvent.class);

    Set<Map<String, String>> propertySets = new HashSet<>();
    when(propertyExtractor.extractFrom(event))
        .thenReturn(new RefEventProperties(Collections.emptyMap(), propertySets));
    when(event.getProjectNameKey()).thenReturn(testProjectName);

    actionController.onEvent(event);
  }

  public void testNoActionsOrNoIssues() {
    ActionController actionController = createActionController();

    ChangeEvent event = mock(ChangeEvent.class);

    Set<Map<String, String>> propertySets = new HashSet<>();
    Map<String, String> properties = ImmutableMap.of("fake", "property");
    propertySets.add(properties);

    when(propertyExtractor.extractFrom(event))
        .thenReturn(new RefEventProperties(properties, propertySets));
    when(event.getProjectNameKey()).thenReturn(testProjectName);

    // When no issues are found in the commit message, the list of actions is empty
    // as there are no matchs with an empty map of properties.
    Collection<ActionRequest> actions = Collections.emptySet();
    when(ruleBase.actionRequestsFor(properties))
        .thenReturn(actions)
        .thenReturn(actions)
        .thenThrow(new UnsupportedOperationException("Method called more than twice"));

    actionController.onEvent(event);
  }

  public void testSinglePropertyMapSingleIssueActionSingleProjectAction() {
    ActionController actionController = createActionController();

    ChangeEvent event = mock(ChangeEvent.class);

    Map<String, String> projectProperties = ImmutableMap.of("its-project", "itsProject");

    Map<String, String> issueProperties =
        ImmutableMap.<String, String>builder()
            .putAll(projectProperties)
            .put("issue", "testIssue")
            .build();

    Set<Map<String, String>> propertySets = ImmutableSet.of(issueProperties);

    when(propertyExtractor.extractFrom(event))
        .thenReturn(new RefEventProperties(projectProperties, propertySets));
    when(event.getProjectNameKey()).thenReturn(testProjectName);

    ActionRequest issueActionRequest1 = mock(ActionRequest.class);
    Collection<ActionRequest> issueActionRequests = ImmutableList.of(issueActionRequest1);
    when(ruleBase.actionRequestsFor(issueProperties))
        .thenReturn(issueActionRequests)
        .thenThrow(new UnsupportedOperationException("Method called more than once"));

    ActionRequest projectActionRequest1 = mock(ActionRequest.class);
    Collection<ActionRequest> projectActionRequests = ImmutableList.of(projectActionRequest1);
    when(ruleBase.actionRequestsFor(projectProperties))
        .thenReturn(projectActionRequests)
        .thenThrow(new UnsupportedOperationException("Method called more than once"));

    actionController.onEvent(event);

    verify(actionExecutor).executeOnIssue(issueActionRequests, issueProperties);
    verify(actionExecutor).executeOnProject(projectActionRequests, projectProperties);
  }

  public void testMultiplePropertyMapsMultipleActionMultipleIssue() {
    ActionController actionController = createActionController();

    ChangeEvent event = mock(ChangeEvent.class);

    Map<String, String> properties1 = ImmutableMap.of("issue", "testIssue");
    Map<String, String> properties2 = ImmutableMap.of("issue", "testIssue2");

    Set<Map<String, String>> propertySets = ImmutableSet.of(properties1, properties2);

    when(propertyExtractor.extractFrom(event))
        .thenReturn(new RefEventProperties(Collections.emptyMap(), propertySets));
    when(event.getProjectNameKey()).thenReturn(testProjectName);

    ActionRequest actionRequest1 = mock(ActionRequest.class);
    Collection<ActionRequest> actionRequests1 = ImmutableList.of(actionRequest1);

    ActionRequest actionRequest2 = mock(ActionRequest.class);
    ActionRequest actionRequest3 = mock(ActionRequest.class);
    Collection<ActionRequest> actionRequests2 = ImmutableList.of(actionRequest2, actionRequest3);

    when(ruleBase.actionRequestsFor(properties1))
        .thenReturn(actionRequests1)
        .thenThrow(new UnsupportedOperationException("Method called more than once"));
    when(ruleBase.actionRequestsFor(properties2))
        .thenReturn(actionRequests2)
        .thenThrow(new UnsupportedOperationException("Method called more than once"));

    actionController.onEvent(event);

    verify(actionExecutor).executeOnIssue(actionRequests1, properties1);
    verify(actionExecutor).executeOnIssue(actionRequests2, properties2);
  }

  private ActionController createActionController() {
    return injector.getInstance(ActionController.class);
  }

  private void setupCommonMocks() {
    when(itsConfig.isEnabled(any(RefEvent.class))).thenReturn(true);
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
      propertyExtractor = mock(PropertyExtractor.class);
      bind(PropertyExtractor.class).toInstance(propertyExtractor);

      ruleBase = mock(RuleBase.class);
      bind(RuleBase.class).toInstance(ruleBase);

      actionExecutor = mock(ActionExecutor.class);
      bind(ActionExecutor.class).toInstance(actionExecutor);

      itsConfig = mock(ItsConfig.class);
      bind(ItsConfig.class).toInstance(itsConfig);
    }
  }
}
