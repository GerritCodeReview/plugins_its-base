// Copyright (C) 2018 The Android Open Source Project
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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gerrit.extensions.config.FactoryModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.googlesource.gerrit.plugins.its.base.its.ItsFacade;
import com.googlesource.gerrit.plugins.its.base.testutil.MockingTestCase;
import com.googlesource.gerrit.plugins.its.base.util.IssueExtractor;
import com.googlesource.gerrit.plugins.its.base.util.PropertyExtractor;
import com.googlesource.gerrit.plugins.its.base.workflow.commit_collector.SinceLastTagCommitCollector;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.easymock.EasyMock;

public class FireEventOnCommitsTest extends MockingTestCase {

  private static final String ITS_PROJECT = "test-project";
  private static final String COMMIT = "1234";
  private static final String PROJECT = "testProject";

  private Injector injector;
  private ItsFacade its;
  private PropertyExtractor propertyExtractor;
  private IssueExtractor issueExtractor;
  private RuleBase ruleBase;
  private ActionExecutor actionExecutor;
  private FireEventOnCommitsParametersExtractor parametersExtractor;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    injector = Guice.createInjector(new TestModule());
  }

  private class TestModule extends FactoryModule {
    @Override
    protected void configure() {
      its = createMock(ItsFacade.class);
      bind(ItsFacade.class).toInstance(its);

      propertyExtractor = createMock(PropertyExtractor.class);
      bind(PropertyExtractor.class).toInstance(propertyExtractor);

      ruleBase = createMock(RuleBase.class);
      bind(RuleBase.class).toInstance(ruleBase);

      issueExtractor = createMock(IssueExtractor.class);
      bind(IssueExtractor.class).toInstance(issueExtractor);

      actionExecutor = createMock(ActionExecutor.class);
      bind(ActionExecutor.class).toInstance(actionExecutor);

      parametersExtractor = createMock(FireEventOnCommitsParametersExtractor.class);
      bind(FireEventOnCommitsParametersExtractor.class).toInstance(parametersExtractor);
    }
  }

  public void testHappyPath() throws IOException {
    Map<String, String> properties = ImmutableMap.of("project", PROJECT);

    FireEventOnCommitsParameters parameters = createMock(FireEventOnCommitsParameters.class);
    SinceLastTagCommitCollector collector = createMock(SinceLastTagCommitCollector.class);
    expect(parameters.getCommitCollector()).andReturn(collector);
    expect(parameters.getProjectName()).andReturn(PROJECT);

    ActionRequest actionRequest = createMock(ActionRequest.class);
    expect(parametersExtractor.extract(actionRequest, properties))
        .andReturn(Optional.of(parameters));
    expect(collector.collect(properties)).andReturn(Collections.singletonList(COMMIT));

    Map<String, Set<String>> associations = Maps.newHashMap();
    expect(issueExtractor.getIssueIds(PROJECT, COMMIT)).andReturn(associations);

    Set<Map<String, String>> issuesProperties = ImmutableSet.of(properties);
    expect(propertyExtractor.extractIssuesProperties(properties, associations))
        .andReturn(issuesProperties);

    ActionRequest subActionRequest = createMock(ActionRequest.class);
    Collection<ActionRequest> subActionRequests = Collections.singleton(subActionRequest);
    expect(ruleBase.actionRequestsFor(properties)).andReturn(subActionRequests);
    actionExecutor.executeOnIssue(subActionRequests, properties);
    EasyMock.expectLastCall().once();

    replayMocks();

    FireEventOnCommits fireEventOnCommits = createFireEventOnCommits();
    fireEventOnCommits.execute(its, ITS_PROJECT, actionRequest, properties);
  }

  private FireEventOnCommits createFireEventOnCommits() {
    return injector.getInstance(FireEventOnCommits.class);
  }
}
